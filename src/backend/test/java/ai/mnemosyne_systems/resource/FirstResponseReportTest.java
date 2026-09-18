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
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FirstResponseReportTest extends AccessTestSupport {

    static final String REQUESTER_EMAIL = "fr-requester@mnemosyne-systems.ai";
    static final String SUPPORT_EMAIL = "fr-support@mnemosyne-systems.ai";

    @Transactional
    Long seedFirstResponseTicket(String companyName, String categoryName) {
        Long companyId = ensureCompany(companyName);
        Category category = ensureCategory(categoryName, categoryName + " description", false);
        Ticket ticket = ensureTicket(companyId);
        Ticket managed = Ticket.findById(ticket.id);
        managed.category = category;
        // Scope seeded tickets to this test's own requester: ensureTicket() defaults
        // to the shared "user" account, which would pollute other test classes'
        // assumptions about that user's ticket list (page size 10).
        ensureFirstResponseUsers();
        managed.requester = User.find("email", REQUESTER_EMAIL).firstResult();
        return managed.id;
    }

    @Transactional
    void ensureFirstResponseUsers() {
        ensureUser("frrequester", REQUESTER_EMAIL, User.TYPE_USER);
        ensureUser("frsupport", SUPPORT_EMAIL, User.TYPE_SUPPORT);
    }

    @Transactional
    void seedFirstResponseMessages(Long ticketId, String bodyPrefix, LocalDateTime firstAt, LocalDateTime replyAt) {
        ensureFirstResponseUsers();
        Ticket ticket = Ticket.findById(ticketId);
        ensureTimedMessage(ticket, bodyPrefix + " request", REQUESTER_EMAIL, firstAt);
        if (replyAt != null) {
            ensureTimedMessage(ticket, bodyPrefix + " reply", SUPPORT_EMAIL, replyAt);
        }
    }

    @Transactional
    Long firstResponseCompanyId(String companyName) {
        return ensureCompany(companyName);
    }

    void ensureFirstResponseAdmin() {
        ensureUser("firstresponseadmin", "firstresponseadmin@mnemosyne-systems.ai", User.TYPE_ADMIN);
    }

    List<Map<String, Object>> firstResponsePoints(Long companyId) {
        ensureFirstResponseAdmin();
        return RestAssured.given().queryParam("companyId", companyId).get("/api/reports").then().statusCode(200)
                .extract().path("firstResponse");
    }

    Double firstResponseValue(List<Map<String, Object>> points, String category) {
        return firstResponseAvg(points, category);
    }

    Double firstResponseAvg(List<Map<String, Object>> points, String category) {
        Map<String, Object> point = firstResponsePoint(points, category);
        return point == null ? null : ((Number) point.get("avg")).doubleValue();
    }

    Double firstResponseMin(List<Map<String, Object>> points, String category) {
        Map<String, Object> point = firstResponsePoint(points, category);
        return point == null ? null : ((Number) point.get("min")).doubleValue();
    }

    Double firstResponseMax(List<Map<String, Object>> points, String category) {
        Map<String, Object> point = firstResponsePoint(points, category);
        return point == null ? null : ((Number) point.get("max")).doubleValue();
    }

    Map<String, Object> firstResponsePoint(List<Map<String, Object>> points, String category) {
        for (Map<String, Object> point : points) {
            if (category.equals(point.get("label"))) {
                return point;
            }
        }
        return null;
    }

    @Test
    @TestSecurity(user = "firstresponseadmin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "firstresponseadmin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "firstresponseadmin") })
    void firstResponseHappyPath() {
        Long companyId = firstResponseCompanyId("First Response Happy Co");
        Long ticketId = seedFirstResponseTicket("First Response Happy Co", "First Response Happy Cat");
        LocalDateTime firstAt = LocalDateTime.now().minusHours(5);
        seedFirstResponseMessages(ticketId, "Happy", firstAt, firstAt.plusHours(2));

        List<Map<String, Object>> points = firstResponsePoints(companyId);

        Assertions.assertEquals(1, points.size());
        Assertions.assertEquals(2.0, firstResponseValue(points, "First Response Happy Cat"), 0.05);
        Assertions.assertEquals(2.0, firstResponseMin(points, "First Response Happy Cat"), 0.05);
        Assertions.assertEquals(2.0, firstResponseMax(points, "First Response Happy Cat"), 0.05);
    }

    @Test
    @TestSecurity(user = "firstresponseadmin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "firstresponseadmin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "firstresponseadmin") })
    void firstResponseReportsMinAvgMax() {
        Long companyId = firstResponseCompanyId("First Response Stats Co");
        LocalDateTime firstAt = LocalDateTime.now().minusHours(10);

        Long firstId = seedFirstResponseTicket("First Response Stats Co", "First Response Stats Cat");
        seedFirstResponseMessages(firstId, "Stats one", firstAt, firstAt.plusHours(2));

        Long secondId = seedFirstResponseTicket("First Response Stats Co", "First Response Stats Cat");
        seedFirstResponseMessages(secondId, "Stats two", firstAt, firstAt.plusHours(4));

        Long thirdId = seedFirstResponseTicket("First Response Stats Co", "First Response Stats Cat");
        seedFirstResponseMessages(thirdId, "Stats three", firstAt, firstAt.plusHours(6));

        List<Map<String, Object>> points = firstResponsePoints(companyId);

        Assertions.assertEquals(1, points.size());
        Assertions.assertEquals(2.0, firstResponseMin(points, "First Response Stats Cat"), 0.05);
        Assertions.assertEquals(4.0, firstResponseAvg(points, "First Response Stats Cat"), 0.05);
        Assertions.assertEquals(6.0, firstResponseMax(points, "First Response Stats Cat"), 0.05);
    }

    @Test
    @TestSecurity(user = "firstresponseadmin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "firstresponseadmin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "firstresponseadmin") })
    void firstResponseExcludesTicketWithoutSupportReply() {
        Long companyId = firstResponseCompanyId("First Response No Reply Co");
        Long ticketId = seedFirstResponseTicket("First Response No Reply Co", "First Response No Reply Cat");
        LocalDateTime firstAt = LocalDateTime.now().minusHours(4);
        ensureFirstResponseUsers();
        Ticket ticket = Ticket.findById(ticketId);
        ensureTimedMessage(ticket, "No reply request", REQUESTER_EMAIL, firstAt);
        ensureTimedMessage(ticket, "No reply follow-up", REQUESTER_EMAIL, firstAt.plusHours(1));

        List<Map<String, Object>> points = firstResponsePoints(companyId);

        Assertions.assertTrue(points.isEmpty(), "Ticket without support reply must be excluded entirely");
    }

    @Test
    @TestSecurity(user = "firstresponseadmin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "firstresponseadmin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "firstresponseadmin") })
    void firstResponseGroupsByCategory() {
        Long companyId = firstResponseCompanyId("First Response Group Co");
        LocalDateTime firstAt = LocalDateTime.now().minusHours(10);

        Long firstId = seedFirstResponseTicket("First Response Group Co", "First Response Group Cat A");
        seedFirstResponseMessages(firstId, "Group A", firstAt, firstAt.plusHours(2));

        Long secondId = seedFirstResponseTicket("First Response Group Co", "First Response Group Cat B");
        seedFirstResponseMessages(secondId, "Group B", firstAt, firstAt.plusHours(6));

        List<Map<String, Object>> points = firstResponsePoints(companyId);

        Assertions.assertEquals(2, points.size());
        Assertions.assertEquals(2.0, firstResponseValue(points, "First Response Group Cat A"), 0.05);
        Assertions.assertEquals(6.0, firstResponseValue(points, "First Response Group Cat B"), 0.05);
    }

    @Test
    @TestSecurity(user = "firstresponsetam", roles = "tam")
    @JwtSecurity(claims = { @Claim(key = "email", value = "firstresponsetam@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "firstresponsetam") })
    void firstResponseRespectsTamCompanyScoping() {
        ensureUser("firstresponsetam", "firstresponsetam@mnemosyne-systems.ai", User.TYPE_TAM);
        Long ownCompanyId = firstResponseCompanyId("First Response Scope Co A");
        firstResponseCompanyId("First Response Scope Co B");
        ensureCompanyUsers(ownCompanyId, "firstresponsetam@mnemosyne-systems.ai");
        LocalDateTime firstAt = LocalDateTime.now().minusHours(10);

        Long ownTicketId = seedFirstResponseTicket("First Response Scope Co A", "First Response Scope Cat A");
        seedFirstResponseMessages(ownTicketId, "Scope own", firstAt, firstAt.plusHours(2));

        Long otherTicketId = seedFirstResponseTicket("First Response Scope Co B", "First Response Scope Cat B");
        seedFirstResponseMessages(otherTicketId, "Scope other", firstAt, firstAt.plusHours(8));

        List<Map<String, Object>> points = RestAssured.given().get("/api/reports").then().statusCode(200)
                .body("role", Matchers.equalTo("tam")).extract().path("firstResponse");

        Assertions.assertEquals(2.0, firstResponseValue(points, "First Response Scope Cat A"), 0.05);
        Assertions.assertNull(firstResponseValue(points, "First Response Scope Cat B"),
                "TAM must not see other companies reflected in the first response average");
    }
}
