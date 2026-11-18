/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.service.BugzillaTicketImportSource;
import ai.mnemosyne_systems.service.CsvTicketImportSource;
import ai.mnemosyne_systems.service.TicketBootstrapService;
import ai.mnemosyne_systems.service.TicketImportLimits;
import ai.mnemosyne_systems.service.TicketImportService;
import ai.mnemosyne_systems.util.AttachmentHelper;
import ai.mnemosyne_systems.util.CurrentUser;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/api/ticket-imports")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
@RolesAllowed({ "admin", "support" })
public class TicketImportApiResource {

    private static final String DEFAULT_TIMEZONE = "UTC";

    @Inject
    CurrentUser currentUser;

    @Inject
    TicketImportService ticketImportService;

    @Inject
    TicketBootstrapService ticketBootstrapService;

    @GET
    @Path("/bootstrap")
    @Transactional
    public TicketImportBootstrapResponse bootstrap(@QueryParam("companyId") Long companyId) {
        List<TicketImportFormat> formats = List.of(
                new TicketImportFormat("csv", "CSV", "/api/ticket-imports/csv", CsvTicketImportSource.REQUIRED_COLUMNS,
                        CsvTicketImportSource.OPTIONAL_COLUMNS),
                new TicketImportFormat("bugzilla", "Bugzilla", "/api/ticket-imports/bugzilla",
                        BugzillaTicketImportSource.REQUIRED_COLUMNS, BugzillaTicketImportSource.OPTIONAL_COLUMNS));
        List<TicketImportCompany> companies = ticketBootstrapService.allCompanyOptions().stream()
                .map(company -> new TicketImportCompany(company.id(), company.name())).toList();
        List<TicketImportEntitlement> entitlements = companyId == null ? List.of()
                : ticketBootstrapService.orderedEntitlementOptionsForCompany(companyId).stream()
                        .map(entitlement -> new TicketImportEntitlement(entitlement.id(),
                                entitlementLabel(entitlement.entitlementName(), entitlement.supportLevelName())))
                        .toList();
        return new TicketImportBootstrapResponse(formats, companies, entitlements, timezoneOptions(), DEFAULT_TIMEZONE);
    }

    @POST
    @Path("/csv")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public TicketImportService.TicketImportSummary importCsv(MultipartFormDataInput input) {
        User actor = currentUser.get();
        AttachmentHelper.UploadedFile file = AttachmentHelper.readFile(input, "file",
                CsvTicketImportSource.MAX_CSV_BYTES);
        if (file == null || file.data() == null || file.data().length == 0) {
            throw new WebApplicationException("CSV file is required", Response.Status.BAD_REQUEST);
        }
        try {
            return ticketImportService.importCsv(file.data(), file.fileName(), actor);
        } catch (BadRequestException ex) {
            throw badRequest(ex);
        }
    }

    @POST
    @Path("/bugzilla")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public TicketImportService.TicketImportSummary importBugzilla(MultipartFormDataInput input) {
        User actor = currentUser.get();
        AttachmentHelper.UploadedFile file = AttachmentHelper.readFile(input, "file",
                TicketImportLimits.MAX_IMPORT_BYTES);
        if (file == null || file.data() == null || file.data().length == 0) {
            throw new WebApplicationException("Bugzilla CSV file is required", Response.Status.BAD_REQUEST);
        }
        Long companyId = AttachmentHelper.readFormLong(input, "companyId");
        Long companyEntitlementId = AttachmentHelper.readFormLong(input, "companyEntitlementId");
        String timezone = AttachmentHelper.readFormValue(input, "timezone");
        String baseUrl = AttachmentHelper.readFormValue(input, "baseUrl");
        try {
            return ticketImportService.importBugzilla(file.data(), file.fileName(), actor, companyId,
                    companyEntitlementId, timezone, baseUrl);
        } catch (BadRequestException ex) {
            throw badRequest(ex);
        }
    }

    private static WebApplicationException badRequest(BadRequestException ex) {
        return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN)
                .entity(ex.getMessage()).build());
    }

    private static List<String> timezoneOptions() {
        List<String> timezones = new ArrayList<>(ZoneId.getAvailableZoneIds());
        timezones.sort(Comparator.naturalOrder());
        timezones.remove(DEFAULT_TIMEZONE);
        timezones.add(0, DEFAULT_TIMEZONE);
        return timezones;
    }

    private static String entitlementLabel(String entitlementName, String supportLevelName) {
        String resolvedEntitlementName = entitlementName == null ? "Unknown entitlement" : entitlementName;
        String resolvedLevelName = supportLevelName == null ? "No level" : supportLevelName;
        return resolvedEntitlementName + " • " + resolvedLevelName;
    }

    public record TicketImportBootstrapResponse(List<TicketImportFormat> formats, List<TicketImportCompany> companies,
            List<TicketImportEntitlement> entitlements, List<String> timezones, String defaultTimezone) {
    }

    public record TicketImportCompany(Long id, String name) {
    }

    public record TicketImportEntitlement(Long id, String name) {
    }

    public record TicketImportFormat(String id, String label, String submitPath, List<String> requiredColumns,
            List<String> optionalColumns) {
    }
}
