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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Path("/tickets")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class TicketResource {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d yyyy, h.mma",
            Locale.ENGLISH);

    @Location("tickets/list.html")
    Template listTemplate;

    @Location("tickets/form.html")
    Template formTemplate;

    @GET
    public TemplateInstance list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireSupport(auth);
        List<Ticket> tickets = Ticket.listAll();
        List<Message> messages = Message.list("order by date desc");
        Map<Long, List<Message>> ticketMessages = new LinkedHashMap<>();
        Map<Long, String> messageLabels = new LinkedHashMap<>();
        for (Message message : messages) {
            if (message.ticket == null) {
                continue;
            }
            ticketMessages.computeIfAbsent(message.ticket.id, ignored -> new java.util.ArrayList<>()).add(message);
            if (message.date != null) {
                messageLabels.put(message.id, formatDate(message.date));
            }
        }
        for (Ticket ticket : tickets) {
            ticketMessages.computeIfAbsent(ticket.id, ignored -> java.util.List.of());
        }
        return listTemplate.data("tickets", tickets).data("ticketMessages", ticketMessages)
                .data("messageLabels", messageLabels).data("currentUser", user);
    }

    @GET
    @Path("/new")
    public TemplateInstance createForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireSupport(auth);
        return formTemplate.data("ticket", new Ticket()).data("companies", Company.listAll())
                .data("companyEntitlements", java.util.List.of()).data("selectedCompanyEntitlementId", null)
                .data("action", "/tickets").data("title", "New Ticket").data("currentUser", user);
    }

    @GET
    @Path("/{id}/edit")
    public TemplateInstance editForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireSupport(auth);
        Ticket ticket = Ticket.find(
                "select t from Ticket t left join fetch t.companyEntitlement ce left join fetch ce.entitlement left join fetch ce.supportLevel where t.id = ?1",
                id).firstResult();
        if (ticket == null) {
            throw new NotFoundException();
        }
        List<Message> messages = Message.list("ticket = ?1 order by date desc", ticket);
        Map<Long, String> messageLabels = new LinkedHashMap<>();
        for (Message message : messages) {
            if (message.date != null) {
                messageLabels.put(message.id, formatDate(message.date));
            }
        }
        return formTemplate.data("ticket", ticket).data("companies", Company.listAll())
                .data("companyEntitlements", ticket.company == null ? java.util.List.of() : CompanyEntitlement.find(
                        "select distinct ce from CompanyEntitlement ce join fetch ce.entitlement join fetch ce.supportLevel where ce.company = ?1",
                        ticket.company).list())
                .data("selectedCompanyEntitlementId",
                        ticket.companyEntitlement == null ? null : ticket.companyEntitlement.id)
                .data("messages", messages).data("messageLabels", messageLabels).data("action", "/tickets/" + id)
                .data("title", "Edit Ticket").data("currentUser", user);
    }

    @POST
    @Transactional
    public Response create(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @FormParam("status") String status,
            @FormParam("companyId") Long companyId, @FormParam("companyEntitlementId") Long companyEntitlementId) {
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
        Ticket ticket = new Ticket();
        ticket.name = Ticket.nextName(company);
        ticket.status = status;
        ticket.company = company;
        ticket.requester = user;
        ticket.companyEntitlement = entitlement;
        ticket.persist();
        return Response.seeOther(URI.create("/tickets")).build();
    }

    @POST
    @Path("/{id}")
    @Transactional
    public Response update(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            @FormParam("status") String status, @FormParam("companyId") Long companyId,
            @FormParam("companyEntitlementId") Long companyEntitlementId) {
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
        ticket.status = status;
        ticket.company = company;
        ticket.requester = user;
        ticket.companyEntitlement = entitlement;
        return Response.seeOther(URI.create("/tickets")).build();
    }

    @POST
    @Path("/{id}/delete")
    @Transactional
    public Response delete(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireSupport(auth);
        Ticket ticket = Ticket.findById(id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        ticket.delete();
        return Response.seeOther(URI.create("/tickets")).build();
    }

    @GET
    @Path("/company/{id}/entitlements")
    public TemplateInstance companyEntitlements(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        User user = requireSupport(auth);
        Company company = Company.findById(id);
        if (company == null) {
            throw new NotFoundException();
        }
        java.util.List<CompanyEntitlement> entitlements = CompanyEntitlement.find(
                "select distinct ce from CompanyEntitlement ce join fetch ce.entitlement join fetch ce.supportLevel where ce.company = ?1",
                company).list();
        Ticket ticket = new Ticket();
        ticket.company = company;
        ticket.name = Ticket.previewNextName(company);
        return formTemplate.data("ticket", ticket).data("companies", Company.listAll())
                .data("companyEntitlements", entitlements).data("selectedCompanyEntitlementId", null)
                .data("action", "/tickets").data("title", "New Ticket").data("currentUser", user);
    }

    private User requireSupport(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isSupport(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }

    private String formatDate(LocalDateTime date) {
        String formatted = DATE_FORMATTER.format(date);
        return formatted.replace("AM", "am").replace("PM", "pm");
    }
}
