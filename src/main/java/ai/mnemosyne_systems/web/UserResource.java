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
import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Timezone;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class UserResource {

    @Location("user/home.html")
    Template homeTemplate;

    @Location("user/tickets.html")
    Template ticketsTemplate;

    @Location("user/ticket-create.html")
    Template ticketCreateTemplate;

    @Location("user/ticket-detail.html")
    Template ticketDetailTemplate;

    @Location("support/ticket-detail.html")
    Template tamTicketDetailTemplate;

    @Location("user/ticket-edit.html")
    Template ticketEditTemplate;

    @Location("user/users.html")
    Template adminUsersTemplate;

    @Location("user/user-form.html")
    Template adminUserFormTemplate;

    @Location("user/user-view.html")
    Template adminUserViewTemplate;

    @Location("support/users.html")
    Template supportUsersTemplate;

    @Location("support/user-form.html")
    Template supportUserFormTemplate;

    @Location("support/support-user-view.html")
    Template supportUserViewTemplate;

    @Location("support/tam-user-view.html")
    Template tamUserViewTemplate;

    @Location("support/user-profile-view.html")
    Template userProfileViewTemplate;

    @Inject
    TicketEmailService ticketEmailService;

    @GET
    @Path("user")
    public TemplateInstance home(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireUser(auth);
        return userTickets(user);
    }

    @GET
    @Path("tam/users")
    public Response tamUsersRoot(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireUser(auth);
        if (!User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/user")).build());
        }
        java.util.List<Company> companies = Company
                .find("select distinct c from Company c join c.users u where u = ?1", user).list();
        if (companies.isEmpty()) {
            throw new NotFoundException();
        }
        Company selectedCompany = companies.get(0);
        if (selectedCompany == null || selectedCompany.id == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/tam/users/" + selectedCompany.id)).build();
    }

    @GET
    @Path("tam/users/{companyId}")
    public Response listTamUsers(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("companyId") Long companyId) {
        User user = requireUser(auth);
        if (!User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/user")).build());
        }
        SupportTicketData data = buildTicketDataForUser(user);
        java.util.List<Company> companies = Company
                .find("select distinct c from Company c join c.users u where u = ?1", user).list();
        Company selectedCompany = null;
        selectedCompany = Company.findById(companyId);
        if (selectedCompany == null) {
            throw new NotFoundException();
        }
        boolean allowed = Company.count("select count(c) from Company c join c.users u where c = ?1 and u = ?2",
                selectedCompany, user) > 0;
        if (!allowed) {
            throw new NotFoundException();
        }
        java.util.List<User> users = selectedCompany == null ? java.util.List.of() : Company
                .find("select u from Company c join c.users u where c = ?1 order by u.name", selectedCompany).list();
        String createUserUrl = selectedCompany == null ? "/tam/users" : "/tam/users/" + selectedCompany.id + "/create";
        return Response
                .ok(supportUsersTemplate.data("users", users).data("companies", companies)
                        .data("selectedCompanyId", selectedCompany == null ? null : selectedCompany.id)
                        .data("selectedCompany", selectedCompany).data("showCompanySelector", false)
                        .data("createUserUrl", createUserUrl).data("usersBase", "/tam/users")
                        .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                        .data("ticketsBase", "/user/tickets").data("showSupportUsers", true).data("currentUser", user))
                .build();
    }

    @GET
    @Path("tam/users/{companyId}/create")
    public TemplateInstance createTamUserForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("companyId") Long companyId) {
        User user = requireUser(auth);
        if (!User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/user")).build());
        }
        SupportTicketData data = buildTicketDataForUser(user);
        java.util.List<Company> companies = Company
                .find("select distinct c from Company c join c.users u where u = ?1", user).list();
        Company selectedCompany = Company.findById(companyId);
        if (selectedCompany == null) {
            throw new NotFoundException();
        }
        boolean allowed = Company.count("select count(c) from Company c join c.users u where c = ?1 and u = ?2",
                selectedCompany, user) > 0;
        if (!allowed) {
            throw new NotFoundException();
        }
        User newUser = new User();
        newUser.type = User.TYPE_USER;
        Country defaultCountry = Country.find("code", "US").firstResult();
        newUser.country = defaultCountry;
        newUser.timezone = defaultCountry != null
                ? Timezone.find("country = ?1 and name = ?2", defaultCountry, "America/New_York").firstResult() : null;
        List<Country> countries = Country.list("order by name");
        List<Timezone> timezones = defaultCountry != null ? Timezone.list("country = ?1 order by name", defaultCountry)
                : List.of();
        return supportUserFormTemplate.data("user", newUser).data("companies", companies)
                .data("selectedCompanyId", selectedCompany == null ? null : selectedCompany.id)
                .data("companyLocked", selectedCompany != null && companies.size() <= 1)
                .data("types", List.of(User.TYPE_USER)).data("action", "/tam/users").data("title", "New user")
                .data("countries", countries).data("timezones", timezones)
                .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                .data("ticketsBase", "/user/tickets").data("showSupportUsers", true).data("currentUser", user);
    }

    @POST
    @Path("tam/users")
    @Transactional
    public Response createTamUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @FormParam("name") String name,
            @FormParam("fullName") String fullName, @FormParam("email") String email,
            @FormParam("social") String social, @FormParam("phoneNumber") String phoneNumber,
            @FormParam("phoneExtension") String phoneExtension, @FormParam("timezoneId") Long timezoneId,
            @FormParam("countryId") Long countryId, @FormParam("password") String password,
            @FormParam("type") String type, @FormParam("companyId") Long companyId) {
        User user = requireUser(auth);
        if (!User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/user")).build());
        }
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Username is required");
        }
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required");
        }
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Password is required");
        }
        if (companyId == null) {
            throw new BadRequestException("Company is required");
        }
        Company company = Company.findById(companyId);
        if (company == null) {
            throw new NotFoundException();
        }
        boolean allowed = Company.count("select count(c) from Company c join c.users u where c = ?1 and u = ?2",
                company, user) > 0;
        if (!allowed) {
            throw new BadRequestException("Company is required");
        }
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
        boolean exists = company.users.stream()
                .anyMatch(existing -> existing.id != null && existing.id.equals(newUser.id));
        if (!exists) {
            company.users.add(newUser);
        }
        return Response.seeOther(URI.create("/tam/users/" + company.id)).build();
    }

    @GET
    @Path("user/tickets")
    public TemplateInstance tickets(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireUser(auth);
        return userTickets(user);
    }

    @GET
    @Path("user/tickets/create")
    public TemplateInstance createTicketForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireUser(auth);
        SupportTicketData data = buildTicketDataForUser(user);
        java.util.List<Company> companies = Company
                .find("select distinct c from Company c join c.users u where u = ?1", user).list();
        Company company = companies.isEmpty() ? null : companies.get(0);
        java.util.List<CompanyEntitlement> entitlements = company == null ? java.util.List.of() : CompanyEntitlement
                .find("select distinct ce from CompanyEntitlement ce join fetch ce.entitlement join fetch ce.supportLevel where ce.company = ?1",
                        company)
                .list();
        java.util.List<CompanyEntitlement> uniqueEntitlements = new java.util.ArrayList<>();
        java.util.Set<Long> entitlementIds = new java.util.LinkedHashSet<>();
        for (CompanyEntitlement entitlement : entitlements) {
            if (entitlement == null || entitlement.entitlement == null || entitlement.entitlement.id == null) {
                continue;
            }
            if (entitlementIds.add(entitlement.entitlement.id)) {
                uniqueEntitlements.add(entitlement);
            }
        }
        java.util.Set<Long> expiredEntitlementIds = new java.util.LinkedHashSet<>();
        for (CompanyEntitlement entitlement : uniqueEntitlements) {
            if (isEntitlementExpired(entitlement) && entitlement.id != null) {
                expiredEntitlementIds.add(entitlement.id);
            }
        }
        java.util.List<Category> categories = Category.listAll();
        Category defaultCategory = Category.findDefault();
        return ticketCreateTemplate.data("companyEntitlements", uniqueEntitlements)
                .data("ticketName", company == null ? "" : Ticket.previewNextName(company))
                .data("expiredEntitlementIds", expiredEntitlementIds).data("assignedCount", data.assignedTickets.size())
                .data("openCount", data.openTickets.size()).data("ticketsBase", "/user/tickets")
                .data("showSupportUsers", User.TYPE_TAM.equalsIgnoreCase(user.type))
                .data("usersBase", User.TYPE_TAM.equalsIgnoreCase(user.type) ? "/tam/users" : "/user/users")
                .data("currentUser", user).data("categories", categories)
                .data("defaultCategoryId", defaultCategory == null ? null : defaultCategory.id);
    }

    @POST
    @Path("user/tickets")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response createUserTicket(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, MultipartFormDataInput input) {
        User user = requireUser(auth);
        String status = AttachmentHelper.readFormValue(input, "status");
        String messageBody = AttachmentHelper.readFormValue(input, "message");
        Long companyEntitlementId = AttachmentHelper.readFormLong(input, "companyEntitlementId");
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
        boolean allowed = Company.count("select count(c) from Company c join c.users u where c = ?1 and u = ?2",
                entitlement.company, user) > 0;
        if (!allowed) {
            throw new BadRequestException("Entitlement is required");
        }
        if (isEntitlementExpired(entitlement)) {
            throw new BadRequestException("Entitlement is expired");
        }
        Ticket ticket = new Ticket();
        ticket.name = Ticket.nextName(entitlement.company);
        ticket.status = "Open";
        ticket.company = entitlement.company;
        ticket.requester = user;
        ticket.companyEntitlement = entitlement;
        ticket.category = categoryId != null ? Category.findById(categoryId) : Category.findDefault();
        ticket.persist();
        Message message = new Message();
        message.body = messageBody.trim();
        message.date = java.time.LocalDateTime.now();
        message.ticket = ticket;
        message.author = user;
        AttachmentHelper.attachToMessage(message, AttachmentHelper.readAttachments(input, "attachments"));
        message.persist();
        ticketEmailService.notifyMessageChange(ticket, message, user);
        return Response.seeOther(URI.create("/user/tickets")).build();
    }

    @GET
    @Path("user/support-users/{id}")
    public TemplateInstance viewSupportUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        User user = requireUser(auth);
        User supportUser = User.findById(id);
        if (supportUser == null || !User.TYPE_SUPPORT.equalsIgnoreCase(supportUser.type)) {
            throw new NotFoundException();
        }
        SupportTicketData data = buildTicketDataForUser(user);
        return supportUserViewTemplate.data("supportUser", supportUser)
                .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                .data("ticketsBase", "/user/tickets")
                .data("showSupportUsers", User.TYPE_TAM.equalsIgnoreCase(user.type))
                .data("usersBase", User.TYPE_TAM.equalsIgnoreCase(user.type) ? "/tam/users" : "/user/users")
                .data("currentUser", user);
    }

    @GET
    @Path("user/tam-users/{id}")
    public TemplateInstance viewTamUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireUser(auth);
        User tamUser = User.findById(id);
        if (tamUser == null || !User.TYPE_TAM.equalsIgnoreCase(tamUser.type)) {
            throw new NotFoundException();
        }
        SupportTicketData data = buildTicketDataForUser(user);
        return tamUserViewTemplate.data("tamUser", tamUser).data("assignedCount", data.assignedTickets.size())
                .data("openCount", data.openTickets.size()).data("ticketsBase", "/user/tickets")
                .data("showSupportUsers", User.TYPE_TAM.equalsIgnoreCase(user.type))
                .data("usersBase", User.TYPE_TAM.equalsIgnoreCase(user.type) ? "/tam/users" : "/user/users")
                .data("currentUser", user);
    }

    @GET
    @Path("user/user-profiles/{id}")
    public TemplateInstance viewUserProfile(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        User user = requireUser(auth);
        User viewedUser = User.findById(id);
        if (viewedUser == null) {
            throw new NotFoundException();
        }
        SupportTicketData data = buildTicketDataForUser(user);
        return userProfileViewTemplate.data("viewedUser", viewedUser).data("assignedCount", data.assignedTickets.size())
                .data("openCount", data.openTickets.size()).data("ticketsBase", "/user/tickets")
                .data("showSupportUsers", User.TYPE_TAM.equalsIgnoreCase(user.type))
                .data("usersBase", User.TYPE_TAM.equalsIgnoreCase(user.type) ? "/tam/users" : "/user/users")
                .data("currentUser", user);
    }

    @GET
    @Path("user/tickets/open")
    public Response tamOpenTickets(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireUser(auth);
        SupportTicketData data = buildTicketDataForUser(user);
        return Response.ok(ticketsTemplate.data("tickets", data.openTickets).data("pageTitle", "Open tickets")
                .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                .data("messageDates", data.messageDates).data("messageDateLabels", data.messageDateLabels)
                .data("messageDirectionArrows", data.messageDirectionArrows).data("slaColors", data.slaColors)
                .data("supportAssignments", data.supportAssignments)
                .data("supportAssignmentNames", data.supportAssignmentNames)
                .data("supportAssignmentIds", data.supportAssignmentIds).data("createTicketUrl", "/user/tickets/create")
                .data("ticketsBase", "/user/tickets")
                .data("showSupportUsers", User.TYPE_TAM.equalsIgnoreCase(user.type))
                .data("usersBase", User.TYPE_TAM.equalsIgnoreCase(user.type) ? "/tam/users" : "/user/users")
                .data("currentUser", user)).build();
    }

    @GET
    @Path("user/tickets/closed")
    public Response tamClosedTickets(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireUser(auth);
        SupportTicketData data = buildTicketDataForUser(user);
        return Response.ok(ticketsTemplate.data("tickets", data.closedTickets).data("pageTitle", "Closed tickets")
                .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                .data("messageDates", data.messageDates).data("messageDateLabels", data.messageDateLabels)
                .data("messageDirectionArrows", data.messageDirectionArrows).data("slaColors", data.slaColors)
                .data("supportAssignments", data.supportAssignments)
                .data("supportAssignmentNames", data.supportAssignmentNames)
                .data("supportAssignmentIds", data.supportAssignmentIds).data("createTicketUrl", "/user/tickets/create")
                .data("ticketsBase", "/user/tickets")
                .data("showSupportUsers", User.TYPE_TAM.equalsIgnoreCase(user.type))
                .data("usersBase", User.TYPE_TAM.equalsIgnoreCase(user.type) ? "/tam/users" : "/user/users")
                .data("currentUser", user)).build();
    }

    @GET
    @Path("user/tickets/{id}")
    public TemplateInstance ticketDetail(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireUser(auth);
        return renderUserTicketDetail(user, id);
    }

    private TemplateInstance renderUserTicketDetail(User user, Long id) {
        Ticket ticket = findTicketForUser(user, id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        java.util.List<ai.mnemosyne_systems.model.Message> messages = loadMessages(ticket);
        java.util.Map<Long, String> messageLabels = new java.util.LinkedHashMap<>();
        java.util.Map<Long, String> messageAuthorNames = new java.util.LinkedHashMap<>();
        java.util.Map<Long, String> messageAuthorLinks = new java.util.LinkedHashMap<>();
        for (ai.mnemosyne_systems.model.Message message : messages) {
            if (message.date != null) {
                messageLabels.put(message.id, formatDate(message.date));
            }
            if (message.author != null && message.author.id != null) {
                messageAuthorNames.put(message.id, message.author.name);
                if (User.TYPE_SUPPORT.equalsIgnoreCase(message.author.type)) {
                    messageAuthorLinks.put(message.id, "/user/support-users/" + message.author.id);
                } else if (User.TYPE_TAM.equalsIgnoreCase(message.author.type)) {
                    messageAuthorLinks.put(message.id, "/user/tam-users/" + message.author.id);
                } else {
                    messageAuthorLinks.put(message.id, "/user/user-profiles/" + message.author.id);
                }
            }
        }
        SupportTicketData data = buildTicketDataForUser(user);
        java.util.List<User> supportUsers = User
                .find("select u from Ticket t join t.supportUsers u where t = ?1 order by u.email", ticket).list();
        String displayStatus = ticket.status;
        if (displayStatus == null || displayStatus.isBlank()) {
            displayStatus = "Open";
        }
        if ("Open".equalsIgnoreCase(displayStatus) && !supportUsers.isEmpty()) {
            displayStatus = "Assigned";
        }
        java.util.List<User> tamUsers = ticket.company == null ? new java.util.ArrayList<>() : User.find(
                "select distinct u from Company c join c.users u where c = ?1 and lower(u.type) = ?2 order by u.email",
                ticket.company, User.TYPE_TAM).list();
        java.util.List<User> ticketTams = User
                .find("select u from Ticket t join t.tamUsers u where t = ?1 order by u.email", ticket).list();
        if (!ticketTams.isEmpty()) {
            java.util.Set<Long> seenIds = new java.util.HashSet<>();
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
        boolean showLevel = User.TYPE_TAM.equalsIgnoreCase(user.type);
        String levelName = showLevel ? resolveLowestEntitlementLevelName(ticket) : null;
        java.util.List<Category> categories = Category.listAll();
        return tamTicketDetailTemplate.data("ticket", ticket).data("displayStatus", displayStatus)
                .data("supportUsers", supportUsers).data("tamUsers", tamUsers).data("messages", messages)
                .data("messageLabels", messageLabels).data("messageAuthorNames", messageAuthorNames)
                .data("messageAuthorLinks", messageAuthorLinks).data("action", "/user/tickets/" + id)
                .data("editableStatus", false).data("supportUserBase", "/user/support-users")
                .data("ticketEntitlementExpired", isEntitlementExpired(ticket)).data("tamUserBase", "/user/tam-users")
                .data("showLevel", showLevel).data("levelName", levelName)
                .data("messageAction", "/user/tickets/" + id + "/messages")
                .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                .data("ticketsBase", "/user/tickets")
                .data("showSupportUsers", User.TYPE_TAM.equalsIgnoreCase(user.type))
                .data("usersBase", User.TYPE_TAM.equalsIgnoreCase(user.type) ? "/tam/users" : "/user/users")
                .data("currentUser", user).data("categories", categories);
    }

    private List<ai.mnemosyne_systems.model.Message> loadMessages(Ticket ticket) {
        List<ai.mnemosyne_systems.model.Message> messages = ai.mnemosyne_systems.model.Message.find(
                "select distinct m from Message m left join fetch m.attachments where m.ticket = ?1 order by m.date desc",
                ticket).list();
        if (messages.isEmpty()) {
            return messages;
        }
        return new ArrayList<>(new LinkedHashSet<>(messages));
    }

    @POST
    @Path("user/tickets/{id}/messages")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response addUserMessage(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            MultipartFormDataInput input) {
        User user = requireUser(auth);
        String body = AttachmentHelper.readFormValue(input, "body");
        if (body == null || body.isBlank()) {
            throw new BadRequestException("Message is required");
        }
        Ticket ticket = findTicketForUser(user, id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        ai.mnemosyne_systems.model.Message message = new ai.mnemosyne_systems.model.Message();
        message.body = body;
        message.date = java.time.LocalDateTime.now();
        message.ticket = ticket;
        message.author = user;
        AttachmentHelper.attachToMessage(message, AttachmentHelper.readAttachments(input, "attachments"));
        message.persist();
        ticketEmailService.notifyMessageChange(ticket, message, user);
        return Response.seeOther(URI.create("/tickets/" + id)).build();
    }

    @GET
    @Path("user/tickets/{id}/edit")
    public TemplateInstance ticketEdit(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireUser(auth);
        Ticket ticket = findTicketForUser(user, id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        return ticketEditTemplate.data("ticket", ticket).data("currentUser", user);
    }

    @POST
    @Path("user/tickets/{id}")
    @Transactional
    public Response updateUserTicket(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            @FormParam("status") String status) {
        User user = requireUser(auth);
        Ticket ticket = findTicketForUser(user, id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        String normalized = status == null ? "" : status.trim();
        if (!(normalized.equals("Assigned") || normalized.equals("In Progress") || normalized.equals("Resolved")
                || normalized.equals("Closed"))) {
            throw new BadRequestException("Status must be Assigned, In Progress, Resolved, or Closed");
        }
        String previousStatus = ticketEmailService.computeEffectiveStatus(ticket, ticket.status);
        ticket.status = normalized;
        if (!sameStatus(previousStatus, ticket.status)) {
            ticketEmailService.notifyStatusChange(ticket, previousStatus, user);
        }
        return Response.seeOther(URI.create("/tickets/" + id)).build();
    }

    @GET
    @Path("users")
    public TemplateInstance listAdminUsers(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("companyId") Long companyId) {
        User user = requireAdmin(auth);
        List<Company> companies = Company.list("order by name");
        Company selectedCompany = null;
        if (companyId != null) {
            selectedCompany = Company.findById(companyId);
            if (selectedCompany == null) {
                throw new NotFoundException();
            }
        }
        if (selectedCompany == null && !companies.isEmpty()) {
            selectedCompany = companies.get(0);
        }
        List<User> users = selectedCompany == null ? List.of() : Company
                .find("select u from Company c join c.users u where c = ?1 order by u.name", selectedCompany).list();
        return adminUsersTemplate.data("users", users).data("companies", companies)
                .data("selectedCompanyId", selectedCompany == null ? null : selectedCompany.id)
                .data("showCompanySelector", true).data("companyLocked", false).data("createUserUrl", "/users/create")
                .data("currentUser", user);
    }

    @GET
    @Path("users/create")
    public TemplateInstance createAdminUserForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        User newUser = new User();
        newUser.type = User.TYPE_USER;
        newUser.name = "";
        newUser.email = "";
        Country defaultCountry = Country.find("code", "US").firstResult();
        newUser.country = defaultCountry;
        newUser.timezone = defaultCountry != null
                ? Timezone.find("country = ?1 and name = ?2", defaultCountry, "America/New_York").firstResult() : null;
        List<Country> countries = Country.list("order by name");
        List<Timezone> timezones = defaultCountry != null ? Timezone.list("country = ?1 order by name", defaultCountry)
                : List.of();
        List<Company> allCompanies = Company.list("order by name");
        return adminUserFormTemplate.data("user", newUser).data("action", "/users").data("title", "New user")
                .data("countries", countries).data("timezones", timezones).data("allCompanies", allCompanies)
                .data("userCompany", null).data("currentUser", user);
    }

    @GET
    @Path("users/{id}/edit")
    public TemplateInstance editAdminUserForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        User user = requireAdmin(auth);
        User editUser = User.findById(id);
        if (editUser == null) {
            throw new NotFoundException();
        }
        List<Country> countries = Country.list("order by name");
        Country timezoneCountry = editUser.country != null ? editUser.country
                : Country.find("code", "US").firstResult();
        List<Timezone> timezones = timezoneCountry != null
                ? Timezone.list("country = ?1 order by name", timezoneCountry) : List.of();
        List<Company> allCompanies = Company.list("order by name");
        Company userCompany = Company.find("select c from Company c join c.users u where u = ?1", editUser)
                .firstResult();
        return adminUserFormTemplate.data("user", editUser).data("action", "/users/" + id).data("title", "Edit User")
                .data("countries", countries).data("timezones", timezones).data("allCompanies", allCompanies)
                .data("userCompany", userCompany).data("currentUser", user);
    }

    @GET
    @Path("users/{id}")
    public TemplateInstance viewAdminUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireAdmin(auth);
        User viewUser = User.findById(id);
        if (viewUser == null) {
            throw new NotFoundException();
        }
        return adminUserViewTemplate.data("user", viewUser).data("typeLabel", typeLabel(viewUser.type))
                .data("currentUser", user);
    }

    @POST
    @Path("users")
    @Transactional
    public Response createAdminUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @FormParam("name") String name,
            @FormParam("fullName") String fullName, @FormParam("email") String email,
            @FormParam("social") String social, @FormParam("phoneNumber") String phoneNumber,
            @FormParam("phoneExtension") String phoneExtension, @FormParam("timezoneId") Long timezoneId,
            @FormParam("countryId") Long countryId, @FormParam("type") String type,
            @FormParam("password") String password, @FormParam("companyId") Long companyId) {
        requireAdmin(auth);
        validateUserFields(name, email, type, true, password);
        User newUser = new User();
        newUser.name = name.trim();
        newUser.fullName = trimOrNull(fullName);
        newUser.email = email.trim();
        newUser.social = trimOrNull(social);
        newUser.phoneNumber = trimOrNull(phoneNumber);
        newUser.phoneExtension = trimOrNull(phoneExtension);
        newUser.timezone = timezoneId != null ? Timezone.findById(timezoneId) : null;
        newUser.country = countryId != null ? Country.findById(countryId) : null;
        newUser.type = normalizeType(type, Set.of(User.TYPE_ADMIN, User.TYPE_SUPPORT, User.TYPE_USER, User.TYPE_TAM),
                "Type must be admin, support, user, or tam");
        newUser.passwordHash = BcryptUtil.bcryptHash(password);
        newUser.persist();
        if (companyId != null) {
            Company company = Company.findById(companyId);
            if (company != null) {
                company.users.add(newUser);
            }
        }
        return Response.seeOther(URI.create("/users")).build();
    }

    @POST
    @Path("users/{id}")
    @Transactional
    public Response updateAdminUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            @FormParam("name") String name, @FormParam("fullName") String fullName, @FormParam("email") String email,
            @FormParam("social") String social, @FormParam("phoneNumber") String phoneNumber,
            @FormParam("phoneExtension") String phoneExtension, @FormParam("timezoneId") Long timezoneId,
            @FormParam("countryId") Long countryId, @FormParam("type") String type,
            @FormParam("password") String password, @FormParam("companyId") Long companyId) {
        requireAdmin(auth);
        User editUser = User.findById(id);
        if (editUser == null) {
            throw new NotFoundException();
        }
        validateUserFields(name, email, type, false, password);
        editUser.name = name.trim();
        editUser.fullName = trimOrNull(fullName);
        editUser.email = email.trim();
        editUser.social = trimOrNull(social);
        editUser.phoneNumber = trimOrNull(phoneNumber);
        editUser.phoneExtension = trimOrNull(phoneExtension);
        editUser.timezone = timezoneId != null ? Timezone.findById(timezoneId) : null;
        editUser.country = countryId != null ? Country.findById(countryId) : null;
        editUser.type = normalizeType(type, Set.of(User.TYPE_ADMIN, User.TYPE_SUPPORT, User.TYPE_USER, User.TYPE_TAM),
                "Type must be admin, support, user, or tam");
        if (password != null && !password.isBlank()) {
            editUser.passwordHash = BcryptUtil.bcryptHash(password);
        }
        List<Company> currentCompanies = Company.find("select c from Company c join c.users u where u = ?1", editUser)
                .list();
        for (Company c : currentCompanies) {
            c.users.removeIf(u -> u.id != null && u.id.equals(editUser.id));
        }
        if (companyId != null) {
            Company company = Company.findById(companyId);
            if (company != null) {
                company.users.add(editUser);
            }
        }
        return Response.seeOther(URI.create("/users")).build();
    }

    private String trimOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @POST
    @Path("users/{id}/delete")
    @Transactional
    public Response deleteAdminUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireAdmin(auth);
        User deleteUser = User.findById(id);
        if (deleteUser == null) {
            throw new NotFoundException();
        }
        deleteUser.delete();
        return Response.seeOther(URI.create("/users")).build();
    }

    private void validateUserFields(String name, String email, String type, boolean requirePassword, String password) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Username is required");
        }
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required");
        }
        if (requirePassword && (password == null || password.isBlank())) {
            throw new BadRequestException("Password is required");
        }
        if (type == null || type.isBlank()) {
            throw new BadRequestException("Type is required");
        }
    }

    private TemplateInstance userTickets(User user) {
        SupportTicketData data = buildTicketDataForUser(user);
        return ticketsTemplate.data("tickets", data.assignedTickets).data("pageTitle", "Tickets")
                .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                .data("messageDates", data.messageDates).data("messageDateLabels", data.messageDateLabels)
                .data("messageDirectionArrows", data.messageDirectionArrows).data("slaColors", data.slaColors)
                .data("supportAssignments", data.supportAssignments)
                .data("supportAssignmentNames", data.supportAssignmentNames)
                .data("supportAssignmentIds", data.supportAssignmentIds).data("createTicketUrl", "/user/tickets/create")
                .data("ticketsBase", "/user/tickets")
                .data("showSupportUsers", User.TYPE_TAM.equalsIgnoreCase(user.type))
                .data("usersBase", User.TYPE_TAM.equalsIgnoreCase(user.type) ? "/tam/users" : "/user/users")
                .data("currentUser", user);
    }

    private SupportTicketData buildTamTicketData(User user) {
        java.util.List<Ticket> tickets = Ticket.list(
                "select distinct t from Ticket t left join t.tamUsers tu left join t.company c left join c.users cu where tu = ?1 or cu = ?1",
                user);
        return buildTicketDataFor(tickets);
    }

    private SupportTicketData buildUserTicketData(User user) {
        java.util.List<Ticket> tickets = Ticket.list("requester = ?1", user);
        return buildTicketDataFor(tickets);
    }

    private SupportTicketData buildTicketDataForUser(User user) {
        if (User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            return buildTamTicketData(user);
        }
        return buildUserTicketData(user);
    }

    private SupportTicketData buildTicketDataFor(java.util.List<Ticket> tickets) {
        java.util.List<Ticket> scopedTickets = tickets == null ? java.util.List.of() : tickets;
        java.util.Map<Long, java.time.LocalDateTime> messageDates = new java.util.LinkedHashMap<>();
        java.util.Map<Long, String> messageDateLabels = new java.util.LinkedHashMap<>();
        java.util.Map<Long, String> messageDirectionArrows = new java.util.LinkedHashMap<>();
        java.util.List<ai.mnemosyne_systems.model.Message> messages = ai.mnemosyne_systems.model.Message
                .find("order by date desc").list();
        for (ai.mnemosyne_systems.model.Message message : messages) {
            if (message.ticket != null && scopedTickets.contains(message.ticket)
                    && !messageDates.containsKey(message.ticket.id)) {
                messageDates.put(message.ticket.id, message.date);
                if (message.date != null) {
                    messageDateLabels.put(message.ticket.id, formatDate(message.date));
                }
                messageDirectionArrows.put(message.ticket.id, messageDirectionArrow(message.author));
            }
        }
        for (Ticket ticket : scopedTickets) {
            if (!messageDateLabels.containsKey(ticket.id)) {
                messageDateLabels.put(ticket.id, "-");
            }
        }
        java.util.Map<Long, String> slaColors = new java.util.LinkedHashMap<>();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        for (Ticket ticket : scopedTickets) {
            if (isEntitlementExpired(ticket)) {
                slaColors.put(ticket.id, "Black");
                continue;
            }
            java.time.LocalDateTime messageDate = messageDates.get(ticket.id);
            if (messageDate == null || ticket.companyEntitlement == null
                    || ticket.companyEntitlement.supportLevel == null) {
                continue;
            }
            long minutes = java.time.Duration.between(messageDate, now).toMinutes();
            if (minutes < 0) {
                minutes = 0;
            }
            String color = resolveSlaColor(ticket.companyEntitlement.supportLevel, minutes);
            if (color != null && !color.isBlank()) {
                slaColors.put(ticket.id, color);
            }
        }
        java.util.Map<Long, String> supportAssignments = new java.util.LinkedHashMap<>();
        java.util.Map<Long, String> supportAssignmentNames = new java.util.LinkedHashMap<>();
        java.util.Map<Long, Long> supportAssignmentIds = new java.util.LinkedHashMap<>();
        for (Ticket ticket : scopedTickets) {
            User assignedSupport = User
                    .find("select u from Ticket t join t.supportUsers u where t = ?1 order by u.id desc", ticket)
                    .firstResult();
            if (assignedSupport != null) {
                supportAssignments.put(ticket.id, assignedSupport.email);
                supportAssignmentNames.put(ticket.id, assignedSupport.name);
                supportAssignmentIds.put(ticket.id, assignedSupport.id);
            }
        }
        java.util.List<Ticket> assignedTickets = new java.util.ArrayList<>();
        java.util.List<Ticket> openTickets = new java.util.ArrayList<>();
        java.util.List<Ticket> closedTickets = new java.util.ArrayList<>();
        for (Ticket ticket : scopedTickets) {
            boolean hasSupport = supportAssignments.containsKey(ticket.id);
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
            if (ticket != null && ticket.id != null) {
                if (!isEntitlementExpired(ticket)) {
                    slaColors.put(ticket.id, "White");
                }
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

    private void sortBySla(List<Ticket> tickets, Map<Long, String> slaColors,
            Map<Long, java.time.LocalDateTime> messageDates) {
        tickets.sort((left, right) -> {
            int leftRank = slaColorRank(slaColors.get(left.id));
            int rightRank = slaColorRank(slaColors.get(right.id));
            if (leftRank != rightRank) {
                return Integer.compare(leftRank, rightRank);
            }
            java.time.LocalDateTime leftDate = messageDates.get(left.id);
            java.time.LocalDateTime rightDate = messageDates.get(right.id);
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
        String normalized = color.trim().toLowerCase(java.util.Locale.ENGLISH);
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
        displayTicket.category = ticket.category;
        displayTicket.externalIssueLink = ticket.externalIssueLink;
        return displayTicket;
    }

    private static class SupportTicketData {
        private List<Ticket> assignedTickets;
        private List<Ticket> openTickets;
        private List<Ticket> closedTickets;
        private Map<Long, java.time.LocalDateTime> messageDates;
        private Map<Long, String> messageDateLabels;
        private Map<Long, String> messageDirectionArrows;
        private Map<Long, String> slaColors;
        private Map<Long, String> supportAssignments;
        private Map<Long, String> supportAssignmentNames;
        private Map<Long, Long> supportAssignmentIds;
    }

    private Ticket findTicketForUser(User user, Long id) {
        if (User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            return Ticket
                    .find("select distinct t from Ticket t join t.company c join c.users u where u = ?1 and t.id = ?2",
                            user, id)
                    .firstResult();
        }
        return Ticket.find("requester = ?1 and id = ?2", user, id).firstResult();
    }

    private String formatDate(java.time.LocalDateTime date) {
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                .ofPattern("MMMM d yyyy, h.mma", java.util.Locale.ENGLISH);
        String formatted = formatter.format(date);
        return formatted.replace("AM", "am").replace("PM", "pm");
    }

    private boolean sameStatus(String left, String right) {
        String normalizedLeft = left == null ? "" : left.trim();
        String normalizedRight = right == null ? "" : right.trim();
        return normalizedLeft.equalsIgnoreCase(normalizedRight);
    }

    private String normalizeType(String type, Set<String> allowedTypes, String errorMessage) {
        String normalized = type == null ? "" : type.trim().toLowerCase();
        if (!allowedTypes.contains(normalized)) {
            throw new BadRequestException(errorMessage);
        }
        return normalized;
    }

    private String typeLabel(String type) {
        if (type == null) {
            return "User";
        }
        return switch (type.toLowerCase()) {
        case User.TYPE_ADMIN -> "Admin";
        case User.TYPE_SUPPORT -> "Support";
        case User.TYPE_TAM -> "TAM";
        default -> "User";
        };
    }

    private User requireUser(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isUser(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }

    private User requireAdmin(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isAdmin(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }
}
