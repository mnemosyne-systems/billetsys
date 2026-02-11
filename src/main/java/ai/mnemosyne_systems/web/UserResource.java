/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

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

    @Location("user/tam-ticket-detail.html")
    Template tamTicketDetailTemplate;

    @Location("user/ticket-edit.html")
    Template ticketEditTemplate;

    @Location("admin/users.html")
    Template adminUsersTemplate;

    @Location("admin/user-form.html")
    Template adminUserFormTemplate;

    @Location("admin/user-view.html")
    Template adminUserViewTemplate;

    @Location("support/users.html")
    Template supportUsersTemplate;

    @Location("support/user-form.html")
    Template supportUserFormTemplate;

    @GET
    @Path("admin")
    public Response adminRoot(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireAdmin(auth);
        return Response.seeOther(URI.create("/admin/companies")).build();
    }

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
        return Response.ok(supportUsersTemplate.data("users", users).data("companies", companies)
                .data("selectedCompanyId", selectedCompany == null ? null : selectedCompany.id)
                .data("selectedCompany", selectedCompany).data("showCompanySelector", false)
                .data("createUserUrl", createUserUrl).data("usersBase", "/tam/users")
                .data("companyLocked", companies.size() <= 1).data("assignedCount", data.assignedTickets.size())
                .data("openCount", data.openTickets.size()).data("ticketsBase", "/user/tickets")
                .data("showSupportUsers", true).data("currentUser", user)).build();
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
        List<Country> countries = Country.list("order by name");
        return supportUserFormTemplate.data("user", newUser).data("companies", companies)
                .data("selectedCompanyId", selectedCompany == null ? null : selectedCompany.id)
                .data("companyLocked", selectedCompany != null && companies.size() <= 1)
                .data("types", List.of(User.TYPE_USER)).data("action", "/tam/users").data("title", "New user")
                .data("countries", countries).data("timezones", List.of())
                .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                .data("ticketsBase", "/user/tickets").data("showSupportUsers", true).data("currentUser", user);
    }

    @POST
    @Path("tam/users")
    @Transactional
    public Response createTamUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @FormParam("name") String name,
            @FormParam("fullName") String fullName, @FormParam("email") String email,
            @FormParam("phoneNumber") String phoneNumber, @FormParam("phoneExtension") String phoneExtension,
            @FormParam("timezoneId") Long timezoneId, @FormParam("countryId") Long countryId,
            @FormParam("password") String password, @FormParam("type") String type,
            @FormParam("companyId") Long companyId) {
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
        return ticketCreateTemplate.data("companyEntitlements", entitlements)
                .data("ticketName", company == null ? "" : Ticket.previewNextName(company))
                .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                .data("ticketsBase", "/user/tickets")
                .data("showSupportUsers", User.TYPE_TAM.equalsIgnoreCase(user.type))
                .data("usersBase", User.TYPE_TAM.equalsIgnoreCase(user.type) ? "/tam/users" : "/user/users")
                .data("currentUser", user);
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
        Ticket ticket = new Ticket();
        ticket.name = Ticket.nextName(entitlement.company);
        ticket.status = "Open";
        ticket.company = entitlement.company;
        ticket.requester = user;
        ticket.companyEntitlement = entitlement;
        ticket.persist();
        Message message = new Message();
        message.body = messageBody.trim();
        message.date = java.time.LocalDateTime.now();
        message.ticket = ticket;
        message.author = user;
        AttachmentHelper.attachToMessage(message, AttachmentHelper.readAttachments(input, "attachments"));
        message.persist();
        return Response.seeOther(URI.create("/user/tickets")).build();
    }

    @GET
    @Path("user/tickets/open")
    public Response tamOpenTickets(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireUser(auth);
        SupportTicketData data = buildTicketDataForUser(user);
        return Response.ok(ticketsTemplate.data("tickets", data.openTickets).data("pageTitle", "Open tickets")
                .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                .data("messageDates", data.messageDates).data("messageDateLabels", data.messageDateLabels)
                .data("slaColors", data.slaColors).data("supportAssignments", data.supportAssignments)
                .data("createTicketUrl", "/user/tickets/create").data("ticketsBase", "/user/tickets")
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
                .data("slaColors", data.slaColors).data("supportAssignments", data.supportAssignments)
                .data("createTicketUrl", "/user/tickets/create").data("ticketsBase", "/user/tickets")
                .data("showSupportUsers", User.TYPE_TAM.equalsIgnoreCase(user.type))
                .data("usersBase", User.TYPE_TAM.equalsIgnoreCase(user.type) ? "/tam/users" : "/user/users")
                .data("currentUser", user)).build();
    }

    @GET
    @Path("user/tickets/{id}")
    public TemplateInstance ticketDetail(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireUser(auth);
        Ticket ticket = findTicketForUser(user, id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        java.util.List<ai.mnemosyne_systems.model.Message> messages = loadMessages(ticket);
        java.util.Map<Long, String> messageLabels = new java.util.LinkedHashMap<>();
        for (ai.mnemosyne_systems.model.Message message : messages) {
            if (message.date != null) {
                messageLabels.put(message.id, formatDate(message.date));
            }
        }
        SupportTicketData data = buildTicketDataForUser(user);
        java.util.List<User> supportUsers = User
                .find("select u from Ticket t join t.supportUsers u where t = ?1 order by u.email", ticket).list();
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
        return tamTicketDetailTemplate.data("ticket", ticket).data("supportUsers", supportUsers)
                .data("tamUsers", tamUsers).data("messages", messages).data("messageLabels", messageLabels)
                .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                .data("ticketsBase", "/user/tickets")
                .data("showSupportUsers", User.TYPE_TAM.equalsIgnoreCase(user.type))
                .data("usersBase", User.TYPE_TAM.equalsIgnoreCase(user.type) ? "/tam/users" : "/user/users")
                .data("currentUser", user);
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
        return Response.seeOther(URI.create("/user/tickets/" + id)).build();
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
        ticket.status = normalized;
        return Response.seeOther(URI.create("/user/tickets/" + id)).build();
    }

    @GET
    @Path("admin/users")
    public TemplateInstance listAdminUsers(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        List<User> users = User.listAll();
        Map<Long, String> typeLabels = new HashMap<>();
        for (User entry : users) {
            typeLabels.put(entry.id, typeLabel(entry.type));
        }
        return adminUsersTemplate.data("users", users).data("typeLabels", typeLabels).data("currentUser", user);
    }

    @GET
    @Path("admin/users/create")
    public TemplateInstance createAdminUserForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        User newUser = new User();
        newUser.type = User.TYPE_USER;
        newUser.name = "";
        newUser.email = "";
        List<Country> countries = Country.list("order by name");
        List<Company> allCompanies = Company.list("order by name");
        return adminUserFormTemplate.data("user", newUser).data("action", "/admin/users").data("title", "New user")
                .data("countries", countries).data("timezones", List.of()).data("allCompanies", allCompanies)
                .data("userCompany", null).data("currentUser", user);
    }

    @GET
    @Path("admin/users/{id}/edit")
    public TemplateInstance editAdminUserForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        User user = requireAdmin(auth);
        User editUser = User.findById(id);
        if (editUser == null) {
            throw new NotFoundException();
        }
        List<Country> countries = Country.list("order by name");
        List<Timezone> timezones = editUser.country != null
                ? Timezone.list("country = ?1 order by name", editUser.country) : List.of();
        List<Company> allCompanies = Company.list("order by name");
        Company userCompany = Company.find("select c from Company c join c.users u where u = ?1", editUser)
                .firstResult();
        return adminUserFormTemplate.data("user", editUser).data("action", "/admin/users/" + id)
                .data("title", "Edit User").data("countries", countries).data("timezones", timezones)
                .data("allCompanies", allCompanies).data("userCompany", userCompany).data("currentUser", user);
    }

    @GET
    @Path("admin/users/{id}")
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
    @Path("admin/users")
    @Transactional
    public Response createAdminUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @FormParam("name") String name,
            @FormParam("fullName") String fullName, @FormParam("email") String email,
            @FormParam("phoneNumber") String phoneNumber, @FormParam("phoneExtension") String phoneExtension,
            @FormParam("timezoneId") Long timezoneId, @FormParam("countryId") Long countryId,
            @FormParam("type") String type, @FormParam("password") String password,
            @FormParam("companyId") Long companyId) {
        requireAdmin(auth);
        validateUserFields(name, email, type, true, password);
        User newUser = new User();
        newUser.name = name.trim();
        newUser.fullName = trimOrNull(fullName);
        newUser.email = email.trim();
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
        return Response.seeOther(URI.create("/admin/users")).build();
    }

    @POST
    @Path("admin/users/{id}")
    @Transactional
    public Response updateAdminUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            @FormParam("name") String name, @FormParam("fullName") String fullName, @FormParam("email") String email,
            @FormParam("phoneNumber") String phoneNumber, @FormParam("phoneExtension") String phoneExtension,
            @FormParam("timezoneId") Long timezoneId, @FormParam("countryId") Long countryId,
            @FormParam("type") String type, @FormParam("password") String password,
            @FormParam("companyId") Long companyId) {
        requireAdmin(auth);
        User editUser = User.findById(id);
        if (editUser == null) {
            throw new NotFoundException();
        }
        validateUserFields(name, email, type, false, password);
        editUser.name = name.trim();
        editUser.fullName = trimOrNull(fullName);
        editUser.email = email.trim();
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
        return Response.seeOther(URI.create("/admin/users")).build();
    }

    private String trimOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @POST
    @Path("admin/users/{id}/delete")
    @Transactional
    public Response deleteAdminUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireAdmin(auth);
        User deleteUser = User.findById(id);
        if (deleteUser == null) {
            throw new NotFoundException();
        }
        deleteUser.delete();
        return Response.seeOther(URI.create("/admin/users")).build();
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
                .data("slaColors", data.slaColors).data("supportAssignments", data.supportAssignments)
                .data("createTicketUrl", "/user/tickets/create").data("ticketsBase", "/user/tickets")
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
        java.util.List<ai.mnemosyne_systems.model.Message> messages = ai.mnemosyne_systems.model.Message
                .find("order by date desc").list();
        for (ai.mnemosyne_systems.model.Message message : messages) {
            if (message.ticket != null && scopedTickets.contains(message.ticket)
                    && !messageDates.containsKey(message.ticket.id)) {
                messageDates.put(message.ticket.id, message.date);
                if (message.date != null) {
                    messageDateLabels.put(message.ticket.id, formatDate(message.date));
                }
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
        for (Ticket ticket : scopedTickets) {
            User assignedSupport = User
                    .find("select u from Ticket t join t.supportUsers u where t = ?1 order by u.id desc", ticket)
                    .firstResult();
            if (assignedSupport != null) {
                supportAssignments.put(ticket.id, assignedSupport.email);
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
        data.slaColors = slaColors;
        data.supportAssignments = supportAssignments;
        return data;
    }

    private String resolveSlaColor(ai.mnemosyne_systems.model.SupportLevel level, long minutes) {
        if (level.normal != null && minutes >= level.normal) {
            return level.normalColor;
        }
        if (level.escalate != null && minutes >= level.escalate) {
            return level.escalateColor;
        }
        if (level.critical != null && minutes >= level.critical) {
            return level.criticalColor;
        }
        return null;
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
        return displayTicket;
    }

    private static class SupportTicketData {
        private List<Ticket> assignedTickets;
        private List<Ticket> openTickets;
        private List<Ticket> closedTickets;
        private Map<Long, java.time.LocalDateTime> messageDates;
        private Map<Long, String> messageDateLabels;
        private Map<Long, String> slaColors;
        private Map<Long, String> supportAssignments;
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
