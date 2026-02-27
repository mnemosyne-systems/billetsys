/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import io.quarkus.qute.Location;
import io.quarkus.qute.RawString;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Path("/reports")
@Produces(MediaType.TEXT_HTML)
@Blocking
public class ReportResource {

    @Location("report/reports.html")
    Template reportsTemplate;

    @GET
    public TemplateInstance adminReports(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("companyId") Long companyId, @QueryParam("period") String period) {
        User user = requireAdmin(auth);
        List<Company> companies = Company.list(
                "select distinct c from Company c where exists (select t from Ticket t where t.company = c) order by c.name");
        Company selectedCompany = null;
        if (companyId != null) {
            selectedCompany = Company.findById(companyId);
        }
        String safePeriod = period == null || period.isBlank() ? "all" : period.toLowerCase();
        ReportData data = buildReportData(selectedCompany != null ? List.of(selectedCompany) : null, safePeriod);
        return reportsTemplate.data("currentUser", user).data("companies", companies)
                .data("selectedCompanyId", companyId).data("showCompanyFilter", true).data("useAdminLayout", true)
                .data("companyName", selectedCompany == null ? "All" : selectedCompany.name)
                .data("statusLabels", raw(toJsonStringArray(data.ticketsByStatus.keySet())))
                .data("statusData", raw(toJsonNumberArray(data.ticketsByStatus.values())))
                .data("categoryLabels", raw(toJsonStringArray(data.ticketsByCategory.keySet())))
                .data("categoryData", raw(toJsonNumberArray(data.ticketsByCategory.values())))
                .data("companyLabels", raw(toJsonStringArray(data.ticketsByCompany.keySet())))
                .data("companyData", raw(toJsonNumberArray(data.ticketsByCompany.values())))
                .data("timeLabels", raw(toJsonStringArray(data.ticketsOverTime.keySet())))
                .data("timeData", raw(toJsonNumberArray(data.ticketsOverTime.values())))
                .data("responseTimeLabels", raw(toJsonStringArray(data.avgFirstResponseTime.keySet())))
                .data("responseTimeData", raw(toJsonDoubleArray(data.avgFirstResponseTime.values())))
                .data("resolutionTimeLabels", raw(toJsonStringArray(data.avgResolutionTime.keySet())))
                .data("resolutionTimeData", raw(toJsonDoubleArray(data.avgResolutionTime.values())))
                .data("totalTickets", data.totalTickets).data("period", safePeriod)
                .data("showCompanyChart", selectedCompany == null);
    }

    @GET
    @Path("/tam")
    public TemplateInstance tamReports(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("companyId") Long companyId, @QueryParam("period") String period) {
        User user = requireTam(auth);
        List<Company> tamCompanies = Company.list(
                "select distinct c from Company c join c.users u where u = ?1 and exists (select t from Ticket t where t.company = c) order by c.name",
                user);
        if (tamCompanies.isEmpty()) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }

        Company selectedCompany = null;
        if (companyId != null) {
            selectedCompany = tamCompanies.stream().filter(c -> c.id.equals(companyId)).findFirst().orElse(null);
        }

        String safePeriod = period == null || period.isBlank() ? "all" : period.toLowerCase();
        List<Company> dataFilter = selectedCompany != null ? List.of(selectedCompany) : tamCompanies;
        ReportData data = buildReportData(dataFilter, safePeriod);

        boolean showFilter = true;
        boolean showCompanyChart = false;
        String companyName = selectedCompany != null ? selectedCompany.name : "All";

        return reportsTemplate.data("currentUser", user).data("companies", tamCompanies)
                .data("selectedCompanyId", companyId).data("showCompanyFilter", showFilter)
                .data("useAdminLayout", false).data("companyName", companyName)
                .data("statusLabels", raw(toJsonStringArray(data.ticketsByStatus.keySet())))
                .data("statusData", raw(toJsonNumberArray(data.ticketsByStatus.values())))
                .data("categoryLabels", raw(toJsonStringArray(data.ticketsByCategory.keySet())))
                .data("categoryData", raw(toJsonNumberArray(data.ticketsByCategory.values())))
                .data("companyLabels", raw(toJsonStringArray(data.ticketsByCompany.keySet())))
                .data("companyData", raw(toJsonNumberArray(data.ticketsByCompany.values())))
                .data("timeLabels", raw(toJsonStringArray(data.ticketsOverTime.keySet())))
                .data("timeData", raw(toJsonNumberArray(data.ticketsOverTime.values())))
                .data("responseTimeLabels", raw(toJsonStringArray(data.avgFirstResponseTime.keySet())))
                .data("responseTimeData", raw(toJsonDoubleArray(data.avgFirstResponseTime.values())))
                .data("resolutionTimeLabels", raw(toJsonStringArray(data.avgResolutionTime.keySet())))
                .data("resolutionTimeData", raw(toJsonDoubleArray(data.avgResolutionTime.values())))
                .data("totalTickets", data.totalTickets).data("period", safePeriod)
                .data("showCompanyChart", showCompanyChart);
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

    private RawString raw(String value) {
        return new RawString(value);
    }

    private String toJsonStringArray(Collection<String> values) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String v : values) {
            if (!first) {
                sb.append(",");
            }
            String safe = v == null ? "" : v;
            safe = safe.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
                    .replace("</", "<\\/");
            sb.append("\"").append(safe).append("\"");
            first = false;
        }
        return sb.append("]").toString();
    }

    private String toJsonNumberArray(Collection<Long> values) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Long v : values) {
            if (!first) {
                sb.append(",");
            }
            sb.append(v);
            first = false;
        }
        return sb.append("]").toString();
    }

    private String toJsonDoubleArray(Collection<Double> values) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Double v : values) {
            if (!first) {
                sb.append(",");
            }
            sb.append(v);
            first = false;
        }
        return sb.append("]").toString();
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

    private static class ReportData {
        int totalTickets;
        Map<String, Long> ticketsByStatus;
        Map<String, Long> ticketsByCategory;
        Map<String, Long> ticketsByCompany;
        Map<String, Long> ticketsOverTime;
        Map<String, Double> avgFirstResponseTime;
        Map<String, Double> avgResolutionTime;
    }
}
