/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.Version;
import ai.mnemosyne_systems.service.PdfService;
import ai.mnemosyne_systems.service.TicketEmailService;
import ai.mnemosyne_systems.util.AuthHelper;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Path("/tickets")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class TicketResource {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d yyyy, h.mma",
            Locale.ENGLISH);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10)).build();

    @Inject
    SupportResource supportResource;

    @Inject
    UserResource userResource;

    @Inject
    SuperuserResource superuserResource;

    @Inject
    TicketEmailService ticketEmailService;

    @Inject
    PdfService pdfService;

    @GET
    public Response list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireSupport(auth);
        return Response.seeOther(URI.create("/tickets")).build();
    }

    @GET
    @Path("/new")
    public Response createForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireSupport(auth);
        return Response.seeOther(URI.create("/tickets/new")).build();
    }

    @GET
    @Path("/{id}/edit")
    public Response editForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireSupport(auth);
        Ticket ticket = Ticket.find(
                "select t from Ticket t left join fetch t.companyEntitlement ce left join fetch ce.entitlement left join fetch ce.supportLevel where t.id = ?1",
                id).firstResult();
        if (ticket == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/tickets/" + id + "/edit")).build();
    }

    @GET
    @Path("/{id}")
    public Object viewTicket(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = AuthHelper.findUser(auth);
        if (AuthHelper.isSupport(user)) {
            return supportResource.ticketDetail(auth, id);
        }
        if (AuthHelper.isSuperuser(user)) {
            return superuserResource.ticketDetail(auth, id);
        }
        if (AuthHelper.isUser(user)) {
            return userResource.ticketDetail(auth, id);
        }
        return Response.seeOther(URI.create("/")).build();
    }

    @GET
    @Path("/alarm/status")
    @Produces(MediaType.TEXT_PLAIN)
    public Response alarmStatus(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = AuthHelper.findUser(auth);
        boolean alarm = hasAlarm(user);
        return Response.ok(Boolean.toString(alarm)).build();
    }

    @GET
    @Path("/external-preview")
    public Response externalPreview(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @QueryParam("url") String url) {
        requireSupport(auth);
        if (url == null || url.isBlank()) {
            throw new BadRequestException("URL is required");
        }
        URI previewUri;
        try {
            previewUri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("URL is invalid");
        }
        String scheme = previewUri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new BadRequestException("URL is invalid");
        }
        String html = fetchExternalPreviewHtml(previewUri);
        return Response.ok(html).type(MediaType.TEXT_HTML + ";charset=UTF-8").build();
    }

    @POST
    @Transactional
    public Response create(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @HeaderParam("X-Billetsys-Client") String client, @FormParam("status") String status,
            @FormParam("companyId") Long companyId, @FormParam("companyEntitlementId") Long companyEntitlementId,
            @FormParam("categoryId") Long categoryId) {
        User user = requireSupport(auth);
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
        Category category = categoryId != null ? Category.findById(categoryId) : Category.findDefault();
        Ticket ticket = new Ticket();
        ticket.name = Ticket.nextName(company);
        ticket.status = status;
        ticket.company = company;
        ticket.requester = user;
        ticket.companyEntitlement = entitlement;
        ticket.affectsVersion = defaultAffectsVersion(entitlement);
        ticket.category = category;
        ticket.persist();
        return ReactRedirectSupport.redirect(client, "/tickets");
    }

    @POST
    @Path("/{id}")
    @Transactional
    public Response update(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            @HeaderParam("X-Billetsys-Client") String client, @FormParam("status") String status,
            @FormParam("companyId") Long companyId, @FormParam("companyEntitlementId") Long companyEntitlementId,
            @FormParam("categoryId") Long categoryId, @FormParam("externalIssueLink") String externalIssueLink,
            @FormParam("affectsVersionId") Long affectsVersionId,
            @FormParam("resolvedVersionId") Long resolvedVersionId) {
        User user = requireSupport(auth);
        Ticket ticket = Ticket.findById(id);
        if (ticket == null) {
            throw new NotFoundException();
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
        ticket.status = status;
        ticket.company = company;
        ticket.requester = user;
        ticket.companyEntitlement = entitlement;
        ticket.affectsVersion = resolveVersion(entitlement, affectsVersionId, "Affects", true);
        ticket.resolvedVersion = resolveVersion(entitlement, resolvedVersionId, "Resolved", false);
        ticket.category = categoryId != null ? Category.findById(categoryId) : null;
        ticket.externalIssueLink = externalIssueLink != null && !externalIssueLink.isBlank() ? externalIssueLink.trim()
                : null;
        if (!sameStatus(previousStatus, ticket.status)) {
            ticketEmailService.notifyStatusChange(ticket, previousStatus, user);
        }
        return ReactRedirectSupport.redirect(client, "/tickets");
    }

    @POST
    @Path("/{id}/delete")
    @Transactional
    public Response delete(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @HeaderParam("X-Billetsys-Client") String client, @PathParam("id") Long id) {
        requireSupport(auth);
        Ticket ticket = Ticket.findById(id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        ticket.delete();
        return ReactRedirectSupport.redirect(client, "/tickets");
    }

    @GET
    @Path("/company/{id}/entitlements")
    public Response companyEntitlements(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireSupport(auth);
        Company company = Company.findById(id);
        if (company == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/tickets/new?companyId=" + company.id)).build();
    }

    private List<Version> availableVersions(CompanyEntitlement companyEntitlement) {
        if (companyEntitlement == null || companyEntitlement.entitlement == null) {
            return java.util.List.of();
        }
        return Version.list("entitlement = ?1 order by date asc, id asc", companyEntitlement.entitlement);
    }

    private Version defaultAffectsVersion(CompanyEntitlement companyEntitlement) {
        if (companyEntitlement == null || companyEntitlement.entitlement == null) {
            return null;
        }
        Version version = Version.find("entitlement = ?1 and name = ?2 order by date asc, id asc",
                companyEntitlement.entitlement, "1.0.0").firstResult();
        if (version != null) {
            return version;
        }
        return Version.find("entitlement = ?1 order by date asc, id asc", companyEntitlement.entitlement).firstResult();
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

    @GET
    @Path("/export/{id}")
    @Produces("application/pdf")
    public Response exportTicketToPdf(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isSupport(user) && !AuthHelper.isUser(user) && !AuthHelper.isSuperuser(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        Ticket ticket = Ticket.findById(id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        if ((AuthHelper.isTam(user) || AuthHelper.isSuperuser(user))
                && ticket.company.users.stream().noneMatch(u -> u.id.equals(user.id))) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        if (User.TYPE_USER.equalsIgnoreCase(user.type)
                && (ticket.requester == null || !ticket.requester.id.equals(user.id))) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        byte[] ticketPdf = pdfService.generateTicketPdf(ticket);
        return Response.ok(ticketPdf).header("Content-Disposition", "attachment; filename=\"" + ticket.name + ".pdf\"")
                .build();
    }

    private User requireSupport(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isSupport(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }

    private String fetchExternalPreviewHtml(URI previewUri) {
        HttpRequest request = HttpRequest.newBuilder(previewUri).timeout(Duration.ofSeconds(15))
                .header("User-Agent", "billetsys-preview").GET().build();
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return previewErrorHtml(previewUri, "Unable to load page (" + response.statusCode() + ").");
            }
            return wrapPreviewHtml(previewUri, response.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return previewErrorHtml(previewUri, "Unable to load page: " + e.getMessage());
        }
    }

    private String wrapPreviewHtml(URI previewUri, String body) {
        String escapedUrl = escapeHtml(previewUri.toString());
        String baseHref = escapeHtml(previewUri.resolve("/").toString());
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>" + escapedUrl + "</title><base href=\""
                + baseHref + "\"></head><body>" + body + "</body></html>";
    }

    private String previewErrorHtml(URI previewUri, String message) {
        String escapedUrl = escapeHtml(previewUri.toString());
        String escapedMessage = escapeHtml(message);
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>" + escapedUrl
                + "</title><style>body{font-family:Arial,sans-serif;margin:0;padding:16px;color:#1a1a1a;}a{color:#b00020;}pre{white-space:pre-wrap;}</style></head><body><h1>"
                + escapedUrl + "</h1><p>" + escapedMessage + "</p><p><a href=\"" + escapedUrl
                + "\" target=\"_blank\" rel=\"noopener\">Open in new tab</a></p></body></html>";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static String externalPreviewUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return "/tickets/external-preview?url=" + URLEncoder.encode(url.trim(), StandardCharsets.UTF_8);
    }

    private boolean hasAlarm(User user) {
        if (user == null) {
            return false;
        }
        List<Ticket> tickets = ticketsForAlarm(user);
        if (tickets.isEmpty()) {
            return false;
        }
        Map<Long, LocalDateTime> latestMessageDates = latestMessageDates(tickets);
        LocalDateTime now = LocalDateTime.now();
        for (Ticket ticket : tickets) {
            if (ticket == null || ticket.id == null || ticket.companyEntitlement == null
                    || ticket.companyEntitlement.supportLevel == null) {
                continue;
            }
            LocalDateTime created = latestMessageDates.get(ticket.id);
            if (created == null) {
                continue;
            }
            long minutes = Duration.between(created, now).toMinutes();
            if (minutes < 0) {
                minutes = 0;
            }
            Integer levelValue = ticket.companyEntitlement.supportLevel.level;
            String color = ticket.companyEntitlement.supportLevel.color;
            if (levelValue == null || color == null || color.isBlank()) {
                continue;
            }
            if (minutes >= levelValue.longValue() && !"white".equalsIgnoreCase(color.trim())) {
                return true;
            }
        }
        return false;
    }

    private List<Ticket> ticketsForAlarm(User user) {
        if (AuthHelper.isSupport(user)) {
            List<Ticket> assigned = Ticket.find(
                    "select distinct t from Ticket t join t.supportUsers u where u = ?1 and (t.status is null or lower(t.status) <> 'closed')",
                    user).list();
            List<Ticket> open = Ticket.find(
                    "select distinct t from Ticket t where t.supportUsers is empty and (t.status is null or lower(t.status) <> 'closed')")
                    .list();
            return combineTickets(assigned, open);
        }
        if (User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            return Ticket.find(
                    "select distinct t from Ticket t left join t.tamUsers tu left join t.company c left join c.users cu where (tu = ?1 or cu = ?1) and (t.status is null or lower(t.status) <> 'closed')",
                    user).list();
        }
        if (User.TYPE_SUPERUSER.equalsIgnoreCase(user.type)) {
            return Ticket.find(
                    "select distinct t from Ticket t join t.company c join c.users u where u = ?1 and (t.status is null or lower(t.status) <> 'closed')",
                    user).list();
        }
        if (User.TYPE_USER.equalsIgnoreCase(user.type)) {
            return Ticket.find("requester = ?1 and (status is null or lower(status) <> 'closed')", user).list();
        }
        return List.of();
    }

    private List<Ticket> combineTickets(List<Ticket> first, List<Ticket> second) {
        List<Ticket> combined = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Ticket ticket : first) {
            if (ticket != null && ticket.id != null && seen.add(ticket.id)) {
                combined.add(ticket);
            }
        }
        for (Ticket ticket : second) {
            if (ticket != null && ticket.id != null && seen.add(ticket.id)) {
                combined.add(ticket);
            }
        }
        return combined;
    }

    private Map<Long, LocalDateTime> latestMessageDates(List<Ticket> tickets) {
        Set<Long> ticketIds = new HashSet<>();
        for (Ticket ticket : tickets) {
            if (ticket != null && ticket.id != null) {
                ticketIds.add(ticket.id);
            }
        }
        Map<Long, LocalDateTime> result = new LinkedHashMap<>();
        if (ticketIds.isEmpty()) {
            return result;
        }
        List<Message> messages = Message.find("order by date desc").list();
        for (Message message : messages) {
            if (message.ticket == null || message.ticket.id == null || !ticketIds.contains(message.ticket.id)) {
                continue;
            }
            result.putIfAbsent(message.ticket.id, message.date);
            if (result.size() == ticketIds.size()) {
                break;
            }
        }
        return result;
    }

    private String formatDate(LocalDateTime date) {
        String formatted = DATE_FORMATTER.format(date);
        return formatted.replace("AM", "am").replace("PM", "pm");
    }

    private boolean sameStatus(String left, String right) {
        String normalizedLeft = left == null ? "" : left.trim();
        String normalizedRight = right == null ? "" : right.trim();
        return normalizedLeft.equalsIgnoreCase(normalizedRight);
    }
}
