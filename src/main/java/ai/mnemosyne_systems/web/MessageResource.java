/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/messages")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class MessageResource {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Location("messages/list.html")
    Template listTemplate;

    @Location("messages/form.html")
    Template formTemplate;

    @Inject
    TicketEmailService ticketEmailService;

    @GET
    public TemplateInstance list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireSupport(auth);
        List<Message> messages = Message.find("select distinct m from Message m left join fetch m.attachments").list();
        if (!messages.isEmpty()) {
            messages = new ArrayList<>(new LinkedHashSet<>(messages));
        }
        return listTemplate.data("messages", messages).data("currentUser", user);
    }

    @GET
    @Path("/new")
    public TemplateInstance createForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireSupport(auth);
        return formTemplate.data("message", new Message()).data("tickets", Ticket.listAll()).data("dateValue", "")
                .data("action", "/messages").data("title", "New Message").data("currentUser", user);
    }

    @GET
    @Path("/{id}/edit")
    public TemplateInstance editForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireSupport(auth);
        Message message = Message.findById(id);
        if (message == null) {
            throw new NotFoundException();
        }
        String dateValue = message.date == null ? "" : message.date.format(DATE_TIME_FORMATTER);
        return formTemplate.data("message", message).data("tickets", Ticket.listAll()).data("dateValue", dateValue)
                .data("action", "/messages/" + id).data("title", "Edit Message").data("currentUser", user);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response create(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, MultipartFormDataInput input) {
        User user = requireSupport(auth);
        String body = AttachmentHelper.readFormValue(input, "body");
        String date = AttachmentHelper.readFormValue(input, "date");
        Long ticketId = AttachmentHelper.readFormLong(input, "ticketId");
        Message message = buildMessage(null, user, body, date, ticketId);
        AttachmentHelper.attachToMessage(message, AttachmentHelper.readAttachments(input, "attachments"));
        message.persist();
        ticketEmailService.notifyMessageChange(message.ticket, message, user);
        return Response.seeOther(URI.create("/messages")).build();
    }

    @POST
    @Path("/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response update(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            MultipartFormDataInput input) {
        User user = requireSupport(auth);
        String body = AttachmentHelper.readFormValue(input, "body");
        String date = AttachmentHelper.readFormValue(input, "date");
        Long ticketId = AttachmentHelper.readFormLong(input, "ticketId");
        Message message = Message.findById(id);
        if (message == null) {
            throw new NotFoundException();
        }
        buildMessage(message, user, body, date, ticketId);
        AttachmentHelper.attachToMessage(message, AttachmentHelper.readAttachments(input, "attachments"));
        ticketEmailService.notifyMessageChange(message.ticket, message, user);
        return Response.seeOther(URI.create("/messages")).build();
    }

    @POST
    @Path("/{id}/delete")
    @Transactional
    public Response delete(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireSupport(auth);
        Message message = Message.findById(id);
        if (message == null) {
            throw new NotFoundException();
        }
        message.delete();
        return Response.seeOther(URI.create("/messages")).build();
    }

    private Message buildMessage(Message existing, User author, String body, String date, Long ticketId) {
        if (body == null || body.isBlank()) {
            throw new BadRequestException("Body is required");
        }
        if (ticketId == null) {
            throw new BadRequestException("Ticket is required");
        }
        Ticket ticket = Ticket.findById(ticketId);
        if (ticket == null) {
            throw new NotFoundException();
        }
        if (date == null || date.isBlank()) {
            throw new BadRequestException("Date is required");
        }
        LocalDateTime parsedDate;
        try {
            parsedDate = LocalDateTime.parse(date, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Invalid date format");
        }

        Message message = existing == null ? new Message() : existing;
        message.body = body;
        message.date = parsedDate;
        message.ticket = ticket;
        message.author = author;
        return message;
    }

    private User requireSupport(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isSupport(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }
}
