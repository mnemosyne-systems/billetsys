package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Attachment;
import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.Version;
import ai.mnemosyne_systems.service.CrossReferenceService;
import ai.mnemosyne_systems.util.AuthHelper;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Path("/api/superuser/tickets")
@Produces(MediaType.APPLICATION_JSON)
public class SuperuserTicketApiResource {

    @Inject
    CrossReferenceService crossReferenceService;

    @Inject
    SuperuserResource superuserResource;

    @GET
    @Transactional
    public SupportTicketApiResource.SupportTicketListResponse list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("view") @DefaultValue("assigned") String view, @QueryParam("q") String q) {
        User user = requireSuperuser(auth);
        SuperuserResource.SupportTicketData data = superuserResource.buildTicketDataForUser(user);
        String normalizedView = normalizeView(view);
        List<Ticket> tickets = switch (normalizedView) {
            case "open" -> data.openTickets;
            case "closed" -> data.closedTickets;
            default -> data.assignedTickets;
        };
        String searchTerm = TicketSearchSupport.normalizeSearchTerm(q);
        if (searchTerm != null) {
            tickets = TicketSearchSupport.combineTickets(data.assignedTickets, data.openTickets, data.closedTickets);
        }
        tickets = TicketSearchSupport.filterTicketsBySearch(tickets, searchTerm);
        String title = switch (normalizedView) {
            case "open" -> "Open tickets";
            case "closed" -> "Closed tickets";
            default -> "Tickets";
        };
        return new SupportTicketApiResource.SupportTicketListResponse(normalizedView, title,
                data.assignedTickets == null ? 0 : data.assignedTickets.size(),
                data.openTickets == null ? 0 : data.openTickets.size(), "/superuser/tickets/new", searchTerm,
                tickets.stream().map(ticket -> toSummary(ticket, data)).toList());
    }

    @GET
    @Path("/suggest")
    @Transactional
    public SupportTicketApiResource.TicketSuggestionResponse suggest(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("view") @DefaultValue("assigned") String view, @QueryParam("q") String q) {
        User user = requireSuperuser(auth);
        SuperuserResource.SupportTicketData data = superuserResource.buildTicketDataForUser(user);
        List<Ticket> tickets = TicketSearchSupport.combineTickets(data.assignedTickets, data.openTickets,
                data.closedTickets);
        return new SupportTicketApiResource.TicketSuggestionResponse(TicketSearchSupport.suggestTickets(tickets, q, 6)
                .stream().map(ticket -> new SupportTicketApiResource.TicketSuggestion(ticket.id, ticket.name,
                        ticket.displayTitle(), "/superuser/tickets/" + ticket.id))
                .toList());
    }

    @GET
    @Path("/bootstrap")
    @Transactional
    public SupportTicketApiResource.SupportTicketBootstrapResponse bootstrap(
            @CookieParam(AuthHelper.AUTH_COOKIE) String auth, @QueryParam("companyId") Long companyId,
            @QueryParam("companyEntitlementId") Long companyEntitlementId) {
        User user = requireSuperuser(auth);
        SuperuserResource.SupportTicketData data = superuserResource.buildTicketDataForUser(user);
        List<Company> companies = superuserResource.userCompanies(user);
        Company selectedCompany = selectCompany(companies, companyId);
        List<CompanyEntitlement> entitlements = selectedCompany == null ? List.of()
                : uniqueEntitlements(CompanyEntitlement.find(
                        "select distinct ce from CompanyEntitlement ce join fetch ce.entitlement join fetch ce.supportLevel where ce.company = ?1 order by ce.entitlement.name, ce.supportLevel.level, ce.supportLevel.id",
                        selectedCompany).list());
        CompanyEntitlement selectedEntitlement = selectEntitlement(entitlements, companyEntitlementId);
        Long selectedCompanyEntitlementId = selectedEntitlement == null ? null : selectedEntitlement.id;
        Version defaultAffectsVersion = superuserResource.defaultAffectsVersion(selectedEntitlement);
        List<SupportTicketApiResource.VersionOption> versions = SupportTicketViewSupport
                .availableVersions(selectedEntitlement).stream()
                .map(version -> new SupportTicketApiResource.VersionOption(version.id, version.name,
                        version.date == null ? null : version.date.toString()))
                .toList();
        Category defaultCategory = Category.findDefault();
        return new SupportTicketApiResource.SupportTicketBootstrapResponse(
                data.assignedTickets == null ? 0 : data.assignedTickets.size(),
                data.openTickets == null ? 0 : data.openTickets.size(),
                selectedCompany == null ? null : selectedCompany.id,
                selectedCompany == null ? "" : Ticket.previewNextName(selectedCompany), "",
                companies.stream().map(company -> new SupportTicketApiResource.CompanyOption(company.id, company.name))
                        .toList(),
                entitlements.stream().map(this::toEntitlementOption).toList(), selectedCompanyEntitlementId,
                Category.<Category> list("order by name").stream().map(this::toCategoryOption).toList(),
                defaultCategory == null ? null : defaultCategory.id,
                defaultAffectsVersion == null ? null
                        : new SupportTicketApiResource.VersionOption(defaultAffectsVersion.id,
                                defaultAffectsVersion.name,
                                defaultAffectsVersion.date == null ? null : defaultAffectsVersion.date.toString()),
                versions, "/superuser/tickets");
    }

    @GET
    @Path("/{id}")
    @Transactional
    public UserTicketApiResource.RoleTicketDetailResponse detail(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        User user = requireSuperuser(auth);
        Ticket ticket = superuserResource.findTicketForSuperuser(user, id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        SuperuserResource.SupportTicketData data = superuserResource.buildTicketDataForUser(user);
        List<Message> messages = Message.find(
                "select distinct m from Message m left join fetch m.attachments where m.ticket = ?1 order by m.date desc",
                ticket).list();
        List<User> supportUsers = User
                .find("select u from Ticket t join t.supportUsers u where t = ?1 order by u.email", ticket).list();
        List<User> secondaryUsers = ticket.company == null ? new ArrayList<>()
                : User.find(
                        "select distinct u from Company c join c.users u where c = ?1 and lower(u.type) = ?2 order by u.email",
                        ticket.company, User.TYPE_SUPERUSER).list();
        java.util.Map<Long, Ticket> ticketCache = crossReferenceService
                .preloadReferencedTickets(messages.stream().map(m -> m.body).toList());
        return new UserTicketApiResource.RoleTicketDetailResponse(ticket.id, ticket.name, ticket.displayTitle(),
                ticket.status == null || ticket.status.isBlank() ? "Open" : ticket.status,
                data.assignedTickets == null ? 0 : data.assignedTickets.size(),
                data.openTickets == null ? 0 : data.openTickets.size(),
                ticket.company == null ? null : ticket.company.id, ticket.company == null ? null : ticket.company.name,
                ticket.category == null ? null : ticket.category.id,
                ticket.category == null ? null : ticket.category.name,
                ticket.companyEntitlement == null ? null : ticket.companyEntitlement.id,
                ticket.companyEntitlement == null || ticket.companyEntitlement.entitlement == null ? null
                        : ticket.companyEntitlement.entitlement.name,
                superuserResource.resolveLowestEntitlementLevelName(ticket), ticket.externalIssueLink,
                ticket.affectsVersion == null ? null : ticket.affectsVersion.id,
                ticket.resolvedVersion == null ? null : ticket.resolvedVersion.id,
                superuserResource.availableVersions(ticket).stream()
                        .map(version -> new SupportTicketApiResource.VersionOption(version.id, version.name,
                                version.date == null ? null : version.date.toString()))
                        .toList(),
                Category.<Category> list("order by name").stream().map(this::toCategoryOption).toList(),
                supportUsers.stream().map(this::toUserReference).toList(), "Superusers",
                secondaryUsers.stream().map(this::toUserReference).toList(),
                messages.stream().map(m -> toMessageEntry(m, ticketCache)).toList(),
                superuserResource.isEntitlementExpired(ticket), "/superuser/tickets/" + ticket.id,
                "/superuser/tickets/" + ticket.id + "/messages", "/tickets/export/" + ticket.id,
                List.of("Open", "Assigned", "In Progress", "Resolved", "Closed"), false, false, false, true, true);
    }

    @GET
    @Path("/{id}/references")
    @Transactional
    public CrossReferenceService.CrossReferencesResponse references(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        requireSuperuser(auth);
        Ticket ticket = Ticket.findById(id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        return crossReferenceService.getReferences(ticket, null, "/superuser/tickets/");
    }

    private SupportTicketApiResource.SupportTicketSummary toSummary(Ticket ticket,
            SuperuserResource.SupportTicketData data) {
        User assignedSupport = data.supportAssignmentUsers.get(ticket.id);
        return new SupportTicketApiResource.SupportTicketSummary(ticket.id, ticket.name, ticket.displayTitle(),
                ticket.status, data.messageDateLabels.get(ticket.id), data.messageDirectionArrows.get(ticket.id),
                data.slaColors.get(ticket.id), ticket.category == null ? null : ticket.category.name,
                assignedSupport == null ? null : toUserReference(assignedSupport),
                ticket.company == null ? null : ticket.company.id, ticket.company == null ? null : ticket.company.name,
                ticket.companyEntitlement == null || ticket.companyEntitlement.entitlement == null ? null
                        : ticket.companyEntitlement.entitlement.name,
                ticket.companyEntitlement == null || ticket.companyEntitlement.supportLevel == null ? null
                        : ticket.companyEntitlement.supportLevel.name,
                ticket.affectsVersion == null ? null : ticket.affectsVersion.name,
                ticket.resolvedVersion == null ? null : ticket.resolvedVersion.name, "/superuser/tickets/" + ticket.id,
                ticket.company == null ? null : "/superuser/companies/" + ticket.company.id);
    }

    private SupportTicketApiResource.EntitlementOption toEntitlementOption(CompanyEntitlement entry) {
        return new SupportTicketApiResource.EntitlementOption(entry.id,
                entry.entitlement == null ? null : entry.entitlement.name,
                entry.supportLevel == null ? null : entry.supportLevel.name);
    }

    private SupportTicketApiResource.CategoryOption toCategoryOption(Category category) {
        return new SupportTicketApiResource.CategoryOption(category.id, category.name);
    }

    private SupportTicketApiResource.UserReference toUserReference(User user) {
        return new SupportTicketApiResource.UserReference(user.id, user.name, user.getDisplayName(), user.fullName,
                user.email, user.type, user.country == null ? null : user.country.name,
                user.timezone == null ? null : user.timezone.name, user.logoBase64, userPath(user));
    }

    private SupportTicketApiResource.MessageEntry toMessageEntry(Message message,
            java.util.Map<Long, Ticket> ticketCache) {
        String transformedBody = crossReferenceService.transformBody(message.body, "/superuser/tickets/", ticketCache);
        return new SupportTicketApiResource.MessageEntry(message.id, transformedBody,
                message.date == null ? null : SupportTicketViewSupport.formatDate(message.date),
                message.date == null ? null : message.date.toString(),
                message.author == null ? null : toUserReference(message.author), message.attachments == null ? List.of()
                        : message.attachments.stream().map(this::toAttachmentEntry).toList());
    }

    private SupportTicketApiResource.AttachmentEntry toAttachmentEntry(Attachment attachment) {
        return new SupportTicketApiResource.AttachmentEntry(attachment.id, attachment.name, attachment.mimeType,
                attachment.sizeLabel(), "/attachments/" + attachment.id);
    }

    private String normalizeView(String view) {
        if ("open".equalsIgnoreCase(view)) {
            return "open";
        }
        if ("closed".equalsIgnoreCase(view)) {
            return "closed";
        }
        return "assigned";
    }

    private Company selectCompany(List<Company> companies, Long companyId) {
        if (companies == null || companies.isEmpty()) {
            return null;
        }
        if (companyId == null) {
            return companies.get(0);
        }
        return companies.stream().filter(company -> company.id != null && company.id.equals(companyId)).findFirst()
                .orElse(companies.get(0));
    }

    private CompanyEntitlement selectEntitlement(List<CompanyEntitlement> entitlements, Long companyEntitlementId) {
        if (entitlements == null || entitlements.isEmpty()) {
            return null;
        }
        if (companyEntitlementId == null) {
            return entitlements.get(0);
        }
        return entitlements.stream()
                .filter(entitlement -> entitlement.id != null && entitlement.id.equals(companyEntitlementId))
                .findFirst().orElse(entitlements.get(0));
    }

    private List<CompanyEntitlement> uniqueEntitlements(List<CompanyEntitlement> entitlements) {
        List<CompanyEntitlement> uniqueEntitlements = new ArrayList<>();
        Set<Long> entitlementIds = new LinkedHashSet<>();
        for (CompanyEntitlement entitlement : entitlements) {
            if (entitlement == null || entitlement.entitlement == null || entitlement.entitlement.id == null) {
                continue;
            }
            if (entitlementIds.add(entitlement.entitlement.id)) {
                uniqueEntitlements.add(entitlement);
            }
        }
        return uniqueEntitlements;
    }

    private String userPath(User user) {
        if (user == null || user.id == null || user.type == null) {
            return null;
        }
        String type = user.type.trim().toLowerCase(Locale.ENGLISH);
        return switch (type) {
            case User.TYPE_SUPPORT -> "/superuser/support-users/" + user.id;
            case User.TYPE_SUPERUSER -> "/superuser/superuser-users/" + user.id;
            default -> "/superuser/user-profiles/" + user.id;
        };
    }

    private User requireSuperuser(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isSuperuser(user)) {
            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        return user;
    }
}
