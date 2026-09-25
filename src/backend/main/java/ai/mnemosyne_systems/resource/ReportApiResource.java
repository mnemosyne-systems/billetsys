/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.HistogramTicket;
import ai.mnemosyne_systems.model.PickupTimeStat;
import ai.mnemosyne_systems.model.ReportData;
import ai.mnemosyne_systems.model.TimeStat;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.service.ReportService;
import ai.mnemosyne_systems.util.AuthHelper;
import ai.mnemosyne_systems.util.CurrentUser;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Path("/api/reports")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({ "admin", "tam", "superuser" })
public class ReportApiResource {
    @Inject
    CurrentUser currentUser;

    @Inject
    ReportService reportService;

    @GET
    @Transactional
    public ReportResponse reports(@QueryParam("companyId") Long companyId, @QueryParam("period") String period) {
        User user = currentUser.get();
        String safePeriod = period == null || period.isBlank() ? "all" : period.toLowerCase();
        if (AuthHelper.isAdmin(user)) {
            List<Company> companies = Company.list(
                    "select distinct c from Company c where exists (select t from Ticket t where t.company = c) order by c.name");
            Company selectedCompany = companyId == null ? null : Company.findById(companyId);
            String scope = selectedCompany != null ? ReportService.companyScope(selectedCompany.id)
                    : ReportService.allScope();
            ReportData data = reportService.computeReport(scope, safePeriod);
            return toResponse("admin", companies, selectedCompany, true, selectedCompany == null, "/reports/export",
                    safePeriod, data);
        }
        if (User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            List<Company> companies = Company.list(
                    "select distinct c from Company c join c.users u where u = ?1 and exists (select t from Ticket t where t.company = c) order by c.name",
                    user);
            Company selectedCompany = companyId == null ? null
                    : companies.stream().filter(company -> company.id.equals(companyId)).findFirst().orElse(null);
            String scope = selectedCompany != null ? ReportService.companyScope(selectedCompany.id)
                    : ReportService.userScope(user.id);
            ReportData data = reportService.computeReport(scope, safePeriod);
            return toResponse("tam", companies, selectedCompany, true, false, "/reports/tam/export", safePeriod, data);
        }
        if (User.TYPE_SUPERUSER.equalsIgnoreCase(user.type)) {
            List<Company> companies = Company.list(
                    "select distinct c from Company c join c.users u where u = ?1 and exists (select t from Ticket t where t.company = c) order by c.name",
                    user);
            Company selectedCompany = companies.isEmpty() ? null : companies.get(0);
            String scope = selectedCompany != null ? ReportService.companyScope(selectedCompany.id)
                    : ReportService.userScope(user.id);
            ReportData data = reportService.computeReport(scope, safePeriod);
            return toResponse("superuser", companies, selectedCompany, false, false, "/reports/superuser/export",
                    safePeriod, data);
        }
        throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
    }

    private ReportResponse toResponse(String role, List<Company> companies, Company selectedCompany,
            boolean showCompanyFilter, boolean showCompanyChart, String exportPath, String period, ReportData data) {
        return new ReportResponse(role,
                companies.stream().map(company -> new CompanyOption(company.id, company.name)).toList(),
                selectedCompany == null ? null : selectedCompany.id,
                selectedCompany == null ? "All" : selectedCompany.name, showCompanyFilter, showCompanyChart, exportPath,
                period, data.totalTickets, toPoints(data.ticketsByStatus), toPoints(data.ticketsByCategory),
                toPoints(data.ticketsByCompany), toPoints(data.ticketsOverTime),
                toTimeStatPoints(data.firstResponseTimeStats), toTimeStatPoints(data.resolutionTimeStats),
                toStatPoints(data.pickupTimeStats), toHistogram(data.resolutionHistogram));
    }

    private List<MetricPoint> toPoints(Map<String, Long> values) {
        return values.entrySet().stream().map(entry -> new MetricPoint(entry.getKey(), entry.getValue())).toList();
    }

    private List<StatMetricPoint> toStatPoints(Map<String, PickupTimeStat> values) {
        return values.entrySet().stream().map(entry -> new StatMetricPoint(entry.getKey(), entry.getValue().min(),
                entry.getValue().avg(), entry.getValue().max())).toList();
    }

    private List<StatMetricPoint> toTimeStatPoints(Map<String, TimeStat> values) {
        return values.entrySet().stream().map(entry -> new StatMetricPoint(entry.getKey(), entry.getValue().min(),
                entry.getValue().avg(), entry.getValue().max())).toList();
    }

    private List<HistogramBucket> toHistogram(Map<String, List<HistogramTicket>> histogram) {
        return histogram
                .entrySet().stream().map(
                        entry -> new HistogramBucket(entry.getKey(), entry.getValue().size(),
                                entry.getValue().stream()
                                        .map(summary -> new TicketSummary(summary.id(), summary.name(),
                                                summary.status(), summary.companyName(), summary.categoryName()))
                                        .toList()))
                .toList();
    }

    public record ReportResponse(String role, List<CompanyOption> companies, Long selectedCompanyId, String companyName,
            boolean showCompanyFilter, boolean showCompanyChart, String exportPath, String period, int totalTickets,
            List<MetricPoint> status, List<MetricPoint> category, List<MetricPoint> company, List<MetricPoint> timeline,
            List<StatMetricPoint> firstResponse, List<StatMetricPoint> resolutionTime, List<StatMetricPoint> pickupTime,
            List<HistogramBucket> histogram) {
    }

    public record CompanyOption(Long id, String name) {
    }

    public record MetricPoint(String label, Long value) {
    }

    public record StatMetricPoint(String label, Double min, Double avg, Double max) {
    }

    public record HistogramBucket(String label, int count, List<TicketSummary> tickets) {
    }

    public record TicketSummary(Long id, String name, String status, String companyName, String categoryName) {
    }
}
