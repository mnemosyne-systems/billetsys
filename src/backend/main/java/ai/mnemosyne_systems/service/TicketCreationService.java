/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.Version;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class TicketCreationService {

    public Ticket createTicketWithInitialMessage(TicketCreationRequest request) {
        Ticket ticket = new Ticket();
        ticket.name = Ticket.nextName(request.company());
        ticket.title = Ticket.normalizeTitle(request.title());
        ticket.status = request.status();
        ticket.company = request.company();
        ticket.requester = request.requester();
        ticket.companyEntitlement = request.companyEntitlement();
        ticket.affectsVersion = defaultAffectsVersion(request.companyEntitlement());
        ticket.category = request.category() == null ? Category.findDefault() : request.category();
        ticket.externalIssueLink = trimOrNull(request.externalIssueLink());
        ticket.persist();
        assignCompanyTams(ticket);

        Message message = new Message();
        message.body = request.initialMessage();
        message.date = request.messageDate() == null ? LocalDateTime.now() : request.messageDate();
        message.ticket = ticket;
        message.author = request.requester();
        message.persist();
        return ticket;
    }

    public void assignCompanyTams(Ticket ticket) {
        if (ticket == null || ticket.company == null) {
            return;
        }
        List<User> tams = User.find("select u from Company c join c.users u where c = ?1 and lower(u.type) = ?2",
                ticket.company, User.TYPE_TAM).list();
        if (tams.isEmpty()) {
            return;
        }
        ticket.tamUsers.size();
        Set<Long> existingIds = new HashSet<>();
        for (User existing : ticket.tamUsers) {
            if (existing.id != null) {
                existingIds.add(existing.id);
            }
        }
        for (User tam : tams) {
            if (tam.id != null && !existingIds.contains(tam.id)) {
                ticket.tamUsers.add(tam);
            }
        }
    }

    public Version defaultAffectsVersion(CompanyEntitlement companyEntitlement) {
        if (companyEntitlement == null || companyEntitlement.entitlement == null) {
            return null;
        }
        Version version = Version.find("entitlement = ?1 and name = ?2 order by date asc, id asc",
                companyEntitlement.entitlement, "1.0.0").firstResult();
        if (version != null) {
            return version;
        }
        return Version.find("entitlement = ?1 order by date asc, id asc", companyEntitlement.entitlement).firstResult();
    }

    private String trimOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record TicketCreationRequest(String title, String status, Company company,
            CompanyEntitlement companyEntitlement, Category category, User requester, String initialMessage,
            LocalDateTime messageDate, String externalIssueLink) {
    }
}
