/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Entitlement;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.TicketImportRecord;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.service.TicketBootstrapService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class BugzillaTicketImportResourceTest extends AccessTestSupport {

    @Inject
    TicketBootstrapService ticketBootstrapService;

    @Test
    @TestSecurity(user = "bugzilla-support", roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = "bugzilla-support@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "bugzilla-support") })
    void supportUserCanImportBugzillaTickets() {
        ImportFixture fixture = fixture("bugzilla-success");
        String bugId = "bz-" + System.nanoTime();
        String csv = """
                bug_id,short_desc,bug_status,product,component,assigned_to,resolution,bug_severity,opendate
                %s,"Imported, quoted bug",RESOLVED,Platform,Theme,dev@example.com,FIXED,enhancement,2026-04-26 10:15:30
                """.formatted(bugId);

        RestAssured.given().contentType("multipart/form-data")
                .multiPart("file", "bugs.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .multiPart("companyId", String.valueOf(fixture.companyId()))
                .multiPart("companyEntitlementId", String.valueOf(fixture.companyEntitlementId()))
                .multiPart("timezone", "Europe/Berlin").multiPart("baseUrl", "https://bugzilla.example.com/")
                .post("/api/ticket-imports/bugzilla").then().statusCode(200)
                .body("sourceType", Matchers.equalTo("bugzilla")).body("rowCount", Matchers.equalTo(1))
                .body("createdCount", Matchers.equalTo(1)).body("skippedCount", Matchers.equalTo(0))
                .body("failedCount", Matchers.equalTo(0)).body("results[0].sourceSystem", Matchers.equalTo("bugzilla"))
                .body("results[0].sourceKey", Matchers.equalTo(bugId))
                .body("results[0].result", Matchers.equalTo("created"));

        Ticket ticket = importedTicket(bugId);
        Assertions.assertNotNull(ticket);
        Assertions.assertEquals("Imported, quoted bug", ticket.title);
        Assertions.assertEquals("Closed", ticket.status);
        Assertions.assertEquals("Feature", ticket.category.name);
        Assertions.assertEquals("https://bugzilla.example.com/show_bug.cgi?id=" + bugId, ticket.externalIssueLink);
        Assertions.assertEquals(fixture.companyId(), ticket.company.id);
        Assertions.assertEquals(fixture.companyEntitlementId(), ticket.companyEntitlement.id);
        Assertions.assertEquals("bugzilla-support@mnemosyne-systems.ai", ticket.requester.email);

        Message message = Message.find("ticket = ?1", ticket).firstResult();
        Assertions.assertNotNull(message);
        Assertions.assertTrue(message.body.startsWith("Summary: Imported, quoted bug"));
        Assertions.assertTrue(message.body.contains("Imported from Bugzilla bug #" + bugId));
        Assertions.assertTrue(message.body.contains("Product: Platform"));
        Assertions.assertTrue(message.body.contains("Resolution: FIXED"));
        LocalDateTime expectedDate = LocalDateTime.parse("2026-04-26T10:15:30").atZone(ZoneId.of("Europe/Berlin"))
                .withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        Assertions.assertEquals(expectedDate, message.date);
    }

    @Test
    @TestSecurity(user = "bugzilla-support", roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = "bugzilla-support@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "bugzilla-support") })
    void duplicateBugIdIsSkippedOnReimport() {
        ImportFixture fixture = fixture("bugzilla-duplicate");
        String bugId = "bz-dup-" + System.nanoTime();
        String csv = """
                bug_id,short_desc,bug_status
                %s,Duplicate bug,NEW
                """.formatted(bugId);

        submit(csv, fixture, "UTC").then().statusCode(200).body("createdCount", Matchers.equalTo(1));
        submit(csv, fixture, "UTC").then().statusCode(200).body("createdCount", Matchers.equalTo(0))
                .body("skippedCount", Matchers.equalTo(1)).body("results[0].result", Matchers.equalTo("skipped"));

        Assertions.assertEquals(1, ticketCountByBugId(bugId));
        Assertions.assertEquals(2, recordCountByBugId(bugId));
    }

    @Test
    @TestSecurity(user = "bugzilla-support", roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = "bugzilla-support@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "bugzilla-support") })
    void entitlementFromAnotherCompanyIsRejected() {
        ImportFixture selected = fixture("bugzilla-mismatch-selected");
        ImportFixture other = fixture("bugzilla-mismatch-other");
        String csv = """
                bug_id,short_desc,bug_status
                bz-mismatch,Rejected bug,NEW
                """;

        RestAssured.given().contentType("multipart/form-data")
                .multiPart("file", "bugs.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .multiPart("companyId", String.valueOf(selected.companyId()))
                .multiPart("companyEntitlementId", String.valueOf(other.companyEntitlementId()))
                .multiPart("timezone", "UTC").post("/api/ticket-imports/bugzilla").then().statusCode(400)
                .body(Matchers.containsString("Entitlement not found"));
    }

    @Test
    @TestSecurity(user = "bugzilla-support", roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = "bugzilla-support@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "bugzilla-support") })
    void invalidTimezoneIsRejected() {
        ImportFixture fixture = fixture("bugzilla-timezone");
        String csv = """
                bug_id,short_desc,bug_status
                bz-timezone,Timezone bug,NEW
                """;

        submit(csv, fixture, "Not/AZone").then().statusCode(400).body(Matchers.containsString("Timezone is invalid"));
    }

    @Test
    @TestSecurity(user = "bugzilla-support", roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = "bugzilla-support@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "bugzilla-support") })
    void missingCompanyAndEntitlementAreRejected() {
        ImportFixture fixture = fixture("bugzilla-required");
        String csv = """
                bug_id,short_desc,bug_status
                bz-required,Required bug,NEW
                """;

        RestAssured.given().contentType("multipart/form-data")
                .multiPart("file", "bugs.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .multiPart("timezone", "UTC").post("/api/ticket-imports/bugzilla").then().statusCode(400)
                .body(Matchers.containsString("Company is required"));

        RestAssured.given().contentType("multipart/form-data")
                .multiPart("file", "bugs.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .multiPart("companyId", String.valueOf(fixture.companyId())).multiPart("timezone", "UTC")
                .post("/api/ticket-imports/bugzilla").then().statusCode(400)
                .body(Matchers.containsString("Entitlement is required"));
    }

    @Test
    @TestSecurity(user = "bugzilla-support", roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = "bugzilla-support@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "bugzilla-support") })
    void bootstrapExposesFormatsCompaniesAndEntitlements() {
        ImportFixture fixture = fixture("bugzilla-bootstrap");
        ticketBootstrapService.invalidateAllCompanies();
        ticketBootstrapService.invalidateOrderedEntitlements(fixture.companyId());

        RestAssured.given().get("/api/ticket-imports/bootstrap?companyId=" + fixture.companyId()).then().statusCode(200)
                .body("defaultTimezone", Matchers.equalTo("UTC")).body("timezones", Matchers.hasItem("UTC"))
                .body("formats.id", Matchers.hasItems("csv", "bugzilla"))
                .body("formats[1].submitPath", Matchers.equalTo("/api/ticket-imports/bugzilla"))
                .body("companies.id", Matchers.hasItem(fixture.companyId().intValue()))
                .body("entitlements.id", Matchers.hasItem(fixture.companyEntitlementId().intValue()));
    }

    @Test
    @TestSecurity(user = "bugzilla-user", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = "bugzilla-user@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "bugzilla-user") })
    void nonSupportUserCannotImportBugzilla() {
        String csv = """
                bug_id,short_desc,bug_status
                bz-denied,Denied bug,NEW
                """;

        RestAssured.given().redirects().follow(false).contentType("multipart/form-data")
                .multiPart("file", "bugs.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .post("/api/ticket-imports/bugzilla").then().statusCode(403);
    }

    private io.restassured.response.Response submit(String csv, ImportFixture fixture, String timezone) {
        return RestAssured.given().contentType("multipart/form-data")
                .multiPart("file", "bugs.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .multiPart("companyId", String.valueOf(fixture.companyId()))
                .multiPart("companyEntitlementId", String.valueOf(fixture.companyEntitlementId()))
                .multiPart("timezone", timezone).post("/api/ticket-imports/bugzilla").then().extract().response();
    }

    private ImportFixture fixture(String suffix) {
        ensureUser("bugzilla-support", "bugzilla-support@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureCategory("Feature", "Feature requests", false);
        Entitlement entitlement = ensureEntitlement("Bugzilla Entitlement " + suffix, "Bugzilla import entitlement");
        Long companyId = ensureCompany("Bugzilla Company " + suffix);
        Company company = Company.findById(companyId);
        CompanyEntitlement companyEntitlement = ensureCompanyEntitlement(company, entitlement,
                ensureLevel("Bugzilla Level " + suffix, "Bugzilla import level", 60, "White"));
        Assertions.assertNotNull(companyEntitlement);
        return new ImportFixture(companyId, companyEntitlement.id);
    }

    @Transactional
    Ticket importedTicket(String bugId) {
        TicketImportRecord record = TicketImportRecord.find("sourceSystem = ?1 and sourceKey = ?2", "bugzilla", bugId)
                .firstResult();
        return record == null ? null : record.ticket;
    }

    @Transactional
    long ticketCountByBugId(String bugId) {
        return TicketImportRecord.count("sourceSystem = ?1 and sourceKey = ?2 and result = ?3", "bugzilla", bugId,
                TicketImportRecord.RESULT_CREATED);
    }

    @Transactional
    long recordCountByBugId(String bugId) {
        return TicketImportRecord.count("sourceSystem = ?1 and sourceKey = ?2", "bugzilla", bugId);
    }

    record ImportFixture(Long companyId, Long companyEntitlementId) {
    }
}
