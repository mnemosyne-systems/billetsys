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
import ai.mnemosyne_systems.util.AuthHelper;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Path("/reports")
@Produces(MediaType.TEXT_HTML)
@Blocking
public class ReportResource {
    private static final String BUCKET_UNDER_1H = "< 1h";
    private static final String BUCKET_1_TO_8H = "1–8h";
    private static final String BUCKET_8_TO_24H = "8–24h";
    private static final String BUCKET_1_TO_7D = "1–7 days";
    private static final String BUCKET_OVER_7D = "> 7 days";

    @Inject
    PdfService pdfService;

    @GET
    public Object adminReports(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("companyId") Long companyId, @QueryParam("period") String period) {
        requireAdmin(auth);
        return Response.seeOther(URI.create("/reports" + reportQuery(companyId, period))).build();
    }

    @GET
    @Path("/tam")
    public Object tamReports(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @QueryParam("companyId") Long companyId,
            @QueryParam("period") String period) {
        requireTam(auth);
        return Response.seeOther(URI.create("/reports" + reportQuery(companyId, period))).build();
    }

    @GET
    @Path("/superuser")
    public Object superuserReports(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("companyId") Long companyId, @QueryParam("period") String period) {
        requireSuperuser(auth);
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
    public Response exportAdminReport(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("companyId") Long companyId, @QueryParam("period") String period,
            @FormParam("statusChart") String statusChart, @FormParam("categoryChart") String categoryChart,
            @FormParam("companyChart") String companyChart, @FormParam("timeChart") String timeChart,
            @FormParam("responseTimeChart") String responseTimeChart,
            @FormParam("resolutionTimeChart") String resolutionTimeChart,
            @FormParam("histogramChart") String histogramChart) {
        requireAdmin(auth);
        Company selectedCompany = companyId != null ? Company.findById(companyId) : null;
        String safePeriod = period == null || period.isBlank() ? "all" : period.toLowerCase();
        ReportData data = buildReportData(selectedCompany != null ? List.of(selectedCompany) : null, safePeriod);
        String companyName = selectedCompany == null ? "All" : selectedCompany.name;
        Map<String, String> chartImages = buildChartImages(statusChart, categoryChart, companyChart, timeChart,
                responseTimeChart, resolutionTimeChart, histogramChart);
        byte[] pdf = pdfService.generateReportPdf(data, companyName, safePeriod, selectedCompany == null, chartImages);
        String filename = "report-" + companyName.toLowerCase().replace(" ", "-") + ".pdf";
        return Response.ok(pdf).header("Content-Disposition", "attachment; filename=\"" + filename + "\"").build();
    }

    @POST
    @Path("/tam/export")
    @Produces("application/pdf")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response exportTamReport(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("companyId") Long companyId, @QueryParam("period") String period,
            @FormParam("statusChart") String statusChart, @FormParam("categoryChart") String categoryChart,
            @FormParam("companyChart") String companyChart, @FormParam("timeChart") String timeChart,
            @FormParam("responseTimeChart") String responseTimeChart,
            @FormParam("resolutionTimeChart") String resolutionTimeChart,
            @FormParam("histogramChart") String histogramChart) {
        User user = requireTam(auth);
        List<Company> tamCompanies = Company.list(
                "select distinct c from Company c join c.users u where u = ?1 and exists (select t from Ticket t where t.company = c) order by c.name",
                user);
        Company selectedCompany = null;
        if (companyId != null) {
            selectedCompany = tamCompanies.stream().filter(c -> c.id.equals(companyId)).findFirst().orElse(null);
        }
        String safePeriod = period == null || period.isBlank() ? "all" : period.toLowerCase();
        List<Company> dataFilter = selectedCompany != null ? List.of(selectedCompany) : tamCompanies;
        ReportData data = buildReportData(dataFilter, safePeriod);
        String companyName = selectedCompany != null ? selectedCompany.name : "All";
        Map<String, String> chartImages = buildChartImages(statusChart, categoryChart, null, timeChart,
                responseTimeChart, resolutionTimeChart, histogramChart);
        byte[] pdf = pdfService.generateReportPdf(data, companyName, safePeriod, false, chartImages);
        String filename = "report-" + companyName.toLowerCase().replace(" ", "-") + ".pdf";
        return Response.ok(pdf).header("Content-Disposition", "attachment; filename=\"" + filename + "\"").build();
    }

    @POST
    @Path("/superuser/export")
    @Produces("application/pdf")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response exportSuperuserReport(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("companyId") Long companyId, @QueryParam("period") String period,
            @FormParam("statusChart") String statusChart, @FormParam("categoryChart") String categoryChart,
            @FormParam("companyChart") String companyChart, @FormParam("timeChart") String timeChart,
            @FormParam("responseTimeChart") String responseTimeChart,
            @FormParam("resolutionTimeChart") String resolutionTimeChart,
            @FormParam("histogramChart") String histogramChart) {
        User user = requireSuperuser(auth);
        List<Company> superuserCompanies = Company.list(
                "select distinct c from Company c join c.users u where u = ?1 and exists (select t from Ticket t where t.company = c) order by c.name",
                user);
        Company selectedCompany = superuserCompanies.isEmpty() ? null : superuserCompanies.get(0);
        String safePeriod = period == null || period.isBlank() ? "all" : period.toLowerCase();
        List<Company> dataFilter = selectedCompany != null ? List.of(selectedCompany) : superuserCompanies;
        ReportData data = buildReportData(dataFilter, safePeriod);
        String companyName = selectedCompany != null ? selectedCompany.name : "All";
        Map<String, String> chartImages = buildChartImages(statusChart, categoryChart, null, timeChart,
                responseTimeChart, resolutionTimeChart, histogramChart);
        byte[] pdf = pdfService.generateReportPdf(data, companyName, safePeriod, false, chartImages);
        String filename = "report-" + companyName.toLowerCase().replace(" ", "-") + ".pdf";
        return Response.ok(pdf).header("Content-Disposition", "attachment; filename=\"" + filename + "\"").build();
    }

    private ReportData buildReportData(List<Company> filterCompanies, String period) {
        List<Ticket> tickets;
        if (filterCompanies != null && !filterCompanies.isEmpty()) {
            tickets = Ticket
                    .find("from Ticket t left join fetch t.category left join fetch t.company where t.company in ?1",
                            filterCompanies)
                    .list();
        } else {
            tickets = Ticket.find("from Ticket t left join fetch t.category left join fetch t.company").list();
        }

        List<Message> allMessages;
        if (filterCompanies != null && !filterCompanies.isEmpty()) {
            allMessages = Message
                    .find("from Message m left join fetch m.author where m.ticket.company in ?1 order by m.date asc",
                            filterCompanies)
                    .list();
        } else {
            allMessages = Message.find("from Message m left join fetch m.author order by m.date asc").list();
        }

        Map<Long, List<Message>> messagesByTicket = new LinkedHashMap<>();
        for (Message message : allMessages) {
            if (message.ticket != null && message.ticket.id != null) {
                messagesByTicket.computeIfAbsent(message.ticket.id, k -> new ArrayList<>()).add(message);
            }
        }

        ReportData data = new ReportData();
        data.totalTickets = tickets.size();
        data.ticketsByStatus = buildTicketsByStatus(tickets);
        data.ticketsByCategory = buildTicketsByCategory(tickets);
        data.ticketsByCompany = buildTicketsByCompany(tickets);
        data.ticketsOverTime = buildTicketsOverTime(messagesByTicket, period);
        data.avgFirstResponseTime = buildAvgFirstResponseTime(tickets, messagesByTicket);
        data.avgResolutionTime = buildAvgResolutionTime(tickets, messagesByTicket);
        data.resolutionHistogram = buildResolutionHistogram(tickets, messagesByTicket);
        return data;
    }

    private Map<String, Long> buildTicketsByStatus(List<Ticket> tickets) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Ticket ticket : tickets) {
            String status = ticket.status == null || ticket.status.isBlank() ? "Open" : ticket.status;
            result.merge(status, 1L, Long::sum);
        }
        return result;
    }

    private Map<String, Long> buildTicketsByCategory(List<Ticket> tickets) {
        Map<String, Long> unsorted = new LinkedHashMap<>();
        for (Ticket ticket : tickets) {
            String name = ticket.category != null && ticket.category.name != null ? ticket.category.name
                    : "Uncategorized";
            unsorted.merge(name, 1L, Long::sum);
        }
        Map<String, Long> result = new LinkedHashMap<>();
        unsorted.entrySet().stream().sorted(Map.Entry.<String, Long> comparingByValue().reversed())
                .forEachOrdered(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    private Map<String, Long> buildTicketsByCompany(List<Ticket> tickets) {
        Map<String, Long> unsorted = new LinkedHashMap<>();
        for (Ticket ticket : tickets) {
            String name = ticket.company != null && ticket.company.name != null ? ticket.company.name : "Unknown";
            unsorted.merge(name, 1L, Long::sum);
        }
        Map<String, Long> result = new LinkedHashMap<>();
        unsorted.entrySet().stream().sorted(Map.Entry.<String, Long> comparingByValue().reversed())
                .forEachOrdered(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    private Map<String, Long> buildTicketsOverTime(Map<Long, List<Message>> messagesByTicket, String period) {
        DateTimeFormatter format;
        java.time.LocalDateTime cutoff;

        if ("month".equals(period)) {
            format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            java.time.LocalDate firstOfMonth = java.time.LocalDate.now().withDayOfMonth(1);
            cutoff = firstOfMonth.atStartOfDay();
        } else if ("year".equals(period)) {
            format = DateTimeFormatter.ofPattern("yyyy-MM");
            java.time.LocalDate firstOfYear = java.time.LocalDate.now().withMonth(1).withDayOfMonth(1);
            cutoff = firstOfYear.atStartOfDay();
        } else {
            format = DateTimeFormatter.ofPattern("yyyy-MM");
            cutoff = null;
        }

        Map<String, Long> result = new TreeMap<>();
        for (List<Message> messages : messagesByTicket.values()) {
            if (!messages.isEmpty() && messages.get(0).date != null) {
                java.time.LocalDateTime date = messages.get(0).date;
                if (cutoff != null && date.isBefore(cutoff)) {
                    continue;
                }
                result.merge(format.format(date), 1L, Long::sum);
            }
        }
        return result;
    }

    private Map<String, Double> buildAvgFirstResponseTime(List<Ticket> tickets,
            Map<Long, List<Message>> messagesByTicket) {
        Map<String, List<Double>> hoursByCategory = new LinkedHashMap<>();
        for (Ticket ticket : tickets) {
            List<Message> messages = messagesByTicket.get(ticket.id);
            if (messages == null || messages.size() < 2) {
                continue;
            }
            Message first = messages.get(0);
            Message firstSupportReply = null;
            for (int i = 1; i < messages.size(); i++) {
                Message message = messages.get(i);
                if (message.author != null && (User.TYPE_SUPPORT.equalsIgnoreCase(message.author.type)
                        || User.TYPE_ADMIN.equalsIgnoreCase(message.author.type))) {
                    firstSupportReply = message;
                    break;
                }
            }
            if (firstSupportReply == null || first.date == null || firstSupportReply.date == null) {
                continue;
            }
            double hours = Duration.between(first.date, firstSupportReply.date).toMinutes() / 60.0;
            if (hours < 0) {
                hours = 0;
            }
            String category = ticket.category != null && ticket.category.name != null ? ticket.category.name
                    : "Uncategorized";
            hoursByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(hours);
        }
        Map<String, Double> unsorted = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : hoursByCategory.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            unsorted.put(entry.getKey(), Math.round(avg * 10.0) / 10.0);
        }
        Map<String, Double> result = new LinkedHashMap<>();
        unsorted.entrySet().stream().sorted(Map.Entry.<String, Double> comparingByValue().reversed())
                .forEachOrdered(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    private Map<String, Double> buildAvgResolutionTime(List<Ticket> tickets,
            Map<Long, List<Message>> messagesByTicket) {
        Map<String, List<Double>> hoursByCategory = new LinkedHashMap<>();
        for (Ticket ticket : tickets) {
            if (!"Closed".equalsIgnoreCase(ticket.status)) {
                continue;
            }
            List<Message> messages = messagesByTicket.get(ticket.id);
            if (messages == null || messages.isEmpty()) {
                continue;
            }
            Message first = messages.get(0);
            Message last = messages.get(messages.size() - 1);
            if (first.date == null || last.date == null) {
                continue;
            }
            double hours = Duration.between(first.date, last.date).toMinutes() / 60.0;
            if (hours < 0) {
                hours = 0;
            }
            String category = ticket.category != null && ticket.category.name != null ? ticket.category.name
                    : "Uncategorized";
            hoursByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(hours);
        }
        Map<String, Double> unsorted = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : hoursByCategory.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            unsorted.put(entry.getKey(), Math.round(avg * 10.0) / 10.0);
        }
        Map<String, Double> result = new LinkedHashMap<>();
        unsorted.entrySet().stream().sorted(Map.Entry.<String, Double> comparingByValue().reversed())
                .forEachOrdered(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    private Map<String, List<Ticket>> buildResolutionHistogram(List<Ticket> tickets,
            Map<Long, List<Message>> messagesByTicket) {
        Map<String, List<Ticket>> histogram = new LinkedHashMap<>();
        histogram.put(BUCKET_UNDER_1H, new ArrayList<>());
        histogram.put(BUCKET_1_TO_8H, new ArrayList<>());
        histogram.put(BUCKET_8_TO_24H, new ArrayList<>());
        histogram.put(BUCKET_1_TO_7D, new ArrayList<>());
        histogram.put(BUCKET_OVER_7D, new ArrayList<>());

        for (Ticket ticket : tickets) {
            if (!"Closed".equalsIgnoreCase(ticket.status)) {
                continue;
            }
            List<Message> messages = messagesByTicket.get(ticket.id);
            if (messages == null || messages.isEmpty()) {
                continue;
            }
            Message first = messages.get(0);
            Message last = messages.get(messages.size() - 1);
            if (first.date == null || last.date == null) {
                continue;
            }
            double hours = Duration.between(first.date, last.date).toMinutes() / 60.0;
            if (hours < 1) {
                histogram.get(BUCKET_UNDER_1H).add(ticket);
            } else if (hours < 8) {
                histogram.get(BUCKET_1_TO_8H).add(ticket);
            } else if (hours < 24) {
                histogram.get(BUCKET_8_TO_24H).add(ticket);
            } else if (hours < 168) {
                histogram.get(BUCKET_1_TO_7D).add(ticket);
            } else {
                histogram.get(BUCKET_OVER_7D).add(ticket);
            }
        }
        return histogram;
    }

    private Map<String, String> buildChartImages(String statusChart, String categoryChart, String companyChart,
            String timeChart, String responseTimeChart, String resolutionTimeChart, String histogramChart) {
        Map<String, String> images = new LinkedHashMap<>();
        images.put("statusChart", statusChart);
        images.put("categoryChart", categoryChart);
        images.put("companyChart", companyChart);
        images.put("timeChart", timeChart);
        images.put("responseTimeChart", responseTimeChart);
        images.put("resolutionTimeChart", resolutionTimeChart);
        images.put("histogramChart", histogramChart);
        return images;
    }

    private User requireAdmin(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isAdmin(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }

    private User requireTam(String auth) {
        User user = AuthHelper.findUser(auth);
        if (user == null || !User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }

    private User requireSuperuser(String auth) {
        User user = AuthHelper.findUser(auth);
        if (user == null || !User.TYPE_SUPERUSER.equalsIgnoreCase(user.type)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }
}
