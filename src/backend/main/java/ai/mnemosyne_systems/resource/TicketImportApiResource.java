/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.service.CsvTicketImportSource;
import ai.mnemosyne_systems.service.TicketImportService;
import ai.mnemosyne_systems.util.AttachmentHelper;
import ai.mnemosyne_systems.util.AuthHelper;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/api/ticket-imports")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
public class TicketImportApiResource {

    @Inject
    TicketImportService ticketImportService;

    @GET
    @Path("/bootstrap")
    public TicketImportBootstrapResponse bootstrap(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireImportAccess(auth);
        return new TicketImportBootstrapResponse(List.of(new TicketImportFormat("csv", "CSV",
                CsvTicketImportSource.REQUIRED_COLUMNS, CsvTicketImportSource.OPTIONAL_COLUMNS)),
                "/api/ticket-imports/csv");
    }

    @POST
    @Path("/csv")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public TicketImportService.TicketImportSummary importCsv(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            MultipartFormDataInput input) {
        User actor = requireImportAccess(auth);
        AttachmentHelper.UploadedFile file = AttachmentHelper.readFile(input, "file",
                CsvTicketImportSource.MAX_CSV_BYTES);
        if (file == null || file.data() == null || file.data().length == 0) {
            throw new WebApplicationException("CSV file is required", Response.Status.BAD_REQUEST);
        }
        try {
            return ticketImportService.importCsv(file.data(), file.fileName(), actor);
        } catch (BadRequestException ex) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN)
                    .entity(ex.getMessage()).build());
        }
    }

    private User requireImportAccess(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isSupport(user) && !AuthHelper.isAdmin(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }

    public record TicketImportBootstrapResponse(List<TicketImportFormat> formats, String submitPath) {
    }

    public record TicketImportFormat(String id, String label, List<String> requiredColumns,
            List<String> optionalColumns) {
    }
}
