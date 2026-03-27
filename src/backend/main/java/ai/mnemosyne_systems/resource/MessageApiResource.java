package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Attachment;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import io.smallrye.common.annotation.Blocking;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;

@Path("/api/messages")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
public class MessageApiResource {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @GET
    @Transactional
    public MessageListResponse list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireSupport(auth);
        List<Message> messages = Message
                .find("select distinct m from Message m left join fetch m.attachments order by m.date desc").list();
        if (!messages.isEmpty()) {
            messages = List.copyOf(new LinkedHashSet<>(messages));
        }
        return new MessageListResponse("Messages", "/messages/new", messages.stream().map(this::toListItem).toList());
    }

    @GET
    @Path("/bootstrap")
    @Transactional
    public MessageFormResponse bootstrap(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("messageId") Long messageId) {
        requireSupport(auth);
        Message message = messageId == null ? new Message() : Message.findById(messageId);
        if (messageId != null && message == null) {
            throw new NotFoundException();
        }
        String title = messageId == null ? "New message" : "Edit message";
        String submitPath = messageId == null ? "/messages" : "/messages/" + messageId;
        String cancelPath = "/messages";
        String dateValue = message.date == null ? "" : message.date.format(DATE_TIME_FORMATTER);
        return new MessageFormResponse(title, submitPath, cancelPath, messageId != null,
                Ticket.<Ticket> list("order by name").stream().map(ticket -> new TicketOption(ticket.id, ticket.name))
                        .toList(),
                new MessageFormData(message.id, message.body == null ? "" : message.body, dateValue,
                        message.ticket == null ? null : message.ticket.id),
                message.attachments == null ? List.of()
                        : message.attachments.stream().map(this::toAttachmentSummary).toList());
    }

    private MessageListItem toListItem(Message message) {
        return new MessageListItem(message.id, preview(message.body), message.body,
                message.date == null ? null : message.date.format(DATE_TIME_FORMATTER),
                message.ticket == null ? null : message.ticket.id, message.ticket == null ? null : message.ticket.name,
                message.author == null ? null : message.author.getDisplayName(),
                message.attachments == null ? 0 : message.attachments.size(), "/messages/" + message.id + "/edit");
    }

    private AttachmentSummary toAttachmentSummary(Attachment attachment) {
        return new AttachmentSummary(attachment.id, attachment.name, attachment.mimeType,
                "/attachments/" + attachment.id + "/data", "/attachments/" + attachment.id);
    }

    private String preview(String body) {
        if (body == null || body.isBlank()) {
            return "No message body";
        }
        String normalized = body.replace('\n', ' ').trim();
        return normalized.length() > 120 ? normalized.substring(0, 117) + "..." : normalized;
    }

    private User requireSupport(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isSupport(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }

    public record MessageListResponse(String title, String createPath, List<MessageListItem> items) {
    }

    public record MessageListItem(Long id, String preview, String body, String date, Long ticketId, String ticketName,
            String authorName, int attachmentCount, String editPath) {
    }

    public record MessageFormResponse(String title, String submitPath, String cancelPath, boolean edit,
            List<TicketOption> tickets, MessageFormData message, List<AttachmentSummary> attachments) {
    }

    public record TicketOption(Long id, String name) {
    }

    public record MessageFormData(Long id, String body, String date, Long ticketId) {
    }

    public record AttachmentSummary(Long id, String name, String mimeType, String downloadPath, String viewPath) {
    }
}
