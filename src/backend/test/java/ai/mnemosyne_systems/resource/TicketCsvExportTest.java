/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TicketCsvExportTest extends AccessTestSupport {

    private static final String HEADER = "ticket_name,title,status,date,category,support_user,support_email,company,"
            + "entitlement,level,affects_version,resolved_version";

    @Test
    void supportCanExportAssignedTicketsAsCsv() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("support2", "support2@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support2");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "pass");
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Csv Export Co");
        ensureCompanyUsers(companyId, "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyId);
        ensureMessage(ticket, "Csv export sample message.");
        String cookie = login("support1", "support1");

        Response response = RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/api/support/tickets/export").then().statusCode(200).contentType(Matchers.startsWith("text/csv"))
                .header("Content-Disposition", Matchers.containsString("tickets-assigned.csv")).extract().response();
        String csv = response.asString();
        Assertions.assertTrue(csv.contains(HEADER));
        Assertions.assertTrue(csv.contains(ticket.name));
    }

    @Test
    void supportExportQuotesSpecialCharacters() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "pass");
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Csv Export Quote Co");
        ensureCompanyUsers(companyId, "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyId);
        ensureMessage(ticket, "Csv export quoting message.");
        setTicketTitle(ticket.id, "Export, \"quoted\" title");
        String cookie = login("support1", "support1");

        String csv = RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).queryParam("q", ticket.name)
                .get("/api/support/tickets/export").then().statusCode(200).extract().asString();
        Assertions.assertTrue(csv.contains("\"Export, \"\"quoted\"\" title\""));
    }

    @Test
    void supportExportRespectsSearchFilter() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "pass");
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Csv Export Filter Co");
        ensureCompanyUsers(companyId, "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyId);
        ensureMessage(ticket, "Csv export filter message.");
        String cookie = login("support1", "support1");

        String filtered = RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).queryParam("q", ticket.name)
                .get("/api/support/tickets/export").then().statusCode(200).extract().asString();
        Assertions.assertTrue(filtered.contains(ticket.name));

        String empty = RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).queryParam("q", "no-such-ticket-xyz")
                .get("/api/support/tickets/export").then().statusCode(200).extract().asString();
        String body = empty.startsWith("\uFEFF") ? empty.substring(1) : empty;
        Assertions.assertEquals(HEADER, body.lines().filter(line -> !line.isBlank()).toList().get(0));
        Assertions.assertEquals(1, body.lines().filter(line -> !line.isBlank()).count());
    }

    @Test
    void exportRequiresAuthentication() {
        RestAssured.given().redirects().follow(false).get("/api/support/tickets/export").then().statusCode(401);
        RestAssured.given().redirects().follow(false).get("/api/user/tickets/export").then().statusCode(401);
        RestAssured.given().redirects().follow(false).get("/api/superuser/tickets/export").then().statusCode(401);
    }

    @Test
    void userCanExportOwnTicketsAsCsv() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "pass");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Csv Export User Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyId);
        ensureMessage(ticket, "Csv export user message.");
        String cookie = login("user", "user");

        String csv = RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/user/tickets/export").then()
                .statusCode(200).contentType(Matchers.startsWith("text/csv")).extract().asString();
        Assertions.assertTrue(csv.contains(HEADER));
        Assertions.assertTrue(csv.contains(ticket.name));
        Assertions.assertTrue(csv.getBytes(StandardCharsets.UTF_8).length > 0);
    }

    @Test
    void superuserCanExportTicketsAsCsv() {
        ensureUser("superuser1", "superuser1@mnemosyne-systems.ai", User.TYPE_SUPERUSER, "superuser1");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "pass");
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Csv Export Superuser Co");
        ensureCompanyUsers(companyId, "superuser1@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyId);
        ensureMessage(ticket, "Csv export superuser message.");
        String cookie = login("superuser1", "superuser1");

        String csv = RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/superuser/tickets/export")
                .then().statusCode(200).contentType(Matchers.startsWith("text/csv")).extract().asString();
        Assertions.assertTrue(csv.contains(HEADER));
        Assertions.assertTrue(csv.contains(ticket.name));
    }

    @Transactional
    void setTicketTitle(Long ticketId, String title) {
        Ticket ticket = Ticket.findById(ticketId);
        Assertions.assertNotNull(ticket);
        ticket.title = title;
    }
}
