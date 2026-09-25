/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.*;
import ai.mnemosyne_systems.service.PdfService;
import ai.mnemosyne_systems.service.ReportService;
import ai.mnemosyne_systems.util.CurrentUser;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/reports")
@Produces(MediaType.TEXT_HTML)
@Blocking
@RolesAllowed({ "admin", "tam", "superuser" })
public class ReportResource {
    @Inject
    PdfService pdfService;

    @Inject
    ReportService reportService;

    @Inject
    CurrentUser currentUser;

    @GET
    @RolesAllowed("admin")
    public Object adminReports(@QueryParam("companyId") Long companyId, @QueryParam("period") String period) {
        return Response.seeOther(URI.create("/reports" + reportQuery(companyId, period))).build();
    }

    @GET
    @Path("/tam")
    @RolesAllowed("tam")
    public Object tamReports(@QueryParam("companyId") Long companyId, @QueryParam("period") String period) {
        return Response.seeOther(URI.create("/reports" + reportQuery(companyId, period))).build();
    }

    @GET
    @Path("/superuser")
    @RolesAllowed("superuser")
    public Object superuserReports(@QueryParam("companyId") Long companyId, @QueryParam("period") String period) {
        return Response.seeOther(URI.create("/reports" + reportQuery(companyId, period))).build();
    }

    private String reportQuery(Long companyId, String period) {
        StringBuilder query = new StringBuilder();
        if (companyId != null) {
            query.append(query.length() == 0 ? "?" : "&").append("companyId=").append(companyId);
        }
        if (period != null && !period.isBlank()) {
            query.append(query.length() == 0 ? "?" : "&").append("period=").append(period);
        }
        return query.toString();
    }

    @POST
    @Path("/export")
    @Produces("application/pdf")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed("admin")
    public Response exportAdminReport(@QueryParam("companyId") Long companyId, @QueryParam("period") String period,
            @FormParam("statusChart") String statusChart, @FormParam("categoryChart") String categoryChart,
            @FormParam("companyChart") String companyChart, @FormParam("timeChart") String timeChart,
            @FormParam("responseTimeChart") String responseTimeChart,
            @FormParam("resolutionTimeChart") String resolutionTimeChart,
            @FormParam("pickupTimeChart") String pickupTimeChart, @FormParam("histogramChart") String histogramChart) {
        Company selectedCompany = companyId != null ? Company.findById(companyId) : null;
        String safePeriod = period == null || period.isBlank() ? "all" : period.toLowerCase();
        String scope = selectedCompany != null ? ReportService.companyScope(selectedCompany.id)
                : ReportService.allScope();
        ReportData data = reportService.computeReport(scope, safePeriod);
        String companyName = selectedCompany == null ? "All" : selectedCompany.name;
        Map<String, String> chartImages = buildChartImages(statusChart, categoryChart, companyChart, timeChart,
                responseTimeChart, resolutionTimeChart, pickupTimeChart, histogramChart);
        byte[] pdf = pdfService.generateReportPdf(data, companyName, safePeriod, selectedCompany == null, chartImages);
        String filename = "report-" + companyName.toLowerCase().replace(" ", "-") + ".pdf";
        return Response.ok(pdf).header("Content-Disposition", "attachment; filename=\"" + filename + "\"").build();
    }

    @POST
    @Path("/tam/export")
    @Produces("application/pdf")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed("tam")
    public Response exportTamReport(@QueryParam("companyId") Long companyId, @QueryParam("period") String period,
            @FormParam("statusChart") String statusChart, @FormParam("categoryChart") String categoryChart,
            @FormParam("companyChart") String companyChart, @FormParam("timeChart") String timeChart,
            @FormParam("responseTimeChart") String responseTimeChart,
            @FormParam("resolutionTimeChart") String resolutionTimeChart,
            @FormParam("pickupTimeChart") String pickupTimeChart, @FormParam("histogramChart") String histogramChart) {
        User user = currentUser.get();
        List<Company> tamCompanies = Company.list(
                "select distinct c from Company c join c.users u where u = ?1 and exists (select t from Ticket t where t.company = c) order by c.name",
                user);
        Company selectedCompany = null;
        if (companyId != null) {
            selectedCompany = tamCompanies.stream().filter(c -> c.id.equals(companyId)).findFirst().orElse(null);
        }
        String safePeriod = period == null || period.isBlank() ? "all" : period.toLowerCase();
        String scope = selectedCompany != null ? ReportService.companyScope(selectedCompany.id)
                : ReportService.userScope(user.id);
        ReportData data = reportService.computeReport(scope, safePeriod);
        String companyName = selectedCompany != null ? selectedCompany.name : "All";
        Map<String, String> chartImages = buildChartImages(statusChart, categoryChart, null, timeChart,
                responseTimeChart, resolutionTimeChart, pickupTimeChart, histogramChart);
        byte[] pdf = pdfService.generateReportPdf(data, companyName, safePeriod, false, chartImages);
        String filename = "report-" + companyName.toLowerCase().replace(" ", "-") + ".pdf";
        return Response.ok(pdf).header("Content-Disposition", "attachment; filename=\"" + filename + "\"").build();
    }

    @POST
    @Path("/superuser/export")
    @Produces("application/pdf")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed("superuser")
    public Response exportSuperuserReport(@QueryParam("companyId") Long companyId, @QueryParam("period") String period,
            @FormParam("statusChart") String statusChart, @FormParam("categoryChart") String categoryChart,
            @FormParam("companyChart") String companyChart, @FormParam("timeChart") String timeChart,
            @FormParam("responseTimeChart") String responseTimeChart,
            @FormParam("resolutionTimeChart") String resolutionTimeChart,
            @FormParam("pickupTimeChart") String pickupTimeChart, @FormParam("histogramChart") String histogramChart) {
        User user = currentUser.get();
        List<Company> superuserCompanies = Company.list(
                "select distinct c from Company c join c.users u where u = ?1 and exists (select t from Ticket t where t.company = c) order by c.name",
                user);
        Company selectedCompany = superuserCompanies.isEmpty() ? null : superuserCompanies.get(0);
        String safePeriod = period == null || period.isBlank() ? "all" : period.toLowerCase();
        String scope = selectedCompany != null ? ReportService.companyScope(selectedCompany.id)
                : ReportService.userScope(user.id);
        ReportData data = reportService.computeReport(scope, safePeriod);
        String companyName = selectedCompany != null ? selectedCompany.name : "All";
        Map<String, String> chartImages = buildChartImages(statusChart, categoryChart, null, timeChart,
                responseTimeChart, resolutionTimeChart, pickupTimeChart, histogramChart);
        byte[] pdf = pdfService.generateReportPdf(data, companyName, safePeriod, false, chartImages);
        String filename = "report-" + companyName.toLowerCase().replace(" ", "-") + ".pdf";
        return Response.ok(pdf).header("Content-Disposition", "attachment; filename=\"" + filename + "\"").build();
    }

    private Map<String, String> buildChartImages(String statusChart, String categoryChart, String companyChart,
            String timeChart, String responseTimeChart, String resolutionTimeChart, String pickupTimeChart,
            String histogramChart) {
        Map<String, String> images = new LinkedHashMap<>();
        images.put("statusChart", statusChart);
        images.put("categoryChart", categoryChart);
        images.put("companyChart", companyChart);
        images.put("timeChart", timeChart);
        images.put("responseTimeChart", responseTimeChart);
        images.put("resolutionTimeChart", resolutionTimeChart);
        images.put("pickupTimeChart", pickupTimeChart);
        images.put("histogramChart", histogramChart);
        return images;
    }

}
