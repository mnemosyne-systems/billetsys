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
import ai.mnemosyne_systems.model.event.Event;
import ai.mnemosyne_systems.service.EventService;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Path("/api/support/tickets")
@Produces(MediaType.APPLICATION_JSON)
public class SupportTicketApiResource {

    @Inject
    EventService eventService;

    @GET
    @Transactional
    public SupportTicketListResponse list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("view") @DefaultValue("assigned") String view, @QueryParam("q") String q) {
        User user = requireSupport(auth);
        SupportTicketViewSupport.SupportTicketData data = SupportTicketViewSupport.buildTicketData(user);
        SupportTicketViewSupport.SupportTicketCounts counts = SupportTicketViewSupport.loadTicketCounts(user);
        String normalizedView = normalizeView(view);
        List<Ticket> tickets = switch (normalizedView) {
            case "open" -> data.openTickets();
            case "closed" -> data.closedTickets();
            default -> data.assignedTickets();
        };
        String searchTerm = TicketSearchSupport.normalizeSearchTerm(q);
        if (searchTerm != null) {
            tickets = TicketSearchSupport.combineTickets(data.assignedTickets(), data.openTickets(),
                    data.closedTickets());
        }
        tickets = TicketSearchSupport.filterTicketsBySearch(tickets, searchTerm);
        String title = switch (normalizedView) {
            case "open" -> "Open tickets";
            case "closed" -> "Closed tickets";
            default -> "Tickets";
        };
        return new SupportTicketListResponse(normalizedView, title, counts.assignedCount(), counts.openCount(),
                "/support/tickets/new", searchTerm, tickets.stream().map(ticket -> toSummary(ticket, data)).toList());
    }

    @GET
    @Path("/suggest")
    @Transactional
    public TicketSuggestionResponse suggest(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("view") @DefaultValue("assigned") String view, @QueryParam("q") String q) {
        User user = requireSupport(auth);
        SupportTicketViewSupport.SupportTicketData data = SupportTicketViewSupport.buildTicketData(user);
        String normalizedView = normalizeView(view);
        List<Ticket> tickets = TicketSearchSupport.combineTickets(data.assignedTickets(), data.openTickets(),
                data.closedTickets());
        return new TicketSuggestionResponse(
                TicketSearchSupport.suggestTickets(tickets, q, 8).stream().map(ticket -> new TicketSuggestion(ticket.id,
                        ticket.name, ticket.displayTitle(), "/support/tickets/" + ticket.id)).toList());
    }

    @GET
    @Path("/bootstrap")
    @Transactional
    public SupportTicketBootstrapResponse bootstrap(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("companyId") Long companyId, @QueryParam("companyEntitlementId") Long companyEntitlementId) {
        User user = requireSupport(auth);
        SupportTicketViewSupport.SupportTicketCounts counts = SupportTicketViewSupport.loadTicketCounts(user);
        List<Company> companies = Company.list("order by name");
        Company selectedCompany = selectCompany(companies, companyId);
        List<CompanyEntitlement> entitlements = selectedCompany == null ? List.of()
                : SupportTicketViewSupport.uniqueEntitlements(CompanyEntitlement.find(
                        "select distinct ce from CompanyEntitlement ce join fetch ce.entitlement join fetch ce.supportLevel where ce.company = ?1 order by ce.entitlement.name, ce.supportLevel.level, ce.supportLevel.id",
                        selectedCompany).list());
        CompanyEntitlement selectedEntitlement = selectEntitlement(entitlements, companyEntitlementId);
        Long selectedCompanyEntitlementId = selectedEntitlement == null ? null : selectedEntitlement.id;
        Version defaultAffectsVersion = SupportTicketViewSupport.defaultAffectsVersion(selectedEntitlement);
        List<VersionOption> versions = SupportTicketViewSupport.availableVersions(selectedEntitlement).stream()
                .map(version -> new VersionOption(version.id, version.name,
                        version.date == null ? null : version.date.toString()))
                .toList();
        Category defaultCategory = Category.findDefault();
        return new SupportTicketBootstrapResponse(counts.assignedCount(), counts.openCount(),
                selectedCompany == null ? null : selectedCompany.id,
                selectedCompany == null ? "" : Ticket.previewNextName(selectedCompany), "",
                companies.stream().map(company -> new CompanyOption(company.id, company.name)).toList(),
                entitlements.stream().map(this::toEntitlementOption).toList(), selectedCompanyEntitlementId,
                Category.<Category> list("order by name").stream().map(this::toCategoryOption).toList(),
                defaultCategory == null ? null : defaultCategory.id,
                defaultAffectsVersion == null ? null
                        : new VersionOption(defaultAffectsVersion.id, defaultAffectsVersion.name,
                                defaultAffectsVersion.date == null ? null : defaultAffectsVersion.date.toString()),
                versions, "/support/tickets");
    }

    @GET
    @Path("/{id}")
    @Transactional
    public SupportTicketDetailResponse detail(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        User user = requireSupport(auth);
        SupportTicketViewSupport.SupportTicketCounts counts = SupportTicketViewSupport.loadTicketCounts(user);
        Ticket ticket = Ticket.findById(id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        assignCompanyTams(ticket);
        String displayStatus = normalizeStatus(ticket.status);
        List<User> supportUsers = User
                .find("select u from Ticket t join t.supportUsers u where t = ?1 order by u.email", ticket).list();
        List<User> tamUsers = User.find(
                "select distinct u from Company c join c.users u where c = ?1 and lower(u.type) = ?2 order by u.email",
                ticket.company, User.TYPE_TAM).list();
        mergeTicketTams(ticket, tamUsers);
        List<Message> messages = SupportTicketViewSupport.loadMessages(ticket);
        List<MessageEntry> messageEntries = messages.stream().map(this::toMessageEntry).toList();
        List<Event> rawEvents = eventService.getAllChangesToEntity(ticket.id.intValue());
        List<EventEntry> eventEntries = rawEvents.stream().map(this::toEventEntry).toList();
        return new SupportTicketDetailResponse(ticket.id, ticket.name, ticket.displayTitle(), displayStatus,
                counts.assignedCount(), counts.openCount(), ticket.company == null ? null : ticket.company.id,
                ticket.company == null ? null : ticket.company.name,
                ticket.category == null ? null : ticket.category.id,
                ticket.category == null ? null : ticket.category.name,
                ticket.companyEntitlement == null ? null : ticket.companyEntitlement.id,
                ticket.companyEntitlement == null || ticket.companyEntitlement.entitlement == null ? null
                        : ticket.companyEntitlement.entitlement.name,
                ticket.companyEntitlement == null || ticket.companyEntitlement.supportLevel == null ? null
                        : ticket.companyEntitlement.supportLevel.name,
                ticket.externalIssueLink, ticket.affectsVersion == null ? null : ticket.affectsVersion.id,
                ticket.resolvedVersion == null ? null : ticket.resolvedVersion.id, Version
                        .<Version> list("entitlement = ?1 order by date asc, id asc",
                                ticket.companyEntitlement == null ? null : ticket.companyEntitlement.entitlement)
                        .stream()
                        .map(version -> new VersionOption(version.id, version.name,
                                version.date == null ? null : version.date.toString()))
                        .toList(),
                Category.<Category> list("order by name").stream().map(this::toCategoryOption).toList(),
                supportUsers.stream().map(this::toUserReference).toList(),
                tamUsers.stream().map(this::toUserReference).toList(), messageEntries,
                SupportTicketViewSupport.isEntitlementExpired(ticket), "/support/tickets/" + ticket.id,
                "/support/tickets/" + ticket.id + "/messages", "/tickets/export/" + ticket.id,
                List.of("Open", "Assigned", "In Progress", "Resolved", "Closed"), eventEntries);
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

    private SupportTicketSummary toSummary(Ticket ticket, SupportTicketViewSupport.SupportTicketData data) {
        User assignedSupport = data.supportAssignmentUsers().get(ticket.id);
        return new SupportTicketSummary(ticket.id, ticket.name, ticket.displayTitle(), ticket.status,
                data.messageDateLabels().get(ticket.id), data.messageDirectionArrows().get(ticket.id),
                data.slaColors().get(ticket.id), ticket.category == null ? null : ticket.category.name,
                assignedSupport == null ? null : toUserReference(assignedSupport),
                ticket.company == null ? null : ticket.company.id, ticket.company == null ? null : ticket.company.name,
                ticket.companyEntitlement == null || ticket.companyEntitlement.entitlement == null ? null
                        : ticket.companyEntitlement.entitlement.name,
                ticket.companyEntitlement == null || ticket.companyEntitlement.supportLevel == null ? null
                        : ticket.companyEntitlement.supportLevel.name,
                ticket.affectsVersion == null ? null : ticket.affectsVersion.name,
                ticket.resolvedVersion == null ? null : ticket.resolvedVersion.name, "/support/tickets/" + ticket.id,
                ticket.company == null ? null : "/support/companies/" + ticket.company.id);
    }

    private EntitlementOption toEntitlementOption(CompanyEntitlement entry) {
        return new EntitlementOption(entry.id, entry.entitlement == null ? null : entry.entitlement.name,
                entry.supportLevel == null ? null : entry.supportLevel.name);
    }

    private CategoryOption toCategoryOption(Category category) {
        return new CategoryOption(category.id, category.name);
    }

    private UserReference toUserReference(User user) {
        return new UserReference(user.id, user.name, user.getDisplayName(), user.fullName, user.email, user.type,
                user.country == null ? null : user.country.name, user.timezone == null ? null : user.timezone.name,
                user.logoBase64, userPath(user));
    }

    private MessageEntry toMessageEntry(Message message) {
        return new MessageEntry(message.id, message.body,
                message.date == null ? null : SupportTicketViewSupport.formatDate(message.date),
                message.author == null ? null : toUserReference(message.author), message.attachments == null ? List.of()
                        : message.attachments.stream().map(this::toAttachmentEntry).toList());
    }

    private EventEntry toEventEntry(Event event) {
        return new EventEntry(event.id, event.action == null ? null : event.action.name(),
                event.createdAt == null ? null : SupportTicketViewSupport.formatDate(event.createdAt),
                event.user == null ? null : toUserReference(event.user));
    }

    private AttachmentEntry toAttachmentEntry(Attachment attachment) {
        return new AttachmentEntry(attachment.id, attachment.name, attachment.mimeType, attachment.sizeLabel(),
                "/attachments/" + attachment.id);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Open";
        }
        return status;
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

    private void mergeTicketTams(Ticket ticket, List<User> tamUsers) {
        List<User> ticketTams = User
                .find("select u from Ticket t join t.tamUsers u where t = ?1 order by u.email", ticket).list();
        if (ticketTams.isEmpty()) {
            return;
        }
        Set<Long> seenIds = new HashSet<>();
        for (User existing : tamUsers) {
            if (existing.id != null) {
                seenIds.add(existing.id);
            }
        }
        for (User existing : ticketTams) {
            if (existing.id != null && !seenIds.contains(existing.id)) {
                tamUsers.add(existing);
            }
        }
    }

    private String userPath(User user) {
        if (user == null || user.id == null || user.type == null) {
            return null;
        }
        String type = user.type.trim().toLowerCase(Locale.ENGLISH);
        return switch (type) {
            case User.TYPE_SUPPORT -> "/support/support-users/" + user.id;
            case User.TYPE_SUPERUSER -> "/support/superuser-users/" + user.id;
            case User.TYPE_TAM -> "/support/tam-users/" + user.id;
            default -> "/support/user-profiles/" + user.id;
        };
    }

    private User requireSupport(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isSupport(user)) {
            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        return user;
    }

    public record SupportTicketListResponse(String view, String title, int assignedCount, int openCount,
            String createPath, String searchTerm, List<SupportTicketSummary> items) {
    }

    public record TicketSuggestionResponse(List<TicketSuggestion> items) {
    }

    public record TicketSuggestion(Long id, String name, String title, String detailPath) {
    }

    public record SupportTicketSummary(Long id, String name, String title, String status, String messageDateLabel,
            String messageDirectionArrow, String slaColor, String categoryName, UserReference supportUser,
            Long companyId, String companyName, String entitlementName, String levelName, String affectsVersionName,
            String resolvedVersionName, String detailPath, String companyPath) {
    }

    public record SupportTicketBootstrapResponse(int assignedCount, int openCount, Long selectedCompanyId,
            String ticketName, String title, List<CompanyOption> companies, List<EntitlementOption> companyEntitlements,
            Long selectedCompanyEntitlementId, List<CategoryOption> categories, Long defaultCategoryId,
            VersionOption defaultAffectsVersion, List<VersionOption> versions, String submitPath) {
    }

    public record RedirectResponse(String redirectTo) {
    }

    public record CompanyOption(Long id, String name) {
    }

    public record EntitlementOption(Long id, String name, String levelName) {
    }

    public record CategoryOption(Long id, String name) {
    }

    public record VersionOption(Long id, String name, String date) {
    }

    public record UserReference(Long id, String username, String displayName, String fullName, String email,
            String type, String countryName, String timezoneName, String logoBase64, String detailPath) {
    }

    public record MessageEntry(Long id, String body, String dateLabel, UserReference author,
            List<AttachmentEntry> attachments) {
    }

    public record EventEntry(Long id, String action, String dateLabel, UserReference user) {
    }

    public record AttachmentEntry(Long id, String name, String mimeType, String sizeLabel, String downloadPath) {
    }

    public record SupportTicketDetailResponse(Long id, String name, String title, String displayStatus,
            int assignedCount, int openCount, Long companyId, String companyName, Long categoryId, String categoryName,
            Long companyEntitlementId, String entitlementName, String levelName, String externalIssueLink,
            Long affectsVersionId, Long resolvedVersionId, List<VersionOption> versions,
            List<CategoryOption> categories, List<UserReference> supportUsers, List<UserReference> tamUsers,
            List<MessageEntry> messages, boolean ticketEntitlementExpired, String actionPath, String messageActionPath,
            String exportPath, List<String> statusOptions, List<EventEntry> events) {
    }
}
