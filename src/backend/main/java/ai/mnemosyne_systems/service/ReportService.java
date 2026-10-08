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
import ai.mnemosyne_systems.model.PickupTimeStat;
import ai.mnemosyne_systems.model.ReportData;
import ai.mnemosyne_systems.model.TimeStat;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.TicketTimeSupport;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
 * Every metric is aggregated database-side via {@link ReportQueryService} (GROUP BY counts plus one slim row per ticket
 * — ids, names, raw timestamps only); no Ticket/Message entity hydration happens here. The {@code report-snapshots}
 * cache on {@link #computeReport} sits above those SQL calls and caches their grouped output.
 */
@ApplicationScoped
public class ReportService {

    @Inject
    ReportQueryService reportQueryService;

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
        List<Long> companyIds = filterCompanies == null || filterCompanies.isEmpty() ? null
                : filterCompanies.stream().map(company -> company.id).toList();
        // Single shared row list for both Closed-ticket metrics: resolution stats and histogram bucketing.
        List<ReportQueryService.ResolutionRow> resolutionRowList = reportQueryService.resolutionRows(companyIds);

        ReportData data = new ReportData();
        data.totalTickets = (int) reportQueryService.totalTickets(companyIds);
        data.ticketsByStatus = buildTicketsByStatus(reportQueryService.statusCounts(companyIds));
        data.ticketsByCategory = buildTicketsByCategory(reportQueryService.categoryCounts(companyIds));
        data.ticketsByCompany = buildTicketsByCompany(reportQueryService.companyCounts(companyIds));
        data.ticketsOverTime = buildTicketsOverTime(reportQueryService.timelineRows(companyIds), period);
        data.firstResponseTimeStats = buildFirstResponseTimeStats(reportQueryService.firstResponseRows(companyIds));
        data.resolutionTimeStats = buildResolutionTimeStats(resolutionRowList);
        data.pickupTimeStats = buildPickupTimeStats(reportQueryService.pickupRows(companyIds));
        data.resolutionHistogram = buildResolutionHistogram(resolutionRowList);
        return data;
    }

    private Map<String, Long> buildTicketsByStatus(List<ReportQueryService.StatusCount> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (ReportQueryService.StatusCount row : rows) {
            String status = row.status() == null || row.status().isBlank() ? "Open" : row.status();
            result.merge(status, row.count(), Long::sum);
        }
        return result;
    }

    private Map<String, Long> buildTicketsByCategory(List<ReportQueryService.CategoryCount> rows) {
        Map<String, Long> unsorted = new LinkedHashMap<>();
        for (ReportQueryService.CategoryCount row : rows) {
            String name = row.categoryName() != null ? row.categoryName() : "Uncategorized";
            unsorted.merge(name, row.count(), Long::sum);
        }
        Map<String, Long> result = new LinkedHashMap<>();
        unsorted.entrySet().stream().sorted(Map.Entry.<String, Long> comparingByValue().reversed())
                .forEachOrdered(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    private Map<String, Long> buildTicketsByCompany(List<ReportQueryService.CompanyCount> rows) {
        Map<String, Long> unsorted = new LinkedHashMap<>();
        for (ReportQueryService.CompanyCount row : rows) {
            String name = row.companyName() != null ? row.companyName() : "Unknown";
            unsorted.merge(name, row.count(), Long::sum);
        }
        Map<String, Long> result = new LinkedHashMap<>();
        unsorted.entrySet().stream().sorted(Map.Entry.<String, Long> comparingByValue().reversed())
                .forEachOrdered(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    private Map<String, Long> buildTicketsOverTime(List<ReportQueryService.TimelineRow> rows, String period) {
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
        for (ReportQueryService.TimelineRow row : rows) {
            if (row.firstAt() != null) {
                java.time.LocalDateTime date = row.firstAt();
                if (cutoff != null && date.isBefore(cutoff)) {
                    continue;
                }
                result.merge(format.format(date), 1L, Long::sum);
            }
        }
        return result;
    }

    private Map<String, TimeStat> buildFirstResponseTimeStats(List<ReportQueryService.FirstResponseRow> rows) {
        Map<String, List<Double>> hoursByCategory = new LinkedHashMap<>();
        for (ReportQueryService.FirstResponseRow row : rows) {
            if (row.messageCount() < 2) {
                continue;
            }
            if (row.firstAt() == null || row.replyAt() == null) {
                continue;
            }
            double hours = Duration.between(row.firstAt(), row.replyAt()).toMinutes() / 60.0;
            String category = row.categoryName() != null ? row.categoryName() : "Uncategorized";
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

    private Map<String, TimeStat> buildResolutionTimeStats(List<ReportQueryService.ResolutionRow> rows) {
        Map<String, List<Double>> hoursByCategory = new LinkedHashMap<>();
        for (ReportQueryService.ResolutionRow row : rows) {
            if (!"Closed".equalsIgnoreCase(row.status())) {
                continue;
            }
            if (row.firstAt() == null || row.lastAt() == null) {
                continue;
            }
            double hours = Duration.between(row.firstAt(), row.lastAt()).toMinutes() / 60.0;
            String category = row.categoryName() != null ? row.categoryName() : "Uncategorized";
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

    private Map<String, PickupTimeStat> buildPickupTimeStats(List<ReportQueryService.PickupRow> rows) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<Double>> hoursByCategory = new LinkedHashMap<>();
        for (ReportQueryService.PickupRow row : rows) {
            LocalDateTime opened = row.openedAt();
            if (opened == null) {
                continue;
            }
            // Stale-ASSIGNED-before-OPENED is already resolved in SQL (assignedAt is the earliest ASSIGNED at or
            // after opened, or null); a null here means "never validly assigned", which falls back to now —
            // exactly like the old getOrDefault(ticket.id, now).
            LocalDateTime assigned = row.assignedAt() != null ? row.assignedAt() : now;
            double hours = TicketTimeSupport.elapsedMinutes(opened, assigned) / 60.0;
            String category = row.categoryName() != null ? row.categoryName() : "Uncategorized";
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

    private Map<String, List<HistogramTicket>> buildResolutionHistogram(List<ReportQueryService.ResolutionRow> rows) {
        Map<String, List<HistogramTicket>> histogram = new LinkedHashMap<>();
        histogram.put(BUCKET_UNDER_1H, new ArrayList<>());
        histogram.put(BUCKET_1_TO_8H, new ArrayList<>());
        histogram.put(BUCKET_8_TO_24H, new ArrayList<>());
        histogram.put(BUCKET_1_TO_7D, new ArrayList<>());
        histogram.put(BUCKET_OVER_7D, new ArrayList<>());

        for (ReportQueryService.ResolutionRow row : rows) {
            if (!"Closed".equalsIgnoreCase(row.status())) {
                continue;
            }
            if (row.firstAt() == null || row.lastAt() == null) {
                continue;
            }
            HistogramTicket summary = new HistogramTicket(row.ticketId(), row.ticketName(), row.status(),
                    row.companyName(), row.categoryName());
            double hours = Duration.between(row.firstAt(), row.lastAt()).toMinutes() / 60.0;
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
