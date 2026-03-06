/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.Attachment;
import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.Version;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class SuperuserResource {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d yyyy, h.mma",
            Locale.ENGLISH);

    @Location("superuser/tickets.html")
    Template ticketsTemplate;

    @Location("superuser/ticket-create.html")
    Template ticketCreateTemplate;

    @Location("superuser/ticket-detail.html")
    Template ticketDetailTemplate;

    @Location("superuser/ticket-edit.html")
    Template ticketEditTemplate;

    @Location("superuser/users.html")
    Template usersTemplate;

    @Location("superuser/user-form.html")
    Template userFormTemplate;

    @Location("superuser/support-user-view.html")
    Template supportUserViewTemplate;

    @Location("superuser/user-profile-view.html")
    Template userProfileViewTemplate;

    @Location("superuser/superuser-user-view.html")
    Template superuserUserViewTemplate;

    @Location("superuser/company-view.html")
    Template companyViewTemplate;

    @Inject
    TicketEmailService ticketEmailService;

    @GET
    @Path("superuser")
    public TemplateInstance home(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireSuperuser(auth);
        return scopedTickets(user);
    }

    @GET
    @Path("superuser/users")
    public Response usersRoot(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireSuperuser(auth);
        List<Company> companies = userCompanies(user);
        if (companies.isEmpty() || companies.get(0) == null || companies.get(0).id == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/superuser/users/" + companies.get(0).id)).build();
    }

    @GET
    @Path("superuser/users/{companyId}")
    public Response listUsers(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("companyId") Long companyId) {
        User user = requireSuperuser(auth);
        Company company = allowedCompany(user, companyId);
        SupportTicketData data = buildTicketDataForUser(user);
        List<User> users = Company.find("select u from Company c join c.users u where c = ?1 order by u.name", company)
                .list();
        return Response.ok(usersTemplate.data("users", users).data("companies", userCompanies(user))
                .data("selectedCompanyId", company.id).data("selectedCompany", company)
                .data("showCompanySelector", false).data("createUserUrl", "/superuser/users/" + company.id + "/create")
                .data("usersBase", "/superuser/users").data("assignedCount", data.assignedTickets.size())
                .data("openCount", data.openTickets.size()).data("ticketsBase", "/superuser/tickets")
                .data("showSupportUsers", true).data("currentUser", user)).build();
    }

    @GET
    @Path("superuser/users/{companyId}/create")
    public TemplateInstance createUserForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("companyId") Long companyId) {
        User user = requireSuperuser(auth);
        Company company = allowedCompany(user, companyId);
        SupportTicketData data = buildTicketDataForUser(user);
        User newUser = new User();
        newUser.type = User.TYPE_USER;
        Country defaultCountry = Country.find("code", "US").firstResult();
        newUser.country = defaultCountry;
        newUser.timezone = defaultCountry != null
                ? Timezone.find("country = ?1 and name = ?2", defaultCountry, "America/New_York").firstResult()
                : null;
        List<Country> countries = Country.list("order by name");
        List<Timezone> timezones = defaultCountry != null ? Timezone.list("country = ?1 order by name", defaultCountry)
                : List.of();
        return userFormTemplate.data("user", newUser).data("companies", userCompanies(user))
                .data("selectedCompanyId", company.id).data("companyLocked", userCompanies(user).size() <= 1)
                .data("types", List.of(User.TYPE_USER)).data("action", "/superuser/users").data("title", "New user")
                .data("countries", countries).data("timezones", timezones)
                .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                .data("ticketsBase", "/superuser/tickets").data("showSupportUsers", true)
                .data("usersBase", "/superuser/users").data("currentUser", user);
    }

    @POST
    @Path("superuser/users")
    @Transactional
    public Response createUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @FormParam("name") String name,
            @FormParam("fullName") String fullName, @FormParam("email") String email,
            @FormParam("social") String social, @FormParam("phoneNumber") String phoneNumber,
            @FormParam("phoneExtension") String phoneExtension, @FormParam("timezoneId") Long timezoneId,
            @FormParam("countryId") Long countryId, @FormParam("password") String password,
            @FormParam("type") String type, @FormParam("companyId") Long companyId) {
        User user = requireSuperuser(auth);
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Username is required");
        }
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required");
        }
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Password is required");
        }
        Company company = allowedCompany(user, companyId);
        String normalized = normalizeType(type, Set.of(User.TYPE_USER), "Type must be user");
        User newUser = new User();
        newUser.name = name.trim();
        newUser.fullName = trimOrNull(fullName);
        newUser.email = email.trim();
        newUser.social = trimOrNull(social);
        newUser.phoneNumber = trimOrNull(phoneNumber);
        newUser.phoneExtension = trimOrNull(phoneExtension);
        newUser.timezone = timezoneId != null ? Timezone.findById(timezoneId) : null;
        newUser.country = countryId != null ? Country.findById(countryId) : null;
        newUser.type = normalized;
        newUser.passwordHash = BcryptUtil.bcryptHash(password);
        newUser.persist();
        addUserIfMissing(company, newUser);
        return Response.seeOther(URI.create("/superuser/users/" + company.id)).build();
    }

    @GET
    @Path("superuser/tickets")
    public TemplateInstance tickets(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireSuperuser(auth);
        return scopedTickets(user);
    }

    @GET
    @Path("superuser/tickets/open")
    public Response openTickets(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireSuperuser(auth);
        SupportTicketData data = buildTicketDataForUser(user);
        return Response
                .ok(ticketsTemplate.data("tickets", data.openTickets).data("pageTitle", "Open tickets")
                        .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                        .data("messageDates", data.messageDates).data("messageDateLabels", data.messageDateLabels)
                        .data("messageDirectionArrows", data.messageDirectionArrows).data("slaColors", data.slaColors)
                        .data("supportAssignments", data.supportAssignments)
                        .data("supportAssignmentNames", data.supportAssignmentNames)
                        .data("supportAssignmentIds", data.supportAssignmentIds)
                        .data("createTicketUrl", "/superuser/tickets/create").data("ticketsBase", "/superuser/tickets")
                        .data("showSupportUsers", true).data("usersBase", "/superuser/users").data("currentUser", user))
                .build();
    }

    @GET
    @Path("superuser/tickets/closed")
    public Response closedTickets(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireSuperuser(auth);
        SupportTicketData data = buildTicketDataForUser(user);
        return Response
                .ok(ticketsTemplate.data("tickets", data.closedTickets).data("pageTitle", "Closed tickets")
                        .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                        .data("messageDates", data.messageDates).data("messageDateLabels", data.messageDateLabels)
                        .data("messageDirectionArrows", data.messageDirectionArrows).data("slaColors", data.slaColors)
                        .data("supportAssignments", data.supportAssignments)
                        .data("supportAssignmentNames", data.supportAssignmentNames)
                        .data("supportAssignmentIds", data.supportAssignmentIds)
                        .data("createTicketUrl", "/superuser/tickets/create").data("ticketsBase", "/superuser/tickets")
                        .data("showSupportUsers", true).data("usersBase", "/superuser/users").data("currentUser", user))
                .build();
    }

    @GET
    @Path("superuser/tickets/create")
    public TemplateInstance createTicketForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireSuperuser(auth);
        SupportTicketData data = buildTicketDataForUser(user);
        List<Company> companies = userCompanies(user);
        Company company = companies.isEmpty() ? null : companies.get(0);
        List<CompanyEntitlement> entitlements = company == null ? List.of()
                : CompanyEntitlement.find(
                        "select distinct ce from CompanyEntitlement ce join fetch ce.entitlement join fetch ce.supportLevel where ce.company = ?1",
                        company).list();
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
        Set<Long> expiredEntitlementIds = new LinkedHashSet<>();
        for (CompanyEntitlement entitlement : uniqueEntitlements) {
            if (isEntitlementExpired(entitlement) && entitlement.id != null) {
                expiredEntitlementIds.add(entitlement.id);
            }
        }
        Category defaultCategory = Category.findDefault();
        return ticketCreateTemplate.data("companyEntitlements", uniqueEntitlements)
                .data("ticketName", company == null ? "" : Ticket.previewNextName(company))
                .data("expiredEntitlementIds", expiredEntitlementIds).data("assignedCount", data.assignedTickets.size())
                .data("openCount", data.openTickets.size()).data("ticketsBase", "/superuser/tickets")
                .data("versions", knownVersions())
                .data("defaultAffectsVersion",
                        uniqueEntitlements.isEmpty() ? null : defaultAffectsVersion(uniqueEntitlements.get(0)))
                .data("showSupportUsers", true).data("usersBase", "/superuser/users").data("currentUser", user)
                .data("categories", Category.listAll())
                .data("defaultCategoryId", defaultCategory == null ? null : defaultCategory.id);
    }

    @POST
    @Path("superuser/tickets")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response createTicket(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, MultipartFormDataInput input) {
        User user = requireSuperuser(auth);
        String status = AttachmentHelper.readFormValue(input, "status");
        String messageBody = AttachmentHelper.readFormValue(input, "message");
        Long companyEntitlementId = AttachmentHelper.readFormLong(input, "companyEntitlementId");
        Long affectsVersionId = AttachmentHelper.readFormLong(input, "affectsVersionId");
        Long categoryId = AttachmentHelper.readFormLong(input, "categoryId");
        if (status == null || status.isBlank()) {
            throw new BadRequestException("Status is required");
        }
        if (!"Open".equalsIgnoreCase(status)) {
            throw new BadRequestException("Status must be Open");
        }
        if (messageBody == null || messageBody.isBlank()) {
            throw new BadRequestException("Message is required");
        }
        if (companyEntitlementId == null) {
            throw new BadRequestException("Entitlement is required");
        }
        CompanyEntitlement entitlement = CompanyEntitlement.findById(companyEntitlementId);
        if (entitlement == null || entitlement.company == null) {
            throw new BadRequestException("Entitlement is required");
        }
        allowedCompany(user, entitlement.company.id);
        if (isEntitlementExpired(entitlement)) {
            throw new BadRequestException("Entitlement is expired");
        }
        Ticket ticket = new Ticket();
        ticket.name = Ticket.nextName(entitlement.company);
        ticket.status = "Open";
        ticket.company = entitlement.company;
        ticket.requester = user;
        ticket.companyEntitlement = entitlement;
        ticket.affectsVersion = resolveKnownVersion(affectsVersionId, "Affects");
        ticket.category = categoryId != null ? Category.findById(categoryId) : Category.findDefault();
        ticket.persist();
        Message message = new Message();
        message.body = messageBody.trim();
        message.date = LocalDateTime.now();
        message.ticket = ticket;
        message.author = user;
        List<Attachment> attachments = AttachmentHelper.readAttachments(input, "attachments");
        AttachmentHelper.attachToMessage(message, attachments);
        message.persistAndFlush();
        AttachmentHelper.resolveInlineAttachmentUrls(message, attachments);
        ticketEmailService.notifyMessageChange(ticket, message, user);
        return Response.seeOther(URI.create("/superuser/tickets")).build();
    }

    @GET
    @Path("superuser/support-users/{id}")
    public TemplateInstance viewSupportUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        User user = requireSuperuser(auth);
        User supportUser = User.findById(id);
        if (supportUser == null || !User.TYPE_SUPPORT.equalsIgnoreCase(supportUser.type)) {
            throw new NotFoundException();
        }
        SupportTicketData data = buildTicketDataForUser(user);
        return supportUserViewTemplate.data("supportUser", supportUser)
                .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                .data("ticketsBase", "/superuser/tickets").data("showSupportUsers", true)
                .data("usersBase", "/superuser/users").data("currentUser", user);
    }

    @GET
    @Path("superuser/superuser-users/{id}")
    public TemplateInstance viewSuperuser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireSuperuser(auth);
        User viewedUser = User.findById(id);
        if (viewedUser == null || !User.TYPE_SUPERUSER.equalsIgnoreCase(viewedUser.type)) {
            throw new NotFoundException();
        }
        boolean allowed = Company.count(
                "select count(c) from Company c join c.users current join c.users viewed where current = ?1 and viewed = ?2",
                user, viewedUser) > 0;
        if (!allowed && (user.id == null || !user.id.equals(viewedUser.id))) {
            throw new NotFoundException();
        }
        SupportTicketData data = buildTicketDataForUser(user);
        return superuserUserViewTemplate.data("superuser", viewedUser)
                .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                .data("ticketsBase", "/superuser/tickets").data("showSupportUsers", true)
                .data("usersBase", "/superuser/users").data("currentUser", user);
    }

    @GET
    @Path("superuser/user-profiles/{id}")
    public TemplateInstance viewUserProfile(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        User user = requireSuperuser(auth);
        User viewedUser = User.findById(id);
        if (viewedUser == null) {
            throw new NotFoundException();
        }
        boolean allowed = Company.count(
                "select count(c) from Company c join c.users current join c.users viewed where current = ?1 and viewed = ?2",
                user, viewedUser) > 0;
        if (!allowed) {
            throw new NotFoundException();
        }
        SupportTicketData data = buildTicketDataForUser(user);
        return userProfileViewTemplate.data("viewedUser", viewedUser).data("assignedCount", data.assignedTickets.size())
                .data("openCount", data.openTickets.size()).data("ticketsBase", "/superuser/tickets")
                .data("showSupportUsers", true).data("usersBase", "/superuser/users").data("currentUser", user);
    }

    @GET
    @Path("superuser/companies/{id}")
    public TemplateInstance viewCompany(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireSuperuser(auth);
        Company company = allowedCompany(user, id);
        List<User> users = User.find(
                "select distinct u from Company c join c.users u where c = ?1 and lower(u.type) = ?2 order by u.name",
                company, User.TYPE_USER).list();
        List<User> superusers = User.find(
                "select distinct u from Company c join c.users u where c = ?1 and lower(u.type) = ?2 order by u.name",
                company, User.TYPE_SUPERUSER).list();
        SupportTicketData data = buildTicketDataForUser(user);
        return companyViewTemplate.data("company", company).data("users", users).data("superusers", superusers)
                .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                .data("ticketsBase", "/superuser/tickets").data("showSupportUsers", true)
                .data("usersBase", "/superuser/users").data("currentUser", user);
    }

    @GET
    @Path("superuser/tickets/{id}")
    public TemplateInstance ticketDetail(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireSuperuser(auth);
        return renderTicketDetail(user, id);
    }

    @POST
    @Path("superuser/tickets/{id}/messages")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response addMessage(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            MultipartFormDataInput input) {
        User user = requireSuperuser(auth);
        String body = AttachmentHelper.readFormValue(input, "body");
        if (body == null || body.isBlank()) {
            throw new BadRequestException("Message is required");
        }
        Ticket ticket = findTicketForSuperuser(user, id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        Message message = new Message();
        message.body = body;
        message.date = LocalDateTime.now();
        message.ticket = ticket;
        message.author = user;
        List<Attachment> attachments = AttachmentHelper.readAttachments(input, "attachments");
        AttachmentHelper.attachToMessage(message, attachments);
        message.persistAndFlush();
        AttachmentHelper.resolveInlineAttachmentUrls(message, attachments);
        ticketEmailService.notifyMessageChange(ticket, message, user);
        return Response.seeOther(URI.create("/superuser/tickets/" + id)).build();
    }

    @GET
    @Path("superuser/tickets/{id}/edit")
    public TemplateInstance ticketEdit(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireSuperuser(auth);
        Ticket ticket = findTicketForSuperuser(user, id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        return ticketEditTemplate.data("ticket", ticket).data("versions", availableVersions(ticket))
                .data("allowResolvedEdit", true).data("currentUser", user)
                .data("assignedCount", loadTicketCounts(user).assignedCount)
                .data("openCount", loadTicketCounts(user).openCount).data("ticketsBase", "/superuser/tickets")
                .data("showSupportUsers", true).data("usersBase", "/superuser/users");
    }

    @POST
    @Path("superuser/tickets/{id}")
    @Transactional
    public Response updateTicket(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            @FormParam("affectsVersionId") Long affectsVersionId,
            @FormParam("resolvedVersionId") Long resolvedVersionId) {
        User user = requireSuperuser(auth);
        Ticket ticket = findTicketForSuperuser(user, id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        ticket.affectsVersion = resolveVersionForTicket(ticket, affectsVersionId, "Affects");
        ticket.resolvedVersion = resolveOptionalVersionForTicket(ticket, resolvedVersionId, "Resolved");
        return Response.seeOther(URI.create("/superuser/tickets/" + id)).build();
    }

    static List<Ticket> loadScopedTickets(User user) {
        if (user == null || !User.TYPE_SUPERUSER.equalsIgnoreCase(user.type)) {
            return List.of();
        }
        return Ticket.find("select distinct t from Ticket t join t.company c join c.users u where u = ?1", user).list();
    }

    static SupportResource.SupportTicketCounts loadTicketCounts(User user) {
        int assigned = 0;
        int open = 0;
        for (Ticket ticket : loadScopedTickets(user)) {
            if (ticket == null || "Closed".equalsIgnoreCase(ticket.status)) {
                continue;
            }
            Long hasSupport = Ticket.count("select count(u) from Ticket t join t.supportUsers u where t = ?1", ticket);
            if (hasSupport != null && hasSupport > 0) {
                assigned++;
            } else {
                open++;
            }
        }
        return new SupportResource.SupportTicketCounts(assigned, open);
    }

    private TemplateInstance scopedTickets(User user) {
        SupportTicketData data = buildTicketDataForUser(user);
        return ticketsTemplate.data("tickets", data.assignedTickets).data("pageTitle", "Tickets")
                .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                .data("messageDates", data.messageDates).data("messageDateLabels", data.messageDateLabels)
                .data("messageDirectionArrows", data.messageDirectionArrows).data("slaColors", data.slaColors)
                .data("supportAssignments", data.supportAssignments)
                .data("supportAssignmentNames", data.supportAssignmentNames)
                .data("supportAssignmentIds", data.supportAssignmentIds)
                .data("createTicketUrl", "/superuser/tickets/create").data("ticketsBase", "/superuser/tickets")
                .data("showSupportUsers", true).data("usersBase", "/superuser/users").data("currentUser", user);
    }

    private TemplateInstance renderTicketDetail(User user, Long id) {
        Ticket ticket = findTicketForSuperuser(user, id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        List<Message> messages = loadMessages(ticket);
        Map<Long, String> messageLabels = new HashMap<>();
        Map<Long, String> messageAuthorNames = new HashMap<>();
        Map<Long, String> messageAuthorLinks = new HashMap<>();
        for (Message message : messages) {
            if (message.date != null && message.id != null) {
                messageLabels.put(message.id, formatDate(message.date));
            }
            if (message.author != null && message.author.id != null) {
                messageAuthorNames.put(message.id, message.author.name);
                if (User.TYPE_SUPPORT.equalsIgnoreCase(message.author.type)) {
                    messageAuthorLinks.put(message.id, "/superuser/support-users/" + message.author.id);
                } else if (User.TYPE_SUPERUSER.equalsIgnoreCase(message.author.type)) {
                    messageAuthorLinks.put(message.id, "/superuser/superuser-users/" + message.author.id);
                } else {
                    messageAuthorLinks.put(message.id, "/superuser/user-profiles/" + message.author.id);
                }
            }
        }
        List<User> supportUsers = User
                .find("select u from Ticket t join t.supportUsers u where t = ?1 order by u.email", ticket).list();
        List<User> superusers = ticket.company == null ? new ArrayList<>()
                : User.find(
                        "select distinct u from Company c join c.users u where c = ?1 and lower(u.type) = ?2 order by u.email",
                        ticket.company, User.TYPE_SUPERUSER).list();
        SupportTicketData data = buildTicketDataForUser(user);
        return ticketDetailTemplate.data("ticket", ticket).data("displayStatus", ticket.status)
                .data("supportUsers", supportUsers).data("superusers", superusers).data("messages", messages)
                .data("messageLabels", messageLabels).data("messageAuthorNames", messageAuthorNames)
                .data("messageAuthorLinks", messageAuthorLinks).data("action", "/superuser/tickets/" + id)
                .data("editableStatus", false).data("supportUserBase", "/superuser/support-users")
                .data("ticketEntitlementExpired", isEntitlementExpired(ticket))
                .data("superuserUserBase", "/superuser/superuser-users").data("companyBase", "/superuser/companies")
                .data("showLevel", true).data("levelName", resolveLowestEntitlementLevelName(ticket))
                .data("messageAction", "/superuser/tickets/" + id + "/messages")
                .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                .data("ticketsBase", "/superuser/tickets").data("showSupportUsers", true)
                .data("usersBase", "/superuser/users").data("currentUser", user).data("categories", Category.listAll())
                .data("versions", availableVersions(ticket));
    }

    private List<Message> loadMessages(Ticket ticket) {
        List<Message> messages = Message.find(
                "select distinct m from Message m left join fetch m.attachments where m.ticket = ?1 order by m.date desc",
                ticket).list();
        if (messages.isEmpty()) {
            return messages;
        }
        return new ArrayList<>(new LinkedHashSet<>(messages));
    }

    private SupportTicketData buildTicketDataForUser(User user) {
        return buildTicketDataFor(loadScopedTickets(user));
    }

    private SupportTicketData buildTicketDataFor(List<Ticket> tickets) {
        List<Ticket> scopedTickets = tickets == null ? List.of() : tickets;
        Map<Long, LocalDateTime> messageDates = new HashMap<>();
        Map<Long, String> messageDateLabels = new HashMap<>();
        Map<Long, String> messageDirectionArrows = new HashMap<>();
        List<Message> messages = Message.find("order by date desc").list();
        for (Message message : messages) {
            if (message.ticket != null && scopedTickets.contains(message.ticket) && message.ticket.id != null
                    && !messageDates.containsKey(message.ticket.id)) {
                messageDates.put(message.ticket.id, message.date);
                if (message.date != null) {
                    messageDateLabels.put(message.ticket.id, formatDate(message.date));
                }
                messageDirectionArrows.put(message.ticket.id, messageDirectionArrow(message.author));
            }
        }
        for (Ticket ticket : scopedTickets) {
            if (ticket != null && ticket.id != null && !messageDateLabels.containsKey(ticket.id)) {
                messageDateLabels.put(ticket.id, "-");
            }
        }
        Map<Long, String> slaColors = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (Ticket ticket : scopedTickets) {
            if (ticket == null || ticket.id == null) {
                continue;
            }
            if (isEntitlementExpired(ticket)) {
                slaColors.put(ticket.id, "Black");
                continue;
            }
            LocalDateTime messageDate = messageDates.get(ticket.id);
            if (messageDate == null || ticket.companyEntitlement == null
                    || ticket.companyEntitlement.supportLevel == null) {
                continue;
            }
            long minutes = Duration.between(messageDate, now).toMinutes();
            if (minutes < 0) {
                minutes = 0;
            }
            String color = resolveSlaColor(ticket.companyEntitlement.supportLevel, minutes);
            if (color != null && !color.isBlank()) {
                slaColors.put(ticket.id, color);
            }
        }
        Map<Long, String> supportAssignments = new HashMap<>();
        Map<Long, String> supportAssignmentNames = new HashMap<>();
        Map<Long, Long> supportAssignmentIds = new HashMap<>();
        for (Ticket ticket : scopedTickets) {
            User assignedSupport = User
                    .find("select u from Ticket t join t.supportUsers u where t = ?1 order by u.id desc", ticket)
                    .firstResult();
            if (assignedSupport != null && ticket.id != null) {
                supportAssignments.put(ticket.id, assignedSupport.email);
                supportAssignmentNames.put(ticket.id, assignedSupport.name);
                supportAssignmentIds.put(ticket.id, assignedSupport.id);
            }
        }
        List<Ticket> assignedTickets = new ArrayList<>();
        List<Ticket> openTickets = new ArrayList<>();
        List<Ticket> closedTickets = new ArrayList<>();
        for (Ticket ticket : scopedTickets) {
            if (ticket == null) {
                continue;
            }
            boolean hasSupport = ticket.id != null && supportAssignments.containsKey(ticket.id);
            boolean isClosed = "Closed".equalsIgnoreCase(ticket.status);
            if (isClosed) {
                closedTickets.add(copyTicketDisplay(ticket));
            } else if (hasSupport) {
                assignedTickets.add(normalizeOpenAssigned(ticket));
            } else {
                openTickets.add(ticket);
            }
        }
        for (Ticket ticket : closedTickets) {
            if (ticket != null && ticket.id != null && !isEntitlementExpired(ticket)) {
                slaColors.put(ticket.id, "White");
            }
        }
        sortBySla(assignedTickets, slaColors, messageDates);
        sortBySla(openTickets, slaColors, messageDates);
        sortBySla(closedTickets, slaColors, messageDates);
        SupportTicketData data = new SupportTicketData();
        data.assignedTickets = assignedTickets;
        data.openTickets = openTickets;
        data.closedTickets = closedTickets;
        data.messageDates = messageDates;
        data.messageDateLabels = messageDateLabels;
        data.messageDirectionArrows = messageDirectionArrows;
        data.slaColors = slaColors;
        data.supportAssignments = supportAssignments;
        data.supportAssignmentNames = supportAssignmentNames;
        data.supportAssignmentIds = supportAssignmentIds;
        return data;
    }

    private String resolveSlaColor(ai.mnemosyne_systems.model.Level level, long minutes) {
        if (level == null || level.level == null || level.color == null || level.color.isBlank()) {
            return null;
        }
        if (minutes >= level.level.longValue()) {
            return level.color;
        }
        return "White";
    }

    private void sortBySla(List<Ticket> tickets, Map<Long, String> slaColors, Map<Long, LocalDateTime> messageDates) {
        tickets.sort((left, right) -> {
            int leftRank = slaColorRank(slaColors.get(left.id));
            int rightRank = slaColorRank(slaColors.get(right.id));
            if (leftRank != rightRank) {
                return Integer.compare(leftRank, rightRank);
            }
            LocalDateTime leftDate = messageDates.get(left.id);
            LocalDateTime rightDate = messageDates.get(right.id);
            if (leftDate == null && rightDate == null) {
                return 0;
            }
            if (leftDate == null) {
                return 1;
            }
            if (rightDate == null) {
                return -1;
            }
            int dateCompare = rightDate.compareTo(leftDate);
            if (dateCompare != 0) {
                return dateCompare;
            }
            if (left.id == null && right.id == null) {
                return 0;
            }
            if (left.id == null) {
                return 1;
            }
            if (right.id == null) {
                return -1;
            }
            return left.id.compareTo(right.id);
        });
    }

    private int slaColorRank(String color) {
        if (color == null) {
            return 3;
        }
        String normalized = color.trim().toLowerCase(Locale.ENGLISH);
        if ("red".equals(normalized)) {
            return 0;
        }
        if ("yellow".equals(normalized)) {
            return 1;
        }
        if ("white".equals(normalized)) {
            return 2;
        }
        return 3;
    }

    private String messageDirectionArrow(User author) {
        if (author != null && User.TYPE_SUPPORT.equalsIgnoreCase(author.type)) {
            return "\u2190";
        }
        return "\u2192";
    }

    private Ticket normalizeOpenAssigned(Ticket ticket) {
        if (ticket == null || !"Open".equalsIgnoreCase(ticket.status)) {
            return ticket;
        }
        Ticket displayTicket = new Ticket();
        displayTicket.id = ticket.id;
        displayTicket.name = ticket.name;
        displayTicket.status = "Assigned";
        displayTicket.company = ticket.company;
        displayTicket.companyEntitlement = ticket.companyEntitlement;
        displayTicket.affectsVersion = ticket.affectsVersion;
        displayTicket.resolvedVersion = ticket.resolvedVersion;
        displayTicket.category = ticket.category;
        displayTicket.externalIssueLink = ticket.externalIssueLink;
        return displayTicket;
    }

    private Ticket copyTicketDisplay(Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        Ticket displayTicket = new Ticket();
        displayTicket.id = ticket.id;
        displayTicket.name = ticket.name;
        displayTicket.status = ticket.status;
        displayTicket.company = ticket.company;
        displayTicket.companyEntitlement = ticket.companyEntitlement;
        displayTicket.affectsVersion = ticket.affectsVersion;
        displayTicket.resolvedVersion = ticket.resolvedVersion;
        displayTicket.category = ticket.category;
        displayTicket.externalIssueLink = ticket.externalIssueLink;
        return displayTicket;
    }

    private Ticket findTicketForSuperuser(User user, Long id) {
        return Ticket.find("select distinct t from Ticket t join t.company c join c.users u where u = ?1 and t.id = ?2",
                user, id).firstResult();
    }

    private List<Company> userCompanies(User user) {
        return Company.find("select distinct c from Company c join c.users u where u = ?1 order by c.name", user)
                .list();
    }

    private Company allowedCompany(User user, Long companyId) {
        if (companyId == null) {
            throw new BadRequestException("Company is required");
        }
        Company company = Company
                .find("select distinct c from Company c left join fetch c.users where c.id = ?1", companyId)
                .firstResult();
        if (company == null) {
            throw new NotFoundException();
        }
        boolean allowed = company.users.stream().anyMatch(
                existing -> existing != null && existing.id != null && user.id != null && existing.id.equals(user.id));
        if (!allowed) {
            throw new NotFoundException();
        }
        return company;
    }

    private void addUserIfMissing(Company company, User user) {
        boolean exists = company.users.stream()
                .anyMatch(existing -> existing.id != null && existing.id.equals(user.id));
        if (!exists) {
            company.users.add(user);
        }
    }

    private String trimOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeType(String type, Set<String> allowedTypes, String errorMessage) {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ENGLISH);
        if (!allowedTypes.contains(normalized)) {
            throw new BadRequestException(errorMessage);
        }
        return normalized;
    }

    private boolean isEntitlementExpired(Ticket ticket) {
        if (ticket == null || ticket.companyEntitlement == null || ticket.companyEntitlement.date == null
                || ticket.companyEntitlement.duration == null) {
            return false;
        }
        java.time.LocalDate endDate = ticket.companyEntitlement.date;
        if (ticket.companyEntitlement.duration == CompanyEntitlement.DURATION_MONTHLY) {
            endDate = endDate.plusMonths(1);
        } else if (ticket.companyEntitlement.duration == CompanyEntitlement.DURATION_YEARLY) {
            endDate = endDate.plusYears(1);
        } else {
            return false;
        }
        return java.time.LocalDate.now().isAfter(endDate);
    }

    private boolean isEntitlementExpired(CompanyEntitlement entitlement) {
        if (entitlement == null || entitlement.date == null || entitlement.duration == null) {
            return false;
        }
        java.time.LocalDate endDate = entitlement.date;
        if (entitlement.duration == CompanyEntitlement.DURATION_MONTHLY) {
            endDate = endDate.plusMonths(1);
        } else if (entitlement.duration == CompanyEntitlement.DURATION_YEARLY) {
            endDate = endDate.plusYears(1);
        } else {
            return false;
        }
        return java.time.LocalDate.now().isAfter(endDate);
    }

    private String resolveLowestEntitlementLevelName(Ticket ticket) {
        if (ticket == null || ticket.companyEntitlement == null || ticket.companyEntitlement.entitlement == null) {
            return null;
        }
        ai.mnemosyne_systems.model.Level level = ai.mnemosyne_systems.model.Level
                .find("select l from Entitlement e join e.supportLevels l where e = ?1 order by l.level asc, l.id asc",
                        ticket.companyEntitlement.entitlement)
                .firstResult();
        return level == null ? null : level.name;
    }

    private List<Version> availableVersions(Ticket ticket) {
        if (ticket == null || ticket.companyEntitlement == null || ticket.companyEntitlement.entitlement == null) {
            return List.of();
        }
        return Version.list("entitlement = ?1 order by date asc, id asc", ticket.companyEntitlement.entitlement);
    }

    private Version defaultAffectsVersion(CompanyEntitlement entitlement) {
        if (entitlement == null || entitlement.entitlement == null) {
            return null;
        }
        Version version = Version
                .find("entitlement = ?1 and name = ?2 order by date asc, id asc", entitlement.entitlement, "1.0.0")
                .firstResult();
        if (version != null) {
            return version;
        }
        return Version.find("entitlement = ?1 order by date asc, id asc", entitlement.entitlement).firstResult();
    }

    private List<Version> knownVersions() {
        java.util.Map<String, Version> versionsByRelease = new java.util.LinkedHashMap<>();
        for (Version version : Version.<Version> list("order by date asc, id asc")) {
            String key = version.name + "|" + version.date;
            versionsByRelease.putIfAbsent(key, version);
        }
        return new java.util.ArrayList<>(versionsByRelease.values());
    }

    private Version resolveKnownVersion(Long versionId, String label) {
        if (versionId == null) {
            if (!knownVersions().isEmpty()) {
                throw new BadRequestException(label + " version is required");
            }
            return null;
        }
        Version version = Version.findById(versionId);
        if (version == null) {
            throw new BadRequestException(label + " version is invalid");
        }
        return version;
    }

    private Version resolveVersionForTicket(Ticket ticket, Long versionId, String label) {
        Version version = resolveOptionalVersionForTicket(ticket, versionId, label);
        if (version == null && !availableVersions(ticket).isEmpty()) {
            throw new BadRequestException(label + " version is required");
        }
        return version;
    }

    private Version resolveOptionalVersionForTicket(Ticket ticket, Long versionId, String label) {
        if (versionId == null) {
            return null;
        }
        if (ticket == null || ticket.companyEntitlement == null || ticket.companyEntitlement.entitlement == null) {
            throw new BadRequestException(label + " version is invalid");
        }
        Version version = Version.find("id = ?1 and entitlement = ?2", versionId, ticket.companyEntitlement.entitlement)
                .firstResult();
        if (version == null) {
            throw new BadRequestException(label + " version is invalid");
        }
        return version;
    }

    private String formatDate(LocalDateTime date) {
        String formatted = DATE_FORMATTER.format(date);
        return formatted.replace("AM", "am").replace("PM", "pm");
    }

    private User requireSuperuser(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isSuperuser(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }

    private static class SupportTicketData {
        private List<Ticket> assignedTickets;
        private List<Ticket> openTickets;
        private List<Ticket> closedTickets;
        private Map<Long, LocalDateTime> messageDates;
        private Map<Long, String> messageDateLabels;
        private Map<Long, String> messageDirectionArrows;
        private Map<Long, String> slaColors;
        private Map<Long, String> supportAssignments;
        private Map<Long, String> supportAssignmentNames;
        private Map<Long, Long> supportAssignmentIds;
    }
}
