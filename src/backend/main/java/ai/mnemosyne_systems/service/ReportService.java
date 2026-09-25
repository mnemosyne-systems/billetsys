/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.HistogramTicket;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.PickupTimeStat;
import ai.mnemosyne_systems.model.ReportData;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.TimeStat;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.event.Event;
import ai.mnemosyne_systems.model.event.EventConstants;
import ai.mnemosyne_systems.util.TicketTimeSupport;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Shared report aggregation, previously duplicated between {@code ReportApiResource} and {@code ReportResource}.
 * <p>
 * Owns the expensive part of reporting (full ticket/message/event scans plus in-Java aggregation). Callers keep role
 * scoping and response mapping; this bean only computes {@link ReportData} for an already-resolved company filter.
 * Separated onto its own bean so a cache can later be applied to {@link #buildReportData} without restructuring callers
 * again.
 */
@ApplicationScoped
public class ReportService {

    private static final String BUCKET_UNDER_1H = "< 1h";
    private static final String BUCKET_1_TO_8H = "1–8h";
    private static final String BUCKET_8_TO_24H = "8–24h";
    private static final String BUCKET_1_TO_7D = "1–7 days";
    private static final String BUCKET_OVER_7D = "> 7 days";

    public static String allScope() {
        return "all";
    }

    public static String companyScope(Long companyId) {
        return "company:" + companyId;
    }

    public static String userScope(Long userId) {
        return "user:" + userId;
    }

    /**
     * Cached report computation.
     * <p>
     * The key is the already-resolved scope plus normalized period — role stays out because the scope encodes what the
     * caller may see. Only the detached {@link ReportData} (scalar maps plus {@link HistogramTicket} DTOs, never
     * managed entities) is cached.
     */
    @CacheResult(cacheName = "report-snapshots")
    public ReportData computeReport(@CacheKey String scopeKey, @CacheKey String period) {
        String safePeriod = period == null || period.isBlank() ? "all" : period.toLowerCase();
        if (scopeKey != null && scopeKey.startsWith("company:")) {
            Company company = Company.findById(Long.valueOf(scopeKey.substring("company:".length())));
            if (company == null) {
                return emptyReport();
            }
            return buildReportData(List.of(company), safePeriod);
        }
        if (scopeKey != null && scopeKey.startsWith("user:")) {
            User user = User.findById(Long.valueOf(scopeKey.substring("user:".length())));
            List<Company> companies = user == null ? List.of()
                    : Company.list(
                            "select distinct c from Company c join c.users u where u = ?1 and exists (select t from Ticket t where t.company = c) order by c.name",
                            user);
            return buildReportData(companies, safePeriod);
        }
        return buildReportData(null, safePeriod);
    }

    /**
     * Evicts every cached report snapshot. Targeted per-company invalidation is infeasible (keys embed user scopes), so
     * ticket/message writes evict everything; correctness over cache efficiency, with TTL as backstop.
     */
    @CacheInvalidateAll(cacheName = "report-snapshots")
    public void invalidateAll() {
    }

    private ReportData emptyReport() {
        ReportData data = new ReportData();
        data.totalTickets = 0;
        data.ticketsByStatus = new LinkedHashMap<>();
        data.ticketsByCategory = new LinkedHashMap<>();
        data.ticketsByCompany = new LinkedHashMap<>();
        data.ticketsOverTime = new TreeMap<>();
        data.firstResponseTimeStats = new LinkedHashMap<>();
        data.resolutionTimeStats = new LinkedHashMap<>();
        data.pickupTimeStats = new LinkedHashMap<>();
        data.resolutionHistogram = new LinkedHashMap<>();
        return data;
    }

    public ReportData buildReportData(List<Company> filterCompanies, String period) {
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
        data.firstResponseTimeStats = buildFirstResponseTimeStats(tickets, messagesByTicket);
        data.resolutionTimeStats = buildResolutionTimeStats(tickets, messagesByTicket);
        data.pickupTimeStats = buildPickupTimeStats(tickets);
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

    private Map<String, TimeStat> buildFirstResponseTimeStats(List<Ticket> tickets,
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
        Map<String, TimeStat> unsorted = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : hoursByCategory.entrySet()) {
            List<Double> values = entry.getValue();
            double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            unsorted.put(entry.getKey(), new TimeStat(Math.round(min * 10.0) / 10.0, Math.round(average * 10.0) / 10.0,
                    Math.round(max * 10.0) / 10.0));
        }
        Map<String, TimeStat> result = new LinkedHashMap<>();
        unsorted.entrySet().stream()
                .sorted((left, right) -> Double.compare(right.getValue().avg(), left.getValue().avg()))
                .forEachOrdered(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    private Map<String, TimeStat> buildResolutionTimeStats(List<Ticket> tickets,
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
        Map<String, TimeStat> unsorted = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : hoursByCategory.entrySet()) {
            List<Double> values = entry.getValue();
            double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            unsorted.put(entry.getKey(), new TimeStat(Math.round(min * 10.0) / 10.0, Math.round(average * 10.0) / 10.0,
                    Math.round(max * 10.0) / 10.0));
        }
        Map<String, TimeStat> result = new LinkedHashMap<>();
        unsorted.entrySet().stream()
                .sorted((left, right) -> Double.compare(right.getValue().avg(), left.getValue().avg()))
                .forEachOrdered(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    private Map<String, PickupTimeStat> buildPickupTimeStats(List<Ticket> tickets) {
        Map<Long, LocalDateTime> openedByTicket = new LinkedHashMap<>();
        Map<Long, LocalDateTime> assignedByTicket = new LinkedHashMap<>();
        List<Long> ticketIds = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if (ticket.id != null) {
                ticketIds.add(ticket.id);
            }
        }
        if (!ticketIds.isEmpty()) {
            List<Event> events = Event.find("key in ?1 and eventType in ?2 order by createdAt asc", ticketIds,
                    List.of(EventConstants.TICKET_OPENED, EventConstants.TICKET_ASSIGNED)).list();
            for (Event event : events) {
                if (event.key == null || event.createdAt == null || event.eventType == null) {
                    continue;
                }
                if (event.eventType == EventConstants.TICKET_OPENED) {
                    openedByTicket.putIfAbsent(event.key, event.createdAt);
                }
            }
            for (Event event : events) {
                if (event.key == null || event.createdAt == null || event.eventType == null) {
                    continue;
                }
                if (event.eventType == EventConstants.TICKET_ASSIGNED && !assignedByTicket.containsKey(event.key)) {
                    LocalDateTime opened = openedByTicket.get(event.key);
                    if (opened != null && !event.createdAt.isBefore(opened)) {
                        assignedByTicket.put(event.key, event.createdAt);
                    }
                }
            }
        }
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<Double>> hoursByCategory = new LinkedHashMap<>();
        for (Ticket ticket : tickets) {
            LocalDateTime opened = openedByTicket.get(ticket.id);
            if (opened == null) {
                continue;
            }
            LocalDateTime assigned = assignedByTicket.getOrDefault(ticket.id, now);
            double hours = TicketTimeSupport.elapsedMinutes(opened, assigned) / 60.0;
            String category = ticket.category != null && ticket.category.name != null ? ticket.category.name
                    : "Uncategorized";
            hoursByCategory.computeIfAbsent(category, ignored -> new ArrayList<>()).add(hours);
        }
        Map<String, PickupTimeStat> unsorted = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : hoursByCategory.entrySet()) {
            List<Double> values = entry.getValue();
            double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            unsorted.put(entry.getKey(), new PickupTimeStat(Math.round(min * 10.0) / 10.0,
                    Math.round(average * 10.0) / 10.0, Math.round(max * 10.0) / 10.0));
        }
        Map<String, PickupTimeStat> result = new LinkedHashMap<>();
        unsorted.entrySet().stream()
                .sorted((left, right) -> Double.compare(right.getValue().avg(), left.getValue().avg()))
                .forEachOrdered(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    private Map<String, List<HistogramTicket>> buildResolutionHistogram(List<Ticket> tickets,
            Map<Long, List<Message>> messagesByTicket) {
        Map<String, List<HistogramTicket>> histogram = new LinkedHashMap<>();
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
            HistogramTicket summary = new HistogramTicket(ticket.id, ticket.name, ticket.status,
                    ticket.company == null ? null : ticket.company.name,
                    ticket.category == null ? null : ticket.category.name);
            double hours = Duration.between(first.date, last.date).toMinutes() / 60.0;
            if (hours < 1) {
                histogram.get(BUCKET_UNDER_1H).add(summary);
            } else if (hours < 8) {
                histogram.get(BUCKET_1_TO_8H).add(summary);
            } else if (hours < 24) {
                histogram.get(BUCKET_8_TO_24H).add(summary);
            } else if (hours < 168) {
                histogram.get(BUCKET_1_TO_7D).add(summary);
            } else {
                histogram.get(BUCKET_OVER_7D).add(summary);
            }
        }
        return histogram;
    }
}
