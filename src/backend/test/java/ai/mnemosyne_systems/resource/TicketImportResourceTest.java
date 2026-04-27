/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Entitlement;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.TicketImportRecord;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TicketImportResourceTest extends AccessTestSupport {

    @Test
    void supportUserCanImportCsvTicketsAndMessages() {
        ImportFixture fixture = fixture("import-success");
        String cookie = login("import-support", "pass");
        String sourceKey = "csv-success-" + System.nanoTime();
        String csv = """
                source_key,title,company,entitlement,status,initial_message,requester_email,category,external_issue_link,created_at
                %s,"Imported, quoted ticket",%s,%s,Open,"First line
                second line",import-user@mnemosyne-systems.ai,Bug,https://example.test/issues/1,2026-04-26T10:15:30
                """
                .formatted(sourceKey, fixture.companyName(), fixture.entitlementName());

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).contentType("multipart/form-data")
                .multiPart("file", "tickets.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .post("/api/ticket-imports/csv").then().statusCode(200).body("rowCount", Matchers.equalTo(1))
                .body("createdCount", Matchers.equalTo(1)).body("skippedCount", Matchers.equalTo(0))
                .body("failedCount", Matchers.equalTo(0)).body("results[0].result", Matchers.equalTo("created"));

        Ticket ticket = importedTicket(sourceKey);
        Assertions.assertNotNull(ticket);
        Assertions.assertEquals("Imported, quoted ticket", ticket.title);
        Assertions.assertEquals("Open", ticket.status);
        Assertions.assertEquals("https://example.test/issues/1", ticket.externalIssueLink);
        Message message = Message.find("ticket = ?1", ticket).firstResult();
        Assertions.assertNotNull(message);
        Assertions.assertEquals("First line\nsecond line", message.body);
    }

    @Test
    void adminUserCanImportCsvTicketsAndMessages() {
        ImportFixture fixture = fixture("import-admin-success");
        ensureUser("import-admin", "import-admin@mnemosyne-systems.ai", User.TYPE_ADMIN, "pass");
        String cookie = login("import-admin", "pass");
        String sourceKey = "csv-admin-success-" + System.nanoTime();
        String csv = """
                source_key,title,company,entitlement,status,initial_message,requester_email,category
                %s,Imported by admin,%s,%s,Open,Imported from admin,import-user@mnemosyne-systems.ai,Bug
                """.formatted(sourceKey, fixture.companyName(), fixture.entitlementName());

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).contentType("multipart/form-data")
                .multiPart("file", "tickets.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .post("/api/ticket-imports/csv").then().statusCode(200).body("rowCount", Matchers.equalTo(1))
                .body("createdCount", Matchers.equalTo(1)).body("failedCount", Matchers.equalTo(0));

        Ticket ticket = importedTicket(sourceKey);
        Assertions.assertNotNull(ticket);
        Assertions.assertEquals("Imported by admin", ticket.title);
    }

    @Test
    void duplicateSourceKeyIsSkippedOnReimport() {
        ImportFixture fixture = fixture("import-duplicate");
        String cookie = login("import-support", "pass");
        String sourceKey = "csv-duplicate-" + System.nanoTime();
        String csv = """
                source_key,title,company,entitlement,status,initial_message
                %s,Duplicate import,%s,%s,Open,Initial duplicate import
                """.formatted(sourceKey, fixture.companyName(), fixture.entitlementName());

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).contentType("multipart/form-data")
                .multiPart("file", "tickets.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .post("/api/ticket-imports/csv").then().statusCode(200).body("createdCount", Matchers.equalTo(1));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).contentType("multipart/form-data")
                .multiPart("file", "tickets.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .post("/api/ticket-imports/csv").then().statusCode(200).body("createdCount", Matchers.equalTo(0))
                .body("skippedCount", Matchers.equalTo(1)).body("results[0].result", Matchers.equalTo("skipped"));

        Assertions.assertEquals(1, ticketCountBySourceKey(sourceKey));
        Assertions.assertEquals(2, importRecordCountBySourceKey(sourceKey));
    }

    @Test
    void invalidRowsReturnRowLevelErrors() {
        fixture("import-invalid");
        String cookie = login("import-support", "pass");
        String sourceKey = "csv-invalid-" + System.nanoTime();
        String csv = """
                source_key,title,company,entitlement,status,initial_message,category
                %s,Missing company,Does Not Exist,Starter,Open,Message,Bug
                """.formatted(sourceKey);

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).contentType("multipart/form-data")
                .multiPart("file", "tickets.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .post("/api/ticket-imports/csv").then().statusCode(200).body("createdCount", Matchers.equalTo(0))
                .body("failedCount", Matchers.equalTo(1)).body("results[0].result", Matchers.equalTo("failed"))
                .body("results[0].errorMessage", Matchers.containsString("Company not found"));

        Assertions.assertNull(importedTicket(sourceKey));
    }

    @Test
    void missingSourceKeyReturnsRowLevelError() {
        ImportFixture fixture = fixture("import-missing-source-key");
        String cookie = login("import-support", "pass");
        String csv = """
                source_key,title,company,entitlement,status,initial_message
                ,Missing source key,%s,%s,Open,Message
                """.formatted(fixture.companyName(), fixture.entitlementName());

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).contentType("multipart/form-data")
                .multiPart("file", "tickets.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .post("/api/ticket-imports/csv").then().statusCode(200).body("createdCount", Matchers.equalTo(0))
                .body("failedCount", Matchers.equalTo(1)).body("results[0].result", Matchers.equalTo("failed"))
                .body("results[0].errorMessage", Matchers.containsString("Source key is required"));
    }

    @Test
    void nonSupportUserCannotImportCsv() {
        fixture("import-access");
        String cookie = login("import-user", "pass");
        String csv = """
                source_key,title,company,entitlement,status,initial_message
                cannot-import,Denied,Company,Starter,Open,Denied
                """;

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType("multipart/form-data")
                .multiPart("file", "tickets.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .post("/api/ticket-imports/csv").then().statusCode(303);
    }

    @Test
    void missingRequiredHeaderFailsClearly() {
        ensureUser("import-support", "import-support@mnemosyne-systems.ai", User.TYPE_SUPPORT, "pass");
        String cookie = login("import-support", "pass");
        String csv = """
                source_key,title,company,status,initial_message
                missing-entitlement,Missing entitlement,Company,Open,Message
                """;

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).contentType("multipart/form-data")
                .multiPart("file", "tickets.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .post("/api/ticket-imports/csv").then().statusCode(400).body(Matchers.containsString("entitlement"));
    }

    private ImportFixture fixture(String suffix) {
        ensureUser("import-support", "import-support@mnemosyne-systems.ai", User.TYPE_SUPPORT, "pass");
        ensureUser("import-user", "import-user@mnemosyne-systems.ai", User.TYPE_USER, "pass");
        ensureCategory("Bug", "Defects", false);
        Entitlement entitlement = ensureEntitlement("Starter Import " + suffix, "Import entitlement");
        Long companyId = ensureCompany("Import Company " + suffix);
        Company company = Company.findById(companyId);
        CompanyEntitlement companyEntitlement = ensureCompanyEntitlement(company, entitlement,
                ensureLevel("Normal Import " + suffix, "Normal import level", 60, "White"));
        Assertions.assertNotNull(companyEntitlement);
        return new ImportFixture(company.name, entitlement.name);
    }

    @Transactional
    Ticket importedTicket(String sourceKey) {
        TicketImportRecord record = TicketImportRecord.find("sourceSystem = ?1 and sourceKey = ?2", "csv", sourceKey)
                .firstResult();
        return record == null ? null : record.ticket;
    }

    @Transactional
    long ticketCountBySourceKey(String sourceKey) {
        return TicketImportRecord.count("sourceSystem = ?1 and sourceKey = ?2 and result = ?3", "csv", sourceKey,
                TicketImportRecord.RESULT_CREATED);
    }

    @Transactional
    long importRecordCountBySourceKey(String sourceKey) {
        return TicketImportRecord.count("sourceSystem = ?1 and sourceKey = ?2", "csv", sourceKey);
    }

    record ImportFixture(String companyName, String entitlementName) {
    }
}
