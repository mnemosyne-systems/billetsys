/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Attachment;
import ai.mnemosyne_systems.service.IncomingEmailService;
import ai.mnemosyne_systems.util.AttachmentHelper;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/mail/incoming")
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Produces(MediaType.TEXT_PLAIN)
@Blocking
public class IncomingEmailResource {

    @Inject
    IncomingEmailService incomingEmailService;

    @ConfigProperty(name = "ticket.mail.incoming.enabled", defaultValue = "true")
    boolean incomingEmailEnabled;

    @POST
    public Response receive(MultipartFormDataInput input) {
        if (!incomingEmailEnabled) {
            throw new NotFoundException();
        }
        String from = AttachmentHelper.readFormValue(input, "from");
        String subject = AttachmentHelper.readFormValue(input, "subject");
        String body = AttachmentHelper.readFormValue(input, "body");
        if (body == null || body.isBlank()) {
            throw new BadRequestException("Body is required");
        }
        List<Attachment> attachments = AttachmentHelper.readAttachments(input, "attachments");
        IncomingEmailService.IncomingEmailResult result = incomingEmailService.processIncomingEmail(from, subject, body,
                attachments);
        if (!result.processed()) {
            return Response.accepted().build();
        }
        return Response.ok(result.ticketName()).build();
    }
}
