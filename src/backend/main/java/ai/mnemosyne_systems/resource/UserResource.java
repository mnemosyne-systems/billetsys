/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.Attachment;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.Version;
import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.service.CrossReferenceService;
import ai.mnemosyne_systems.service.TicketEmailService;
import ai.mnemosyne_systems.util.AttachmentHelper;
import ai.mnemosyne_systems.util.AuthHelper;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
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
    @Inject
    CrossReferenceService crossReferenceService;

    @Inject
    TicketEmailService ticketEmailService;

    @GET
    @Path("user")
    public Object home(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = AuthHelper.findUser(auth);
        if (AuthHelper.isSuperuser(user)) {
            return Response.seeOther(URI.create("/superuser")).build();
        }
        requireUser(auth);
        return Response.seeOther(URI.create("/user/tickets")).build();
    }

    @GET
    @Path("tam/users")
    public Response tamUsersRoot(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireUser(auth);
        if (!User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/user")).build());
        }
        return Response.seeOther(URI.create("/tam/users")).build();
    }

    @GET
    @Path("tam/users/{companyId}")
    public Response listTamUsers(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("companyId") Long companyId) {
        User user = requireUser(auth);
        if (!User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/user")).build());
        }
        Company selectedCompany = Company.findById(companyId);
        if (selectedCompany == null) {
            throw new NotFoundException();
        }
        boolean allowed = Company.count("select count(c) from Company c join c.users u where c = ?1 and u = ?2",
                selectedCompany, user) > 0;
        if (!allowed) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/tam/users?companyId=" + selectedCompany.id)).build();
    }

    @GET
    @Path("tam/users/{companyId}/create")
    public Response createTamUserForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("companyId") Long companyId) {
        User user = requireUser(auth);
        if (!User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/user")).build());
        }
        Company selectedCompany = Company.findById(companyId);
        if (selectedCompany == null) {
            throw new NotFoundException();
        }
        boolean allowed = Company.count("select count(c) from Company c join c.users u where c = ?1 and u = ?2",
                selectedCompany, user) > 0;
        if (!allowed) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/tam/users/new?companyId=" + selectedCompany.id)).build();
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
        if (User.usernameExists(name)) {
            throw new BadRequestException("Username already exists");
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
    public Response tickets(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireUser(auth);
        return Response.seeOther(URI.create("/user/tickets")).build();
    }

    @GET
    @Path("user/tickets/create")
    public Response createTicketForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireUser(auth);
        return Response.seeOther(URI.create("/user/tickets/new")).build();
    }

    @POST
    @Path("user/tickets")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response createUserTicket(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @HeaderParam("X-Billetsys-Client") String client, MultipartFormDataInput input) {
        User user = requireUser(auth);
        String status = AttachmentHelper.readFormValue(input, "status");
        String title = Ticket.normalizeTitle(AttachmentHelper.readFormValue(input, "title"));
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
        if (title == null) {
            throw new BadRequestException("Title is required");
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
        ticket.title = title;
        ticket.status = "Open";
        ticket.company = entitlement.company;
        ticket.requester = user;
        ticket.companyEntitlement = entitlement;
        ticket.affectsVersion = resolveKnownVersion(affectsVersionId, "Affects");
        ticket.category = categoryId != null ? Category.findById(categoryId) : Category.findDefault();
        ticket.persist();
        Message message = new Message();
        message.body = messageBody.trim();
        message.date = java.time.LocalDateTime.now();
        message.ticket = ticket;
        message.author = user;
        List<Attachment> attachments = AttachmentHelper.readAttachments(input, "attachments");
        AttachmentHelper.attachToMessage(message, attachments);
        message.persistAndFlush();
        AttachmentHelper.resolveInlineAttachmentUrls(message, attachments);
        ticketEmailService.notifyMessageChange(ticket, message, user);
        return createTicketRedirect(client, "/user/tickets/" + ticket.id);
    }

    @GET
    @Path("user/support-users/{id}")
    public Response viewSupportUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireUser(auth);
        User supportUser = User.findById(id);
        if (supportUser == null || !User.TYPE_SUPPORT.equalsIgnoreCase(supportUser.type)) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/user/support-users/" + id)).build();
    }

    @GET
    @Path("user/tam-users/{id}")
    public Response viewTamUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireUser(auth);
        User tamUser = User.findById(id);
        if (tamUser == null || !User.TYPE_TAM.equalsIgnoreCase(tamUser.type)) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/user/tam-users/" + id)).build();
    }

    @GET
    @Path("user/superuser-users/{id}")
    public Response viewSuperuser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireUser(auth);
        User superuser = User.findById(id);
        if (superuser == null || !User.TYPE_SUPERUSER.equalsIgnoreCase(superuser.type)) {
            throw new NotFoundException();
        }
        boolean allowed = Company.count(
                "select count(c) from Company c join c.users current join c.users viewed where current = ?1 and viewed = ?2",
                user, superuser) > 0;
        if (!allowed) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/user/superuser-users/" + id)).build();
    }

    @GET
    @Path("user/user-profiles/{id}")
    public Response viewUserProfile(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireUser(auth);
        User viewedUser = User.findById(id);
        if (viewedUser == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/user/user-profiles/" + id)).build();
    }

    @GET
    @Path("user/companies/{id}")
    public Response viewCompany(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireUser(auth);
        Company company = Company.findById(id);
        if (company == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/user/companies/" + id)).build();
    }

    @GET
    @Path("user/tickets/open")
    public Response tamOpenTickets(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireUser(auth);
        return Response.seeOther(URI.create("/user/tickets/open")).build();
    }

    @GET
    @Path("user/tickets/closed")
    public Response tamClosedTickets(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireUser(auth);
        return Response.seeOther(URI.create("/user/tickets/closed")).build();
    }

    @GET
    @Path("user/tickets/{id}")
    public Response ticketDetail(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireUser(auth);
        if (findTicketForUser(user, id) == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/user/tickets/" + id)).build();
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
        List<Attachment> attachments = AttachmentHelper.readAttachments(input, "attachments");
        AttachmentHelper.attachToMessage(message, attachments);
        message.persistAndFlush();
        SupportTicketData data = buildTicketDataForUser(user);
        java.util.Set<Long> accessibleIds = TicketSearchSupport
                .combineTickets(data.assignedTickets, data.openTickets, data.closedTickets).stream().map(t -> t.id)
                .collect(java.util.stream.Collectors.toSet());
        crossReferenceService.extractAndSaveReferences(message, accessibleIds);
        AttachmentHelper.resolveInlineAttachmentUrls(message, attachments);
        ticketEmailService.notifyMessageChange(ticket, message, user);
        return Response.seeOther(URI.create("/user/tickets/" + id + "?replyAdded=1")).build();
    }

    @GET
    @Path("user/tickets/{id}/edit")
    public Response ticketEdit(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireUser(auth);
        if (findTicketForUser(user, id) == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/user/tickets/" + id)).build();
    }

    @POST
    @Path("user/tickets/{id}")
    @Transactional
    public Response updateUserTicket(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            @HeaderParam("X-Billetsys-Client") String client, @FormParam("title") String title,
            @FormParam("affectsVersionId") Long affectsVersionId,
            @FormParam("resolvedVersionId") Long resolvedVersionId) {
        User user = requireUser(auth);
        String normalizedTitle = Ticket.normalizeTitle(title);
        Ticket ticket = findTicketForUser(user, id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        if (normalizedTitle == null) {
            throw new BadRequestException("Title is required");
        }
        ticket.title = normalizedTitle;
        ticket.affectsVersion = resolveVersionForTicket(ticket, affectsVersionId, "Affects");
        if (User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            ticket.resolvedVersion = resolveOptionalVersionForTicket(ticket, resolvedVersionId, "Resolved");
        }
        return ReactRedirectSupport.redirect(client, "/user/tickets/" + id);
    }

    @GET
    @Path("users")
    public Response listAdminUsersRoot(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("companyId") Long companyId) {
        requireAdmin(auth);
        if (companyId != null) {
            return Response.seeOther(URI.create("/users?companyId=" + companyId)).build();
        }
        return Response.seeOther(URI.create("/users")).build();
    }

    @GET
    @Path("users/{companyId}")
    public Response listAdminUsers(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("companyId") Long companyId) {
        requireAdmin(auth);
        Company selectedCompany = Company.findById(companyId);
        if (selectedCompany == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/users?companyId=" + selectedCompany.id)).build();
    }

    @GET
    @Path("users/create")
    public Response createAdminUserForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireAdmin(auth);
        return Response.seeOther(URI.create("/users/new")).build();
    }

    @GET
    @Path("user/{id}/edit")
    public Response editAdminUserForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireAdmin(auth);
        User editUser = User.findById(id);
        if (editUser == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/users/" + id + "/edit")).build();
    }

    @GET
    @Path("user/{id}")
    public Response viewAdminUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireAdmin(auth);
        User viewUser = User.findById(id);
        if (viewUser == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/users/" + id)).build();
    }

    @POST
    @Path("users")
    @Transactional
    public Response createAdminUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @HeaderParam("X-Billetsys-Client") String client, @FormParam("name") String name,
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
        newUser.type = normalizeType(type,
                Set.of(User.TYPE_ADMIN, User.TYPE_SUPPORT, User.TYPE_USER, User.TYPE_TAM, User.TYPE_SUPERUSER),
                "Type must be admin, support, user, tam, or superuser");
        newUser.passwordHash = BcryptUtil.bcryptHash(password);
        newUser.persist();
        if (companyId != null) {
            Company company = Company.findById(companyId);
            if (company != null) {
                company.users.add(newUser);
            }
        }
        return ReactRedirectSupport.redirect(client, "/users");
    }

    @POST
    @Path("user/{id}")
    @Transactional
    public Response updateAdminUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            @HeaderParam("X-Billetsys-Client") String client, @FormParam("name") String name,
            @FormParam("fullName") String fullName, @FormParam("email") String email,
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
        if (editUser.name == null || editUser.name.isBlank()) {
            editUser.name = name.trim();
        }
        editUser.fullName = trimOrNull(fullName);
        editUser.email = email.trim();
        editUser.social = trimOrNull(social);
        editUser.phoneNumber = trimOrNull(phoneNumber);
        editUser.phoneExtension = trimOrNull(phoneExtension);
        editUser.timezone = timezoneId != null ? Timezone.findById(timezoneId) : null;
        editUser.country = countryId != null ? Country.findById(countryId) : null;
        editUser.type = normalizeType(type,
                Set.of(User.TYPE_ADMIN, User.TYPE_SUPPORT, User.TYPE_USER, User.TYPE_TAM, User.TYPE_SUPERUSER),
                "Type must be admin, support, user, tam, or superuser");
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
        return ReactRedirectSupport.redirect(client, "/users");
    }

    private String trimOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @POST
    @Path("user/{id}/delete")
    @Transactional
    public Response deleteAdminUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @HeaderParam("X-Billetsys-Client") String client, @PathParam("id") Long id) {
        requireAdmin(auth);
        User deleteUser = User.findById(id);
        if (deleteUser == null) {
            throw new NotFoundException();
        }
        if (Company.count("superuser = ?1", deleteUser) > 0) {
            throw new BadRequestException("Reassign this company superuser before deleting the user.");
        }
        removeUserReferences(deleteUser);
        deleteUser.delete();
        return ReactRedirectSupport.redirect(client, "/users");
    }

    private void removeUserReferences(User user) {
        if (user != null && user.id != null && hasPasswordResetTokenTable()) {
            Panache.getEntityManager().createNativeQuery("delete from password_reset_tokens where user_id = ?1")
                    .setParameter(1, user.id).executeUpdate();
        }
        List<Company> companies = Company
                .find("select distinct c from Company c left join c.users u where u = ?1", user).list();
        for (Company company : companies) {
            company.users.removeIf(existing -> existing.id != null && existing.id.equals(user.id));
        }
        List<Ticket> supportTickets = Ticket
                .find("select distinct t from Ticket t join t.supportUsers u where u = ?1", user).list();
        for (Ticket ticket : supportTickets) {
            ticket.supportUsers.removeIf(existing -> existing.id != null && existing.id.equals(user.id));
        }
        List<Ticket> tamTickets = Ticket.find("select distinct t from Ticket t join t.tamUsers u where u = ?1", user)
                .list();
        for (Ticket ticket : tamTickets) {
            ticket.tamUsers.removeIf(existing -> existing.id != null && existing.id.equals(user.id));
        }
        List<Ticket> requesterTickets = Ticket.find("requester = ?1", user).list();
        for (Ticket ticket : requesterTickets) {
            ticket.requester = null;
        }
        List<Message> messages = Message.find("author = ?1", user).list();
        for (Message message : messages) {
            message.author = null;
        }
    }

    private boolean hasPasswordResetTokenTable() {
        Number matches = (Number) Panache.getEntityManager().createNativeQuery(
                "select count(*) from information_schema.tables where lower(table_name) = 'password_reset_tokens'")
                .getSingleResult();
        return matches != null && matches.longValue() > 0;
    }

    private void validateUserFields(String name, String email, String type, boolean requirePassword, String password) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Username is required");
        }
        if (User.usernameExists(name)) {
            throw new BadRequestException("Username already exists");
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

    SupportTicketData buildTamTicketData(User user) {
        java.util.List<Ticket> tickets = Ticket.list(
                "select distinct t from Ticket t left join t.tamUsers tu left join t.company c left join c.users cu where tu = ?1 or cu = ?1",
                user);
        return buildTicketDataFor(tickets);
    }

    SupportTicketData buildUserTicketData(User user) {
        java.util.List<Ticket> tickets = Ticket.list("requester = ?1", user);
        return buildTicketDataFor(tickets);
    }

    SupportTicketData buildTicketDataForUser(User user) {
        if (User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            return buildTamTicketData(user);
        }
        return buildUserTicketData(user);
    }

    private SupportTicketData buildTicketDataFor(java.util.List<Ticket> tickets) {
        return buildTicketDataFor(tickets, false);
    }

    private SupportTicketData buildTicketDataFor(java.util.List<Ticket> tickets,
            boolean keepExistingSlaForRequesterReplies) {
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
        java.util.Map<Long, java.time.LocalDateTime> slaDates = keepExistingSlaForRequesterReplies
                ? requesterPendingSlaDates(scopedTickets)
                : messageDates;
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
            java.time.LocalDateTime messageDate = slaDates.get(ticket.id);
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
        java.util.Map<Long, User> supportAssignmentUsers = new java.util.LinkedHashMap<>();
        for (Ticket ticket : scopedTickets) {
            User assignedSupport = User
                    .find("select u from Ticket t join t.supportUsers u where t = ?1 order by u.id desc", ticket)
                    .firstResult();
            if (assignedSupport != null) {
                supportAssignments.put(ticket.id, assignedSupport.email);
                supportAssignmentNames.put(ticket.id, assignedSupport.name);
                supportAssignmentIds.put(ticket.id, assignedSupport.id);
                supportAssignmentUsers.put(ticket.id, assignedSupport);
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
        data.supportAssignmentUsers = supportAssignmentUsers;
        return data;
    }

    private java.util.Map<Long, java.time.LocalDateTime> requesterPendingSlaDates(java.util.List<Ticket> tickets) {
        java.util.Map<Long, java.time.LocalDateTime> pendingDates = new java.util.LinkedHashMap<>();
        java.util.List<ai.mnemosyne_systems.model.Message> messages = ai.mnemosyne_systems.model.Message
                .find("order by date asc").list();
        for (ai.mnemosyne_systems.model.Message message : messages) {
            if (message.ticket == null || !tickets.contains(message.ticket) || message.ticket.id == null) {
                continue;
            }
            if (resetsRequesterSla(message.author)) {
                pendingDates.remove(message.ticket.id);
                continue;
            }
            pendingDates.putIfAbsent(message.ticket.id, message.date);
        }
        return pendingDates;
    }

    private boolean resetsRequesterSla(User author) {
        return AuthHelper.isSupport(author) || AuthHelper.isTam(author);
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

    boolean isEntitlementExpired(Ticket ticket) {
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

    boolean isEntitlementExpired(CompanyEntitlement entitlement) {
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

    String resolveLowestEntitlementLevelName(Ticket ticket) {
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
        displayTicket.title = ticket.title;
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
        displayTicket.title = ticket.title;
        displayTicket.status = ticket.status;
        displayTicket.company = ticket.company;
        displayTicket.companyEntitlement = ticket.companyEntitlement;
        displayTicket.affectsVersion = ticket.affectsVersion;
        displayTicket.resolvedVersion = ticket.resolvedVersion;
        displayTicket.category = ticket.category;
        displayTicket.externalIssueLink = ticket.externalIssueLink;
        return displayTicket;
    }

    static class SupportTicketData {
        List<Ticket> assignedTickets;
        List<Ticket> openTickets;
        List<Ticket> closedTickets;
        Map<Long, java.time.LocalDateTime> messageDates;
        Map<Long, String> messageDateLabels;
        Map<Long, String> messageDirectionArrows;
        Map<Long, String> slaColors;
        Map<Long, String> supportAssignments;
        Map<Long, String> supportAssignmentNames;
        Map<Long, Long> supportAssignmentIds;
        Map<Long, User> supportAssignmentUsers;
    }

    Ticket findTicketForUser(User user, Long id) {
        if (User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            return Ticket
                    .find("select distinct t from Ticket t join t.company c join c.users u where u = ?1 and t.id = ?2",
                            user, id)
                    .firstResult();
        }
        return Ticket.find("requester = ?1 and id = ?2", user, id).firstResult();
    }

    String formatDate(java.time.LocalDateTime date) {
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

    List<Version> availableVersions(Ticket ticket) {
        if (ticket == null || ticket.companyEntitlement == null || ticket.companyEntitlement.entitlement == null) {
            return List.of();
        }
        return Version.list("entitlement = ?1 order by date asc, id asc", ticket.companyEntitlement.entitlement);
    }

    Version defaultAffectsVersion(CompanyEntitlement entitlement) {
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

    List<Version> knownVersions() {
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

    private Response createTicketRedirect(String client, String path) {
        return ReactRedirectSupport.redirect(client, path);
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
            case User.TYPE_SUPERUSER -> "Superuser";
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
