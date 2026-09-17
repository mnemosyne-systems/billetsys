/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Attachment;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import ai.mnemosyne_systems.util.CurrentUser;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Path("/api/attachments")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
@RolesAllowed({ "admin", "support", "superuser", "tam", "user" })
public class AttachmentApiResource {

    @Inject
    CurrentUser currentUser;

    @GET
    @Path("/{id}")
    @Transactional
    public AttachmentResponse view(@PathParam("id") Long id) {
        User user = currentUser.get();

        Attachment attachment = Attachment.findById(id);
        if (attachment == null) {
            throw new NotFoundException();
        }
        Message message = attachment.message;
        Ticket ticket = message == null ? null : message.ticket;
        if (!MessageVisibilitySupport.canAccessTicket(user, ticket)
                || !MessageVisibilitySupport.canViewMessage(user, message)) {
            throw new NotFoundException();
        }
        return new AttachmentResponse(attachment.id, attachment.name, attachment.mimeType, attachment.sizeLabel(),
                attachment.isImage(), "/attachments/" + attachment.id + "/data", ticket == null ? null : ticket.id,
                ticket == null ? null : ticket.name, resolveBackPath(user, ticket), textLines(attachment),
                message == null ? null : message.body);
    }

    private List<AttachmentLine> textLines(Attachment attachment) {
        if (attachment == null || attachment.isImage()) {
            return List.of();
        }
        String text = attachment.data == null ? "" : new String(attachment.data, StandardCharsets.UTF_8);
        String[] split = text.split("\\R", -1);
        List<AttachmentLine> lines = new ArrayList<>();
        for (int i = 0; i < split.length; i++) {
            lines.add(new AttachmentLine(i + 1, split[i]));
        }
        return lines;
    }

    private String resolveBackPath(User user, Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        if (AuthHelper.isSupport(user)) {
            return "/support/tickets/" + ticket.id;
        }
        if (AuthHelper.isSuperuser(user)) {
            return "/superuser/tickets/" + ticket.id;
        }
        if (AuthHelper.isUser(user)) {
            return "/user/tickets/" + ticket.id;
        }
        return null;
    }

    public record AttachmentResponse(Long id, String name, String mimeType, String sizeLabel, boolean image,
            String downloadPath, Long ticketId, String ticketName, String backPath, List<AttachmentLine> lines,
            String messageBody) {
    }

    public record AttachmentLine(int number, String content) {
    }
}
