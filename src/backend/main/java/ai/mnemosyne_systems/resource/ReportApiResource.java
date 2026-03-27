package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.ReportData;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
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

@Path("/api/reports")
@Produces(MediaType.APPLICATION_JSON)
public class ReportApiResource {
    private static final String BUCKET_UNDER_1H = "< 1h";
    private static final String BUCKET_1_TO_8H = "1–8h";
    private static final String BUCKET_8_TO_24H = "8–24h";
    private static final String BUCKET_1_TO_7D = "1–7 days";
    private static final String BUCKET_OVER_7D = "> 7 days";

    @GET
    @Transactional
    public ReportResponse reports(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("companyId") Long companyId, @QueryParam("period") String period) {
        User user = requireReporter(auth);
        String safePeriod = period == null || period.isBlank() ? "all" : period.toLowerCase();
        if (AuthHelper.isAdmin(user)) {
            List<Company> companies = Company.list(
                    "select distinct c from Company c where exists (select t from Ticket t where t.company = c) order by c.name");
            Company selectedCompany = companyId == null ? null : Company.findById(companyId);
            ReportData data = buildReportData(selectedCompany != null ? List.of(selectedCompany) : null, safePeriod);
            return toResponse("admin", companies, selectedCompany, true, selectedCompany == null, "/reports/export",
                    safePeriod, data);
        }
        if (User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            List<Company> companies = Company.list(
                    "select distinct c from Company c join c.users u where u = ?1 and exists (select t from Ticket t where t.company = c) order by c.name",
                    user);
            Company selectedCompany = companyId == null ? null
                    : companies.stream().filter(company -> company.id.equals(companyId)).findFirst().orElse(null);
            ReportData data = buildReportData(selectedCompany != null ? List.of(selectedCompany) : companies,
                    safePeriod);
            return toResponse("tam", companies, selectedCompany, true, false, "/reports/tam/export", safePeriod, data);
        }
        if (User.TYPE_SUPERUSER.equalsIgnoreCase(user.type)) {
            List<Company> companies = Company.list(
                    "select distinct c from Company c join c.users u where u = ?1 and exists (select t from Ticket t where t.company = c) order by c.name",
                    user);
            Company selectedCompany = companyId == null ? null
                    : companies.stream().filter(company -> company.id.equals(companyId)).findFirst().orElse(null);
            ReportData data = buildReportData(selectedCompany != null ? List.of(selectedCompany) : companies,
                    safePeriod);
            return toResponse("superuser", companies, selectedCompany, true, false, "/reports/superuser/export",
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
                toDoublePoints(data.avgFirstResponseTime), toDoublePoints(data.avgResolutionTime),
                toHistogram(data.resolutionHistogram));
    }

    private List<MetricPoint> toPoints(Map<String, Long> values) {
        return values.entrySet().stream().map(entry -> new MetricPoint(entry.getKey(), entry.getValue())).toList();
    }

    private List<DoubleMetricPoint> toDoublePoints(Map<String, Double> values) {
        return values.entrySet().stream().map(entry -> new DoubleMetricPoint(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<HistogramBucket> toHistogram(Map<String, List<Ticket>> histogram) {
        return histogram.entrySet().stream()
                .map(entry -> new HistogramBucket(entry.getKey(), entry.getValue().size(),
                        entry.getValue().stream()
                                .map(ticket -> new TicketSummary(ticket.id, ticket.name, ticket.status,
                                        ticket.company == null ? null : ticket.company.name,
                                        ticket.category == null ? null : ticket.category.name))
                                .toList()))
                .toList();
    }

    private User requireReporter(String auth) {
        User user = AuthHelper.findUser(auth);
        if (user == null) {
            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        if (AuthHelper.isAdmin(user) || User.TYPE_TAM.equalsIgnoreCase(user.type)
                || User.TYPE_SUPERUSER.equalsIgnoreCase(user.type)) {
            return user;
        }
        throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).location(URI.create("/")).build());
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
                messagesByTicket.computeIfAbsent(message.ticket.id, ignored -> new ArrayList<>()).add(message);
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
                .forEachOrdered(entry -> result.put(entry.getKey(), entry.getValue()));
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
                .forEachOrdered(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    private Map<String, Long> buildTicketsOverTime(Map<Long, List<Message>> messagesByTicket, String period) {
        DateTimeFormatter format;
        java.time.LocalDateTime cutoff;
        if ("month".equals(period)) {
            format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            cutoff = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();
        } else if ("year".equals(period)) {
            format = DateTimeFormatter.ofPattern("yyyy-MM");
            cutoff = java.time.LocalDate.now().withMonth(1).withDayOfMonth(1).atStartOfDay();
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
            for (int index = 1; index < messages.size(); index++) {
                Message message = messages.get(index);
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
            String category = ticket.category != null && ticket.category.name != null ? ticket.category.name
                    : "Uncategorized";
            hoursByCategory.computeIfAbsent(category, ignored -> new ArrayList<>()).add(Math.max(hours, 0));
        }
        Map<String, Double> unsorted = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : hoursByCategory.entrySet()) {
            double average = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            unsorted.put(entry.getKey(), Math.round(average * 10.0) / 10.0);
        }
        Map<String, Double> result = new LinkedHashMap<>();
        unsorted.entrySet().stream().sorted(Map.Entry.<String, Double> comparingByValue().reversed())
                .forEachOrdered(entry -> result.put(entry.getKey(), entry.getValue()));
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
            String category = ticket.category != null && ticket.category.name != null ? ticket.category.name
                    : "Uncategorized";
            hoursByCategory.computeIfAbsent(category, ignored -> new ArrayList<>()).add(Math.max(hours, 0));
        }
        Map<String, Double> unsorted = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : hoursByCategory.entrySet()) {
            double average = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            unsorted.put(entry.getKey(), Math.round(average * 10.0) / 10.0);
        }
        Map<String, Double> result = new LinkedHashMap<>();
        unsorted.entrySet().stream().sorted(Map.Entry.<String, Double> comparingByValue().reversed())
                .forEachOrdered(entry -> result.put(entry.getKey(), entry.getValue()));
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

    public record ReportResponse(String role, List<CompanyOption> companies, Long selectedCompanyId, String companyName,
            boolean showCompanyFilter, boolean showCompanyChart, String exportPath, String period, int totalTickets,
            List<MetricPoint> status, List<MetricPoint> category, List<MetricPoint> company, List<MetricPoint> timeline,
            List<DoubleMetricPoint> firstResponse, List<DoubleMetricPoint> resolutionTime,
            List<HistogramBucket> histogram) {
    }

    public record CompanyOption(Long id, String name) {
    }

    public record MetricPoint(String label, Long value) {
    }

    public record DoubleMetricPoint(String label, Double value) {
    }

    public record HistogramBucket(String label, int count, List<TicketSummary> tickets) {
    }

    public record TicketSummary(Long id, String name, String status, String companyName, String categoryName) {
    }
}
