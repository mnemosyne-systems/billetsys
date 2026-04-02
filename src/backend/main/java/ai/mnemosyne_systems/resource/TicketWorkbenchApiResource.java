package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.Version;
import ai.mnemosyne_systems.util.AuthHelper;
import io.smallrye.common.annotation.Blocking;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Path("/api/ticket-workbench")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
public class TicketWorkbenchApiResource {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d yyyy, h.mma",
            Locale.ENGLISH);

    @GET
    @Transactional
    public TicketListResponse list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireSupport(auth);
        List<Ticket> tickets = Ticket.listAll();
        Map<Long, String> lastMessageLabels = new LinkedHashMap<>();
        for (Message message : Message.<Message> list("order by date desc")) {
            if (message.ticket != null && message.ticket.id != null
                    && !lastMessageLabels.containsKey(message.ticket.id)) {
                lastMessageLabels.put(message.ticket.id, formatDate(message.date));
            }
        }
        return new TicketListResponse("Tickets", "/tickets/new", tickets.stream()
                .map(ticket -> new TicketListItem(ticket.id, ticket.name, ticket.status,
                        ticket.company == null ? null : ticket.company.name,
                        ticket.requester == null ? null : ticket.requester.getDisplayName(),
                        ticket.category == null ? null : ticket.category.name, lastMessageLabels.get(ticket.id),
                        "/support/tickets/" + ticket.id, "/tickets/" + ticket.id + "/edit", ticket.externalIssueLink))
                .toList());
    }

    @GET
    @Path("/bootstrap")
    @Transactional
    public TicketFormResponse bootstrap(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("ticketId") Long ticketId, @QueryParam("companyId") Long companyId) {
        requireSupport(auth);
        Ticket ticket = ticketId == null ? new Ticket()
                : Ticket.find(
                        "select t from Ticket t left join fetch t.companyEntitlement ce left join fetch ce.entitlement left join fetch ce.supportLevel where t.id = ?1",
                        ticketId).firstResult();
        if (ticketId != null && ticket == null) {
            throw new NotFoundException();
        }
        Company selectedCompany = determineCompany(ticket, companyId);
        List<CompanyEntitlement> entitlements = loadEntitlements(selectedCompany);
        CompanyEntitlement selectedEntitlement = determineEntitlement(ticket, entitlements);
        List<Version> versions = availableVersions(selectedEntitlement);
        Category defaultCategory = ticket.category == null ? Category.findDefault() : ticket.category;
        if (ticketId == null) {
            ticket.status = ticket.status == null ? "Open" : ticket.status;
            ticket.title = ticket.displayTitle();
            ticket.company = selectedCompany;
            ticket.companyEntitlement = selectedEntitlement;
            ticket.affectsVersion = defaultAffectsVersion(selectedEntitlement);
            ticket.category = defaultCategory;
        }
        return new TicketFormResponse(ticketId == null ? "New ticket" : "Edit ticket",
                ticketId == null ? "/tickets" : "/tickets/" + ticketId, "/tickets", ticketId != null,
                Company.<Company> list("order by name").stream()
                        .map(company -> new CompanyOption(company.id, company.name)).toList(),
                entitlements.stream()
                        .map(entitlement -> new EntitlementOption(entitlement.id, entitlementLabel(entitlement)))
                        .toList(),
                Category.<Category> listAll().stream().map(category -> new CategoryOption(category.id, category.name))
                        .toList(),
                versions.stream().map(version -> new VersionOption(version.id, version.name)).toList(),
                ticketId == null ? List.of()
                        : Message.<Message> list("ticket = ?1 order by date desc", ticket).stream()
                                .map(this::toMessageSummary).toList(),
                new TicketFormData(ticket.id, ticket.displayTitle(), ticket.status,
                        ticket.company == null ? null : ticket.company.id,
                        ticket.companyEntitlement == null ? null : ticket.companyEntitlement.id,
                        ticket.category == null ? null : ticket.category.id, ticket.externalIssueLink,
                        ticket.affectsVersion == null ? null : ticket.affectsVersion.id,
                        ticket.resolvedVersion == null ? null : ticket.resolvedVersion.id));
    }

    @GET
    @Path("/{id}")
    @Transactional
    public TicketFormResponse detail(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        return bootstrap(auth, id, null);
    }

    private MessageSummary toMessageSummary(Message message) {
        return new MessageSummary(message.id, message.body, formatDate(message.date));
    }

    private Company determineCompany(Ticket ticket, Long companyId) {
        if (companyId != null) {
            Company selected = Company.findById(companyId);
            if (selected != null) {
                return selected;
            }
        }
        if (ticket != null && ticket.company != null) {
            return ticket.company;
        }
        return Company.find("order by name").firstResult();
    }

    private CompanyEntitlement determineEntitlement(Ticket ticket, List<CompanyEntitlement> entitlements) {
        if (ticket != null && ticket.companyEntitlement != null) {
            return ticket.companyEntitlement;
        }
        return entitlements.isEmpty() ? null : entitlements.get(0);
    }

    private List<CompanyEntitlement> loadEntitlements(Company company) {
        if (company == null) {
            return List.of();
        }
        return CompanyEntitlement.find(
                "select distinct ce from CompanyEntitlement ce join fetch ce.entitlement join fetch ce.supportLevel where ce.company = ?1",
                company).list();
    }

    private List<Version> availableVersions(CompanyEntitlement companyEntitlement) {
        if (companyEntitlement == null || companyEntitlement.entitlement == null) {
            return List.of();
        }
        return Version.list("entitlement = ?1 order by date asc, id asc", companyEntitlement.entitlement);
    }

    private Version defaultAffectsVersion(CompanyEntitlement companyEntitlement) {
        if (companyEntitlement == null || companyEntitlement.entitlement == null) {
            return null;
        }
        Version version = Version.find("entitlement = ?1 and name = ?2 order by date asc, id asc",
                companyEntitlement.entitlement, "1.0.0").firstResult();
        return version != null ? version
                : Version.find("entitlement = ?1 order by date asc, id asc", companyEntitlement.entitlement)
                        .firstResult();
    }

    private String entitlementLabel(CompanyEntitlement entitlement) {
        String entitlementName = entitlement == null || entitlement.entitlement == null ? "Unknown entitlement"
                : entitlement.entitlement.name;
        String levelName = entitlement == null || entitlement.supportLevel == null ? "No level"
                : entitlement.supportLevel.name;
        return entitlementName + " • " + levelName;
    }

    private String formatDate(LocalDateTime date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private User requireSupport(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isSupport(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }

    public record TicketListResponse(String title, String createPath, List<TicketListItem> items) {
    }

    public record TicketListItem(Long id, String name, String status, String companyName, String requesterName,
            String categoryName, String lastMessageLabel, String detailPath, String editPath,
            String externalIssueLink) {
    }

    public record TicketFormResponse(String title, String submitPath, String cancelPath, boolean edit,
            List<CompanyOption> companies, List<EntitlementOption> entitlements, List<CategoryOption> categories,
            List<VersionOption> versions, List<MessageSummary> messages, TicketFormData ticket) {
    }

    public record CompanyOption(Long id, String name) {
    }

    public record EntitlementOption(Long id, String name) {
    }

    public record CategoryOption(Long id, String name) {
    }

    public record VersionOption(Long id, String name) {
    }

    public record MessageSummary(Long id, String body, String dateLabel) {
    }

    public record TicketFormData(Long id, String title, String status, Long companyId, Long companyEntitlementId,
            Long categoryId, String externalIssueLink, Long affectsVersionId, Long resolvedVersionId) {
    }
}
