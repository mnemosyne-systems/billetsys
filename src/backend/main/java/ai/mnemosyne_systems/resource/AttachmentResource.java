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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URLEncoder;

@Path("/attachments")
@Produces(MediaType.TEXT_HTML)
@Blocking
@RolesAllowed({ "admin", "support", "superuser", "tam", "user" })
public class AttachmentResource {

    @Inject
    CurrentUser currentUser;

    @GET
    @Path("/{id}/data")
    @Produces("*/*")
    public Response data(@PathParam("id") Long id) {
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
        String encoded = URLEncoder.encode(attachment.name, java.nio.charset.StandardCharsets.UTF_8).replace("+",
                "%20");
        return Response.ok(attachment.data, attachment.mimeType)
                .header("Content-Disposition", "inline; filename*=UTF-8''" + encoded).build();
    }

    @GET
    @Path("/{id}")
    public Response view(@PathParam("id") Long id) {
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
        return Response.seeOther(URI.create("/attachments/" + id)).build();
    }
}
