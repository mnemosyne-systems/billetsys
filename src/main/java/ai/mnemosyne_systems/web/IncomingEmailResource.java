/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/mail/incoming")
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Produces(MediaType.TEXT_PLAIN)
@Blocking
public class IncomingEmailResource {

    private static final Logger LOGGER = Logger.getLogger(IncomingEmailResource.class);
    private static final Pattern SUBJECT_TICKET_PATTERN = Pattern.compile("\\[([^\\]]+)]");

    @Inject
    TicketEmailService ticketEmailService;

    @POST
    @Transactional
    public Response receive(MultipartFormDataInput input) {
        String from = AttachmentHelper.readFormValue(input, "from");
        String subject = AttachmentHelper.readFormValue(input, "subject");
        String body = AttachmentHelper.readFormValue(input, "body");
        if (body == null || body.isBlank()) {
            throw new BadRequestException("Body is required");
        }
        String fromAddress = from == null ? "" : from.trim().toLowerCase();
        if (fromAddress.isBlank()) {
            LOGGER.warn("Ignoring incoming email: missing From");
            return Response.accepted().build();
        }
        User sender = from == null || from.isBlank() ? null
                : User.find("email", from.trim().toLowerCase()).firstResult();
        if (sender == null) {
            LOGGER.warnf("Ignoring incoming email: user not found for From '%s'", fromAddress);
            return Response.accepted().build();
        }
        String ticketName = extractTicketName(subject);
        Ticket ticket = resolveTicket(ticketName);
        if (ticketName != null) {
            if (ticket == null) {
                LOGGER.warnf("Ignoring incoming email: ticket '%s' not found for From '%s'", ticketName, fromAddress);
                return Response.accepted().build();
            }
            if (!senderMatchesTicket(sender, ticket)) {
                LOGGER.warnf("Ignoring incoming email: From '%s' does not match ticket '%s'", fromAddress, ticket.name);
                return Response.accepted().build();
            }
        }
        if (ticket == null) {
            ticket = createTicketForIncoming(sender);
            if (ticket == null) {
                return Response.accepted().build();
            }
        }
        Message message = new Message();
        message.body = body.trim();
        message.date = LocalDateTime.now();
        message.ticket = ticket;
        message.author = sender;
        AttachmentHelper.attachToMessage(message, AttachmentHelper.readAttachments(input, "attachments"));
        message.persist();
        ticketEmailService.notifyMessageChange(ticket, message, sender);
        return Response.ok(ticket.name).build();
    }

    private Ticket resolveTicket(String ticketName) {
        if (ticketName == null) {
            return null;
        }
        return Ticket.find("name = ?1 order by id desc", ticketName).firstResult();
    }

    private String extractTicketName(String subject) {
        if (subject == null || subject.isBlank()) {
            return null;
        }
        Matcher matcher = SUBJECT_TICKET_PATTERN.matcher(subject);
        if (!matcher.find()) {
            return null;
        }
        String candidate = matcher.group(1);
        return candidate == null ? null : candidate.trim();
    }

    private Ticket createTicketForIncoming(User sender) {
        Company company = companyForSender(sender);
        if (company == null) {
            LOGGER.warnf("Ignoring incoming email: no company found for From '%s'",
                    sender == null ? null : sender.email);
            return null;
        }
        CompanyEntitlement entitlement = CompanyEntitlement.find(
                "select ce from CompanyEntitlement ce join fetch ce.entitlement join fetch ce.supportLevel where ce.company = ?1 order by ce.entitlement.name, ce.supportLevel.level, ce.supportLevel.id",
                company).firstResult();
        if (entitlement == null) {
            throw new BadRequestException("Entitlement is required to create ticket from email");
        }
        Ticket ticket = new Ticket();
        ticket.name = Ticket.nextName(company);
        ticket.status = "Open";
        ticket.company = company;
        ticket.requester = sender;
        ticket.companyEntitlement = entitlement;
        ticket.category = Category.findDefault();
        ticket.persist();
        assignCompanyTams(ticket);
        return ticket;
    }

    private Company companyForSender(User sender) {
        if (sender == null) {
            return null;
        }
        return Company.find("select c from Company c join c.users u where u = ?1 order by c.id", sender).firstResult();
    }

    private boolean senderMatchesTicket(User sender, Ticket ticket) {
        if (sender == null || sender.id == null || ticket == null) {
            return false;
        }
        if (ticket.requester != null && sender.id.equals(ticket.requester.id)) {
            return true;
        }
        if (ticket.company != null && ticket.company.users != null) {
            for (User user : ticket.company.users) {
                if (user != null && user.id != null && sender.id.equals(user.id)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void assignCompanyTams(Ticket ticket) {
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
}
