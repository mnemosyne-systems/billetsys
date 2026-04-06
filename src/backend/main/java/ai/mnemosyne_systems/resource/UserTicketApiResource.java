package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Attachment;
import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.Version;
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

@Path("/api/user/tickets")
@Produces(MediaType.APPLICATION_JSON)
public class UserTicketApiResource {

    @Inject
    UserResource userResource;

    @GET
    @Transactional
    public SupportTicketApiResource.SupportTicketListResponse list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("view") @DefaultValue("assigned") String view, @QueryParam("q") String q) {
        User user = requireUser(auth);
        UserResource.SupportTicketData data = userResource.buildTicketDataForUser(user);
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
                data.openTickets == null ? 0 : data.openTickets.size(), "/user/tickets/new", searchTerm,
                tickets.stream().map(ticket -> toSummary(ticket, data, user)).toList());
    }

    @GET
    @Path("/suggest")
    @Transactional
    public SupportTicketApiResource.TicketSuggestionResponse suggest(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("view") @DefaultValue("assigned") String view, @QueryParam("q") String q) {
        User user = requireUser(auth);
        UserResource.SupportTicketData data = userResource.buildTicketDataForUser(user);
        List<Ticket> tickets = TicketSearchSupport.combineTickets(data.assignedTickets, data.openTickets,
                data.closedTickets);
        return new SupportTicketApiResource.TicketSuggestionResponse(TicketSearchSupport.suggestTickets(tickets, q, 8)
                .stream().map(ticket -> new SupportTicketApiResource.TicketSuggestion(ticket.id, ticket.name,
                        ticket.displayTitle(), "/user/tickets/" + ticket.id))
                .toList());
    }

    @GET
    @Path("/bootstrap")
    @Transactional
    public SupportTicketApiResource.SupportTicketBootstrapResponse bootstrap(
            @CookieParam(AuthHelper.AUTH_COOKIE) String auth, @QueryParam("companyId") Long companyId,
            @QueryParam("companyEntitlementId") Long companyEntitlementId) {
        User user = requireUser(auth);
        UserResource.SupportTicketData data = userResource.buildTicketDataForUser(user);
        List<Company> companies = Company
                .find("select distinct c from Company c join c.users u where u = ?1 order by c.name", user).list();
        Company selectedCompany = selectCompany(companies, companyId);
        List<CompanyEntitlement> entitlements = selectedCompany == null ? List.of()
                : uniqueEntitlements(CompanyEntitlement.find(
                        "select distinct ce from CompanyEntitlement ce join fetch ce.entitlement join fetch ce.supportLevel where ce.company = ?1 order by ce.entitlement.name, ce.supportLevel.level, ce.supportLevel.id",
                        selectedCompany).list());
        CompanyEntitlement selectedEntitlement = selectEntitlement(entitlements, companyEntitlementId);
        Long selectedCompanyEntitlementId = selectedEntitlement == null ? null : selectedEntitlement.id;
        Version defaultAffectsVersion = userResource.defaultAffectsVersion(selectedEntitlement);
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
                versions, "/user/tickets");
    }

    @GET
    @Path("/{id}")
    @Transactional
    public RoleTicketDetailResponse detail(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireUser(auth);
        Ticket ticket = userResource.findTicketForUser(user, id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        UserResource.SupportTicketData data = userResource.buildTicketDataForUser(user);
        List<Message> messages = Message.find(
                "select distinct m from Message m left join fetch m.attachments where m.ticket = ?1 order by m.date desc",
                ticket).list();
        List<User> supportUsers = User
                .find("select u from Ticket t join t.supportUsers u where t = ?1 order by u.email", ticket).list();
        List<User> secondaryUsers = ticket.company == null ? new ArrayList<>()
                : User.find(
                        "select distinct u from Company c join c.users u where c = ?1 and lower(u.type) = ?2 order by u.email",
                        ticket.company, User.TYPE_TAM).list();
        List<User> ticketTams = User
                .find("select u from Ticket t join t.tamUsers u where t = ?1 order by u.email", ticket).list();
        mergeMissingUsers(secondaryUsers, ticketTams);
        boolean tamView = User.TYPE_TAM.equalsIgnoreCase(user.type);
        return new RoleTicketDetailResponse(ticket.id, ticket.name, ticket.displayTitle(),
                ticket.status == null || ticket.status.isBlank() ? "Open" : ticket.status,
                data.assignedTickets == null ? 0 : data.assignedTickets.size(),
                data.openTickets == null ? 0 : data.openTickets.size(),
                ticket.company == null ? null : ticket.company.id, ticket.company == null ? null : ticket.company.name,
                ticket.category == null ? null : ticket.category.id,
                ticket.category == null ? null : ticket.category.name,
                ticket.companyEntitlement == null ? null : ticket.companyEntitlement.id,
                ticket.companyEntitlement == null || ticket.companyEntitlement.entitlement == null ? null
                        : ticket.companyEntitlement.entitlement.name,
                tamView ? userResource.resolveLowestEntitlementLevelName(ticket) : null, ticket.externalIssueLink,
                ticket.affectsVersion == null ? null : ticket.affectsVersion.id,
                ticket.resolvedVersion == null ? null : ticket.resolvedVersion.id,
                userResource.availableVersions(ticket).stream()
                        .map(version -> new SupportTicketApiResource.VersionOption(version.id, version.name,
                                version.date == null ? null : version.date.toString()))
                        .toList(),
                Category.<Category> list("order by name").stream().map(this::toCategoryOption).toList(),
                supportUsers.stream().map(this::toUserReference).toList(), "TAM",
                secondaryUsers.stream().map(this::toUserReference).toList(),
                messages.stream().map(this::toMessageEntry).toList(), userResource.isEntitlementExpired(ticket),
                "/user/tickets/" + ticket.id, "/user/tickets/" + ticket.id + "/messages",
                "/tickets/export/" + ticket.id, List.of("Open", "Assigned", "In Progress", "Resolved", "Closed"), false,
                false, false, true, tamView);
    }

    private SupportTicketApiResource.SupportTicketSummary toSummary(Ticket ticket, UserResource.SupportTicketData data,
            User currentUser) {
        User assignedSupport = data.supportAssignmentUsers.get(ticket.id);
        return new SupportTicketApiResource.SupportTicketSummary(ticket.id, ticket.name, ticket.displayTitle(),
                ticket.status, data.messageDateLabels.get(ticket.id), data.messageDirectionArrows.get(ticket.id),
                data.slaColors.get(ticket.id), ticket.category == null ? null : ticket.category.name,
                assignedSupport == null ? null : toUserReference(assignedSupport),
                ticket.company == null ? null : ticket.company.id, ticket.company == null ? null : ticket.company.name,
                ticket.companyEntitlement == null || ticket.companyEntitlement.entitlement == null ? null
                        : ticket.companyEntitlement.entitlement.name,
                User.TYPE_TAM.equalsIgnoreCase(currentUser.type)
                        ? userResource.resolveLowestEntitlementLevelName(ticket)
                        : null,
                ticket.affectsVersion == null ? null : ticket.affectsVersion.name,
                ticket.resolvedVersion == null ? null : ticket.resolvedVersion.name, "/user/tickets/" + ticket.id,
                ticket.company == null ? null : "/user/companies/" + ticket.company.id);
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

    private SupportTicketApiResource.MessageEntry toMessageEntry(Message message) {
        return new SupportTicketApiResource.MessageEntry(message.id, message.body,
                message.date == null ? null : userResource.formatDate(message.date),
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

    private void mergeMissingUsers(List<User> users, List<User> extraUsers) {
        Set<Long> seenIds = new LinkedHashSet<>();
        for (User existing : users) {
            if (existing.id != null) {
                seenIds.add(existing.id);
            }
        }
        for (User existing : extraUsers) {
            if (existing.id != null && !seenIds.contains(existing.id)) {
                users.add(existing);
            }
        }
    }

    private String userPath(User user) {
        if (user == null || user.id == null || user.type == null) {
            return null;
        }
        String type = user.type.trim().toLowerCase(Locale.ENGLISH);
        return switch (type) {
            case User.TYPE_SUPPORT -> "/user/support-users/" + user.id;
            case User.TYPE_SUPERUSER -> "/user/superuser-users/" + user.id;
            case User.TYPE_TAM -> "/user/tam-users/" + user.id;
            default -> "/user/user-profiles/" + user.id;
        };
    }

    private User requireUser(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isUser(user)) {
            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        return user;
    }

    public record RoleTicketDetailResponse(Long id, String name, String title, String displayStatus, int assignedCount,
            int openCount, Long companyId, String companyName, Long categoryId, String categoryName,
            Long companyEntitlementId, String entitlementName, String levelName, String externalIssueLink,
            Long affectsVersionId, Long resolvedVersionId, List<SupportTicketApiResource.VersionOption> versions,
            List<SupportTicketApiResource.CategoryOption> categories,
            List<SupportTicketApiResource.UserReference> supportUsers, String secondaryUsersLabel,
            List<SupportTicketApiResource.UserReference> secondaryUsers,
            List<SupportTicketApiResource.MessageEntry> messages, boolean ticketEntitlementExpired, String actionPath,
            String messageActionPath, String exportPath, List<String> statusOptions, boolean editableStatus,
            boolean editableCategory, boolean editableExternalIssue, boolean editableAffectsVersion,
            boolean editableResolvedVersion) {
    }
}
