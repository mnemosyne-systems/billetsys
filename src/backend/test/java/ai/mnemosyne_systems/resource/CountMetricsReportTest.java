/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * HTTP-level parity tests for the count-based report metrics (status, category, company, timeline, histogram, total).
 * <p>
 * All assertions go through {@code GET /api/reports} against freshly seeded per-test companies, so the expected values
 * below are hand-computed from the fixtures — independent of the previous entity-based implementation.
 */
@QuarkusTest
class CountMetricsReportTest extends AccessTestSupport {

    static final String REQUESTER_EMAIL = "cm-requester@mnemosyne-systems.ai";
    static final String SUPPORT_EMAIL = "cm-support@mnemosyne-systems.ai";
    static final String ADMIN_EMAIL = "countmetricsadmin@mnemosyne-systems.ai";

    @Transactional
    Long seedCountTicket(String companyName, String categoryName) {
        ensureCountUsers();
        Long companyId = ensureCompany(companyName);
        Ticket ticket = ensureTicket(companyId);
        Ticket managed = Ticket.findById(ticket.id);
        // Scope seeded tickets to this test's own users: ensureTicket() defaults to the shared
        // "user"/"support1"/"tam1" accounts, which would pollute other test classes' assumptions about those users'
        // ticket lists (page size 10).
        managed.requester = User.find("email", REQUESTER_EMAIL).firstResult();
        managed.supportUsers.clear();
        managed.supportUsers.add(User.find("email", SUPPORT_EMAIL).firstResult());
        managed.tamUsers.clear();
        if (categoryName != null) {
            Category category = ensureCategory(categoryName, categoryName + " description", false);
            managed.category = category;
        }
        return ticket.id;
    }

    @Transactional
    void ensureCountUsers() {
        ensureUser("cmrequester", REQUESTER_EMAIL, User.TYPE_USER);
        ensureUser("cmsupport", SUPPORT_EMAIL, User.TYPE_SUPPORT);
    }

    @Transactional
    void seedCountMessages(Long ticketId, String bodyPrefix, LocalDateTime firstAt, LocalDateTime lastAt) {
        ensureCountUsers();
        Ticket ticket = Ticket.findById(ticketId);
        ensureTimedMessage(ticket, bodyPrefix + " opened", REQUESTER_EMAIL, firstAt);
        ensureTimedMessage(ticket, bodyPrefix + " closed", SUPPORT_EMAIL, lastAt);
        setTicketStatus(ticketId, "Closed");
    }

    @Transactional
    String ticketName(Long ticketId) {
        Ticket ticket = Ticket.findById(ticketId);
        return ticket.name;
    }

    void ensureCountAdmin() {
        ensureUser("countmetricsadmin", ADMIN_EMAIL, User.TYPE_ADMIN);
    }

    ExtractableResponse<Response> report(Long companyId, String period) {
        ensureCountAdmin();
        return RestAssured.given().queryParam("companyId", companyId).queryParam("period", period).get("/api/reports")
                .then().statusCode(200).extract();
    }

    static Map<String, Long> toCountMap(List<Map<String, Object>> points) {
        Map<String, Long> result = new HashMap<>();
        for (Map<String, Object> point : points) {
            result.put((String) point.get("label"), ((Number) point.get("value")).longValue());
        }
        return result;
    }

    @Test
    @TestSecurity(user = "countmetricsadmin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "countmetricsadmin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "countmetricsadmin") })
    void statusCountsMergeNullAndBlankIntoOpen() {
        String company = "Count Status Co";
        seedCountTicket(company, null);
        seedCountTicket(company, null);
        Long closedId = seedCountTicket(company, null);
        Long blankStatusId = seedCountTicket(company, null);
        setTicketStatus(closedId, "Closed");
        setTicketStatus(blankStatusId, "   ");
        Long companyId = ensureCompany(company);

        Map<String, Long> status = toCountMap(report(companyId, "all").path("status"));

        Assertions.assertEquals(Map.of("Assigned", 2L, "Closed", 1L, "Open", 1L), status);
    }

    @Test
    @TestSecurity(user = "countmetricsadmin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "countmetricsadmin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "countmetricsadmin") })
    void categoryCountsSortByCountDescWithUncategorizedFallback() {
        String company = "Count Category Co";
        for (int i = 0; i < 5; i++) {
            seedCountTicket(company, "Count Cat A");
        }
        for (int i = 0; i < 3; i++) {
            seedCountTicket(company, "Count Cat B");
        }
        for (int i = 0; i < 2; i++) {
            seedCountTicket(company, "Count Cat C");
        }
        seedCountTicket(company, null);
        Long companyId = ensureCompany(company);

        List<Map<String, Object>> category = report(companyId, "all").path("category");

        Assertions.assertEquals(List.of("Count Cat A", "Count Cat B", "Count Cat C", "Uncategorized"),
                category.stream().map(point -> point.get("label")).toList());
        Assertions.assertEquals(List.of(5L, 3L, 2L, 1L),
                category.stream().map(point -> ((Number) point.get("value")).longValue()).toList());
    }

    @Test
    @TestSecurity(user = "countmetricsadmin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "countmetricsadmin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "countmetricsadmin") })
    void companyChartVisibleToAdminWithoutCompanyFilter() {
        String company = "Count Company Co";
        seedCountTicket(company, null);
        Long companyId = ensureCompany(company);
        ensureCountAdmin();

        RestAssured.given().get("/api/reports").then().statusCode(200).body("showCompanyChart", Matchers.equalTo(true))
                .body("company.label", Matchers.hasItem(company));

        // The scoped view still reports the company chart data for its own company.
        Assertions.assertEquals(Map.of(company, 1L), toCountMap(RestAssured.given().queryParam("companyId", companyId)
                .get("/api/reports").then().statusCode(200).extract().path("company")));
    }

    @Test
    @TestSecurity(user = "countmetrictam", roles = "tam")
    @JwtSecurity(claims = { @Claim(key = "email", value = "countmetrictam@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "countmetrictam") })
    void companyChartHiddenFromTam() {
        ensureUser("countmetrictam", "countmetrictam@mnemosyne-systems.ai", User.TYPE_TAM);
        Long ownCompanyId = ensureCompany("Count Tam Chart Co");
        ensureCompanyUsers(ownCompanyId, "countmetrictam@mnemosyne-systems.ai");
        seedCountTicket("Count Tam Chart Co", null);

        RestAssured.given().get("/api/reports").then().statusCode(200).body("role", Matchers.equalTo("tam"))
                .body("showCompanyChart", Matchers.equalTo(false));
    }

    @Test
    @TestSecurity(user = "countmetricsadmin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "countmetricsadmin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "countmetricsadmin") })
    void timelineRespectsPeriodCutoffsAndSkipsMessageLessTickets() {
        String company = "Count Timeline Co";
        ensureCountUsers();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("yyyy-MM");

        // First message exactly at the month cutoff plus a later same-scope message: proves the cutoff is inclusive
        // and that the first (not last) message date is used as the creation proxy.
        Long cutoffId = seedCountTicket(company, null);
        Ticket cutoffTicket = Ticket.findById(cutoffId);
        ensureTimedMessage(cutoffTicket, "Cutoff first", REQUESTER_EMAIL, cutoff);
        ensureTimedMessage(cutoffTicket, "Cutoff second", SUPPORT_EMAIL, cutoff.plusHours(3));

        Long todayId = seedCountTicket(company, null);
        ensureTimedMessage(Ticket.findById(todayId), "Today message", REQUESTER_EMAIL, now);

        LocalDateTime lastMonthAt = now.minusMonths(1);
        Long lastMonthId = seedCountTicket(company, null);
        ensureTimedMessage(Ticket.findById(lastMonthId), "Last month message", REQUESTER_EMAIL, lastMonthAt);

        LocalDateTime lastYearAt = now.minusYears(1);
        Long lastYearId = seedCountTicket(company, null);
        ensureTimedMessage(Ticket.findById(lastYearId), "Last year message", REQUESTER_EMAIL, lastYearAt);

        // Zero-message ticket: must never be counted.
        seedCountTicket(company, null);
        Long companyId = ensureCompany(company);

        Map<String, Long> monthExpected = new HashMap<>();
        monthExpected.merge(dayFormat.format(cutoff), 1L, Long::sum);
        monthExpected.merge(dayFormat.format(now), 1L, Long::sum);
        Assertions.assertEquals(monthExpected, toCountMap(report(companyId, "month").path("timeline")));

        Map<String, Long> yearExpected = new HashMap<>();
        yearExpected.merge(monthFormat.format(cutoff), 1L, Long::sum);
        yearExpected.merge(monthFormat.format(now), 1L, Long::sum);
        // A "last month" fixture in January belongs to the previous year and is correctly excluded.
        if (lastMonthAt.getYear() == now.getYear()) {
            yearExpected.merge(monthFormat.format(lastMonthAt), 1L, Long::sum);
        }
        Map<String, Long> yearActual = toCountMap(report(companyId, "year").path("timeline"));
        Assertions.assertEquals(yearExpected, yearActual);
        Assertions.assertFalse(yearActual.containsKey(monthFormat.format(lastYearAt)));

        Map<String, Long> allActual = toCountMap(report(companyId, "all").path("timeline"));
        Assertions.assertTrue(allActual.containsKey(monthFormat.format(lastYearAt)));
        Assertions.assertEquals(4L, allActual.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    @TestSecurity(user = "countmetricsadmin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "countmetricsadmin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "countmetricsadmin") })
    void histogramBucketsClosedTicketsWithDrilldown() {
        String company = "Count Histogram Co";
        LocalDateTime base = LocalDateTime.now().minusDays(20);

        Long underHourId = seedCountTicket(company, "Count Hist Cat");
        seedCountMessages(underHourId, "HistHalfHour", base, base.plusMinutes(30));
        Long oneToEightId = seedCountTicket(company, "Count Hist Cat");
        seedCountMessages(oneToEightId, "HistTwoHours", base, base.plusHours(2));
        Long eightToDayId = seedCountTicket(company, "Count Hist Cat");
        seedCountMessages(eightToDayId, "HistTenHours", base, base.plusHours(10));
        Long oneToSevenId = seedCountTicket(company, "Count Hist Cat");
        seedCountMessages(oneToSevenId, "HistThreeDays", base, base.plusHours(72));
        // Uncategorized on purpose: the drill-down must propagate a null category name.
        Long overWeekId = seedCountTicket(company, null);
        seedCountMessages(overWeekId, "HistTenDays", base, base.plusHours(240));

        // Open ticket with messages: excluded from the histogram.
        Long openId = seedCountTicket(company, "Count Hist Cat");
        ensureCountUsers();
        Ticket openTicket = Ticket.findById(openId);
        ensureTimedMessage(openTicket, "HistOpen first", REQUESTER_EMAIL, base);
        ensureTimedMessage(openTicket, "HistOpen second", SUPPORT_EMAIL, base.plusHours(5));

        // Closed ticket without messages: excluded from the histogram.
        Long closedNoMessagesId = seedCountTicket(company, "Count Hist Cat");
        setTicketStatus(closedNoMessagesId, "Closed");
        Long companyId = ensureCompany(company);

        List<Map<String, Object>> histogram = report(companyId, "all").path("histogram");
        Assertions.assertEquals(5, histogram.size());
        assertHistogramBucket(histogram, "< 1h", ticketName(underHourId), "Closed", company, "Count Hist Cat");
        assertHistogramBucket(histogram, "1–8h", ticketName(oneToEightId), "Closed", company, "Count Hist Cat");
        assertHistogramBucket(histogram, "8–24h", ticketName(eightToDayId), "Closed", company, "Count Hist Cat");
        assertHistogramBucket(histogram, "1–7 days", ticketName(oneToSevenId), "Closed", company, "Count Hist Cat");
        assertHistogramBucket(histogram, "> 7 days", ticketName(overWeekId), "Closed", company, null);

        List<String> allNames = histogram.stream()
                .flatMap(bucket -> ((List<Map<String, Object>>) bucket.get("tickets")).stream())
                .map(ticket -> (String) ticket.get("name")).toList();
        Assertions.assertFalse(allNames.contains(ticketName(openId)));
        Assertions.assertFalse(allNames.contains(ticketName(closedNoMessagesId)));
    }

    void assertHistogramBucket(List<Map<String, Object>> histogram, String label, String expectedName,
            String expectedStatus, String expectedCompany, String expectedCategory) {
        Map<String, Object> bucket = histogram.stream().filter(entry -> label.equals(entry.get("label"))).findFirst()
                .orElseThrow(() -> new AssertionError("Missing histogram bucket: " + label));
        Assertions.assertEquals(1, ((Number) bucket.get("count")).intValue());
        List<Map<String, Object>> tickets = (List<Map<String, Object>>) bucket.get("tickets");
        Assertions.assertEquals(1, tickets.size());
        Assertions.assertEquals(expectedName, tickets.get(0).get("name"));
        Assertions.assertEquals(expectedStatus, tickets.get(0).get("status"));
        Assertions.assertEquals(expectedCompany, tickets.get(0).get("companyName"));
        Assertions.assertEquals(expectedCategory, tickets.get(0).get("categoryName"));
    }

    @Test
    @TestSecurity(user = "countscopetam", roles = "tam")
    @JwtSecurity(claims = { @Claim(key = "email", value = "countscopetam@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "countscopetam") })
    void countMetricsRespectTamCompanyScoping() {
        // Dedicated TAM user: company memberships accumulate across tests in the shared test database.
        ensureUser("countscopetam", "countscopetam@mnemosyne-systems.ai", User.TYPE_TAM);
        Long ownCompanyId = ensureCompany("Count Scope Co A");
        ensureCompany("Count Scope Co B");
        ensureCompanyUsers(ownCompanyId, "countscopetam@mnemosyne-systems.ai");
        seedCountTicket("Count Scope Co A", null);
        Long otherId = seedCountTicket("Count Scope Co B", null);
        setTicketStatus(otherId, "Closed");

        ExtractableResponse<Response> response = RestAssured.given().get("/api/reports").then().statusCode(200)
                .body("role", Matchers.equalTo("tam")).extract();

        Assertions.assertEquals(1, ((Number) response.path("totalTickets")).intValue());
        Assertions.assertEquals(Map.of("Assigned", 1L), toCountMap(response.path("status")));
        Assertions.assertEquals(Map.of("Uncategorized", 1L), toCountMap(response.path("category")));
        Assertions.assertEquals(Map.of("Count Scope Co A", 1L), toCountMap(response.path("company")));
        List<Map<String, Object>> histogram = response.path("histogram");
        Assertions.assertTrue(histogram.stream().allMatch(bucket -> ((List<?>) bucket.get("tickets")).isEmpty()));
    }

    @Test
    @TestSecurity(user = "countmetricsuper", roles = "superuser")
    @JwtSecurity(claims = { @Claim(key = "email", value = "countmetricsuper@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "countmetricsuper") })
    void countMetricsRespectSuperuserCompanyScoping() {
        ensureUser("countmetricsuper", "countmetricsuper@mnemosyne-systems.ai", User.TYPE_SUPERUSER);
        Long ownCompanyId = ensureCompany("Count Super Co A");
        ensureCompany("Count Super Co B");
        ensureCompanyUsers(ownCompanyId, "countmetricsuper@mnemosyne-systems.ai");
        seedCountTicket("Count Super Co A", null);
        Long otherId = seedCountTicket("Count Super Co B", null);
        setTicketStatus(otherId, "Closed");

        ExtractableResponse<Response> response = RestAssured.given().get("/api/reports").then().statusCode(200)
                .body("role", Matchers.equalTo("superuser")).extract();

        Assertions.assertEquals(1, ((Number) response.path("totalTickets")).intValue());
        Assertions.assertEquals(Map.of("Assigned", 1L), toCountMap(response.path("status")));
        Assertions.assertEquals(Map.of("Uncategorized", 1L), toCountMap(response.path("category")));
    }

    @Test
    @TestSecurity(user = "countmetricsadmin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "countmetricsadmin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "countmetricsadmin") })
    void totalTicketsCountsScopedTickets() {
        String company = "Count Total Co";
        seedCountTicket(company, null);
        seedCountTicket(company, null);
        Long closedId = seedCountTicket(company, null);
        setTicketStatus(closedId, "Closed");
        Long companyId = ensureCompany(company);

        Assertions.assertEquals(3, ((Number) report(companyId, "all").path("totalTickets")).intValue());
    }
}
