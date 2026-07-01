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
import ai.mnemosyne_systems.util.CurrentUser;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;

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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/support")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
@RolesAllowed("support")
public class SupportResource {

    @Inject
    CurrentUser currentUser;

    @Inject
    CrossReferenceService crossReferenceService;

    @Inject
    TicketEmailService ticketEmailService;

    @GET
    public Response listTickets() {
        return Response.seeOther(URI.create("/support/tickets")).build();
    }

    @GET
    @Path("/open")
    public Response listOpenTickets() {
        return Response.seeOther(URI.create("/support/tickets/open")).build();
    }

    @GET
    @Path("/closed")
    public Response listClosedTickets() {
        return Response.seeOther(URI.create("/support/tickets/closed")).build();
    }

    @GET
    @Path("/support-users/{id}")
    public Response viewSupportUser(@PathParam("id") Long id) {
        User supportUser = User.findById(id);
        if (supportUser == null || !User.TYPE_SUPPORT.equalsIgnoreCase(supportUser.type)) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/support/support-users/" + id)).build();
    }

    @GET
    @Path("/tam-users/{id}")
    public Response viewTamUser(@PathParam("id") Long id) {
        User tamUser = User.findById(id);
        if (tamUser == null || !User.TYPE_TAM.equalsIgnoreCase(tamUser.type)) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/support/tam-users/" + id)).build();
    }

    @GET
    @Path("/superuser-users/{id}")
    public Response viewSuperuser(@PathParam("id") Long id) {
        User superuser = User.findById(id);
        if (superuser == null || !User.TYPE_SUPERUSER.equalsIgnoreCase(superuser.type)) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/support/superuser-users/" + id)).build();
    }

    @GET
    @Path("/user-profiles/{id}")
    public Response viewUserProfile(@PathParam("id") Long id) {
        User viewedUser = User.findById(id);
        if (viewedUser == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/support/user-profiles/" + id)).build();
    }

    @GET
    @Path("/companies/{id}")
    public Response viewCompany(@PathParam("id") Long id) {
        Company company = Company.findById(id);
        if (company == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/support/companies/" + id)).build();
    }

    @GET
    @Path("/users")
    public Response supportUsersRoot() {
        return Response.seeOther(URI.create("/support/users")).build();
    }

    @GET
    @Path("/users/{companyId}")
    public Response listSupportUsers(@PathParam("companyId") Long companyId) {
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
        return Response.seeOther(URI.create("/support/users?companyId=" + selectedCompany.id)).build();
    }

    @GET
    @Path("/users/{companyId}/create")
    public Response createSupportUserForm(@PathParam("companyId") Long companyId) {
        Company selectedCompany = Company.findById(companyId);
        if (selectedCompany == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/support/users/new?companyId=" + selectedCompany.id)).build();
    }

    @POST
    @Path("/users")
    @Transactional
    public Response createSupportUser(@FormParam("name") String name, @FormParam("fullName") String fullName,
            @FormParam("email") String email, @FormParam("social") String social,
            @FormParam("phoneNumber") String phoneNumber, @FormParam("phoneExtension") String phoneExtension,
            @FormParam("timezoneId") Long timezoneId, @FormParam("countryId") Long countryId,
            @FormParam("password") String password, @FormParam("type") String type,
            @FormParam("companyId") Long companyId, @Context HttpServletRequest request) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Username is required");
        }
        if (User.usernameExists(name)) {
            throw new BadRequestException("Username already exists");
        }
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required");
        }
        if (!User.TYPE_EXTERNAL.equalsIgnoreCase(type) && (password == null || password.isBlank())) {
            throw new BadRequestException("Password is required");
        }
        if (companyId == null) {
            throw new BadRequestException("Company is required");
        }
        Company company = Company.findById(companyId);
        if (company == null) {
            throw new NotFoundException();
        }
        String normalized = normalizeType(type, Set.of(User.TYPE_USER, User.TYPE_EXTERNAL),
                "Type must be user or external");
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
        if (password != null && !password.isBlank()) {
            newUser.passwordHash = BcryptUtil.bcryptHash(password);
        }
        newUser.persist();
        boolean exists = company.users.stream()
                .anyMatch(existing -> existing.id != null && existing.id.equals(newUser.id));
        if (!exists) {
            company.users.add(newUser);
        }
        return Response.seeOther(URI.create("/support/users/" + company.id)).build();
    }

    private String trimOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @GET
    @Path("/tickets/create")
    public Response createForm() {
        return Response.seeOther(URI.create("/support/tickets/new")).build();
    }

    @GET
    @Path("/tickets/{id}")
    public Response ticketDetail(@jakarta.ws.rs.PathParam("id") Long id) {
        Ticket ticket = Ticket.findById(id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/support/tickets/" + id)).build();
    }

    private List<Message> loadMessages(Ticket ticket, User viewer) {
        return SupportTicketViewSupport.loadMessages(ticket, viewer);
    }

    @POST
    @Path("/tickets/{id}/messages")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response addMessage(@jakarta.ws.rs.PathParam("id") Long id, MultipartFormDataInput input) {
        User user = currentUser.get();
        String body = AttachmentHelper.readFormValue(input, "body");
        if (body == null || body.isBlank()) {
            throw new BadRequestException("Message is required");
        }
        Ticket ticket = Ticket.findById(id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        String previousStatus = ticketEmailService.computeEffectiveStatus(ticket, ticket.status);
        boolean isPublic = AttachmentHelper.readFormBoolean(input, "isPublic", true);
        Message message = new Message();
        message.body = body;
        message.date = LocalDateTime.now();
        message.ticket = ticket;
        message.author = user;
        message.isPublic = isPublic;
        List<Attachment> attachments = AttachmentHelper.readAttachments(input, "attachments");
        AttachmentHelper.attachToMessage(message, attachments);
        message.persistAndFlush();
        crossReferenceService.extractAndSaveReferences(message, null);
        AttachmentHelper.resolveInlineAttachmentUrls(message, attachments);
        if (ticket.supportUsers.stream().noneMatch(existing -> existing.id != null && existing.id.equals(user.id))) {
            ticket.supportUsers.add(user);
        }
        if (ticket.status == null || ticket.status.isBlank() || "Open".equalsIgnoreCase(ticket.status)) {
            ticket.status = "Assigned";
        }
        if (!sameStatus(previousStatus, ticket.status)) {
            ticketEmailService.notifyStatusChange(ticket, previousStatus, user);
        }
        ticketEmailService.notifyMessageChange(ticket, message, user);
        return Response.seeOther(URI.create("/support/tickets/" + id + "?replyAdded=1")).build();
    }

    @POST
    @Path("/tickets")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response createTicket(@HeaderParam("X-Billetsys-Client") String client, MultipartFormDataInput input) {
        User user = currentUser.get();
        String status = AttachmentHelper.readFormValue(input, "status");
        String title = Ticket.normalizeTitle(AttachmentHelper.readFormValue(input, "title"));
        String messageBody = AttachmentHelper.readFormValue(input, "message");
        Long companyId = AttachmentHelper.readFormLong(input, "companyId");
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
        if (companyId == null) {
            throw new BadRequestException("Company is required");
        }
        if (companyEntitlementId == null) {
            throw new BadRequestException("Entitlement is required");
        }
        Company company = Company.findById(companyId);
        if (company == null) {
            throw new NotFoundException();
        }
        CompanyEntitlement entitlement = CompanyEntitlement
                .find("company = ?1 and id = ?2", company, companyEntitlementId).firstResult();
        if (entitlement == null) {
            throw new BadRequestException("Entitlement is required");
        }
        Ticket ticket = new Ticket();
        ticket.name = Ticket.nextName(company);
        ticket.title = title;
        ticket.status = "Open";
        ticket.company = company;
        ticket.requester = user;
        ticket.companyEntitlement = entitlement;
        ticket.affectsVersion = resolveVersion(entitlement, affectsVersionId, "Affects", true);
        ticket.category = categoryId != null ? Category.findById(categoryId) : Category.findDefault();
        ticket.persist();
        assignCompanyTams(ticket);
        boolean isPublic = AttachmentHelper.readFormBoolean(input, "isPublic", true);
        Message message = new Message();
        message.body = messageBody;
        message.date = LocalDateTime.now();
        message.ticket = ticket;
        message.author = user;
        message.isPublic = isPublic;
        List<Attachment> attachments = AttachmentHelper.readAttachments(input, "attachments");
        AttachmentHelper.attachToMessage(message, attachments);
        message.persistAndFlush();
        AttachmentHelper.resolveInlineAttachmentUrls(message, attachments);
        ticketEmailService.notifyMessageChange(ticket, message, user);
        return createTicketRedirect(client, "/support/tickets/" + ticket.id);
    }

    @POST
    @Path("/tickets/{id}")
    @Transactional
    public Response updateTicket(@HeaderParam("X-Billetsys-Client") String client,
            @jakarta.ws.rs.PathParam("id") Long id, @FormParam("title") String title,
            @FormParam("status") String status, @FormParam("companyId") Long companyId,
            @FormParam("companyEntitlementId") Long companyEntitlementId, @FormParam("categoryId") Long categoryId,
            @FormParam("externalIssueLink") String externalIssueLink,
            @FormParam("affectsVersionId") Long affectsVersionId,
            @FormParam("resolvedVersionId") Long resolvedVersionId) {
        User user = currentUser.get();
        String normalizedTitle = Ticket.normalizeTitle(title);
        Ticket ticket = Ticket.findById(id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        if (normalizedTitle == null) {
            throw new BadRequestException("Title is required");
        }
        if (status == null || status.isBlank()) {
            throw new BadRequestException("Status is required");
        }
        if (companyId == null) {
            throw new BadRequestException("Company is required");
        }
        if (companyEntitlementId == null) {
            throw new BadRequestException("Entitlement is required");
        }
        Company company = Company.findById(companyId);
        if (company == null) {
            throw new NotFoundException();
        }
        CompanyEntitlement entitlement = CompanyEntitlement
                .find("company = ?1 and id = ?2", company, companyEntitlementId).firstResult();
        if (entitlement == null) {
            throw new BadRequestException("Entitlement is required");
        }
        String previousStatus = ticketEmailService.computeEffectiveStatus(ticket, ticket.status);
        if (sameStatus(status, "Resolved") || sameStatus(status, "Closed")) {
            if (ticket.resolvedAt == null) {
                ticket.resolvedAt = java.time.LocalDateTime.now();
            }
        } else {
            ticket.resolvedAt = null;
        }
        ticket.title = normalizedTitle;
        ticket.status = status;
        ticket.company = company;
        ticket.companyEntitlement = entitlement;
        ticket.affectsVersion = resolveVersion(entitlement, affectsVersionId, "Affects", true);
        ticket.resolvedVersion = resolveVersion(entitlement, resolvedVersionId, "Resolved", false);
        ticket.category = categoryId != null ? Category.findById(categoryId) : null;
        ticket.externalIssueLink = externalIssueLink != null && !externalIssueLink.isBlank() ? externalIssueLink.trim()
                : null;
        if ("Assigned".equalsIgnoreCase(status)) {
            boolean assigned = ticket.supportUsers.stream()
                    .anyMatch(existing -> existing.id != null && existing.id.equals(user.id));
            if (!assigned) {
                ticket.supportUsers.add(user);
            }
        }
        assignCompanyTams(ticket);
        if (!sameStatus(previousStatus, ticket.status)) {
            ticketEmailService.notifyStatusChange(ticket, previousStatus, user);
        }
        return createTicketRedirect(client, "/support/tickets/" + id);
    }

    @POST
    @Path("/tickets/{id}/assign")
    @Transactional
    public Response assignTicket(@HeaderParam("X-Billetsys-Client") String client,
            @jakarta.ws.rs.PathParam("id") Long id) {
        User user = currentUser.get();
        Ticket ticket = Ticket.findById(id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        String previousStatus = ticketEmailService.computeEffectiveStatus(ticket, ticket.status);
        if (ticket.supportUsers.stream().noneMatch(existing -> existing.id != null && existing.id.equals(user.id))) {
            ticket.supportUsers.add(user);
        }
        if (ticket.status == null || ticket.status.isBlank() || "Open".equalsIgnoreCase(ticket.status)) {
            ticket.status = "Assigned";
        }
        assignCompanyTams(ticket);
        if (!sameStatus(previousStatus, ticket.status)) {
            ticketEmailService.notifyStatusChange(ticket, previousStatus, user);
        }
        return createTicketRedirect(client, "/support/tickets/" + id);
    }

    private void assignCompanyTams(Ticket ticket) {
        if (ticket == null || ticket.company == null) {
            return;
        }
        java.util.List<User> tams = User
                .find("select u from Company c join c.users u where c = ?1 and lower(u.type) = ?2", ticket.company,
                        User.TYPE_TAM)
                .list();
        if (tams.isEmpty()) {
            return;
        }
        ticket.tamUsers.size();
        java.util.Set<Long> existingIds = new HashSet<>();
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

    @GET
    @Path("/tickets/company/{id}/entitlements")
    public Response companyEntitlements(@jakarta.ws.rs.PathParam("id") Long id, @QueryParam("message") String message) {
        Company company = Company.findById(id);
        if (company == null) {
            throw new NotFoundException();
        }
        String suffix = message == null || message.isBlank() ? ""
                : "&message=" + java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
        return Response.seeOther(URI.create("/support/tickets/new?companyId=" + company.id + suffix)).build();
    }

    private List<CompanyEntitlement> uniqueEntitlements(List<CompanyEntitlement> entries) {
        return SupportTicketViewSupport.uniqueEntitlements(entries);
    }

    private List<Version> availableVersions(CompanyEntitlement companyEntitlement) {
        return SupportTicketViewSupport.availableVersions(companyEntitlement);
    }

    private Version defaultAffectsVersion(CompanyEntitlement companyEntitlement) {
        return SupportTicketViewSupport.defaultAffectsVersion(companyEntitlement);
    }

    private Version resolveVersion(CompanyEntitlement companyEntitlement, Long versionId, String label,
            boolean required) {
        if (versionId == null) {
            if (required && !availableVersions(companyEntitlement).isEmpty()) {
                throw new BadRequestException(label + " version is required");
            }
            return null;
        }
        if (companyEntitlement == null || companyEntitlement.entitlement == null) {
            throw new BadRequestException(label + " version is invalid");
        }
        Version version = Version.find("id = ?1 and entitlement = ?2", versionId, companyEntitlement.entitlement)
                .firstResult();
        if (version == null) {
            throw new BadRequestException(label + " version is invalid");
        }
        return version;
    }

    private Response createTicketRedirect(String client, String path) {
        return ReactRedirectSupport.redirect(client, path);
    }

    private String formatDate(LocalDateTime date) {
        return SupportTicketViewSupport.formatDate(date);
    }

    private boolean sameStatus(String left, String right) {
        String normalizedLeft = left == null ? "" : left.trim();
        String normalizedRight = right == null ? "" : right.trim();
        return normalizedLeft.equalsIgnoreCase(normalizedRight);
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
        return SupportTicketViewSupport.isEntitlementExpired(ticket);
    }

    static SupportTicketCounts loadTicketCounts(User user) {
        SupportTicketViewSupport.SupportTicketCounts counts = SupportTicketViewSupport.loadTicketCounts(user);
        return new SupportTicketCounts(counts.assignedCount(), counts.openCount());
    }

    private String normalizeType(String type, Set<String> allowedTypes, String errorMessage) {
        String normalized = type == null ? "" : type.trim().toLowerCase();
        if (!allowedTypes.contains(normalized)) {
            throw new BadRequestException(errorMessage);
        }
        return normalized;
    }

    static class SupportTicketCounts {
        final int assignedCount;
        final int openCount;

        SupportTicketCounts(int assignedCount, int openCount) {
            this.assignedCount = assignedCount;
            this.openCount = openCount;
        }
    }

}
