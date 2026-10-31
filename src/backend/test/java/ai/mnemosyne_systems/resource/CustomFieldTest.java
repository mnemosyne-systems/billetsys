/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.CustomField;
import ai.mnemosyne_systems.model.CustomFieldOption;
import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.TicketCustomFieldValue;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.Version;
import ai.mnemosyne_systems.service.TicketCustomFieldService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CustomFieldTest extends AccessTestSupport {

    private static final String ADMIN = "cf-admin@mnemosyne-systems.ai";
    private static final String SUPPORT = "cf-support@mnemosyne-systems.ai";
    private static final String SUPERUSER = "cf-superuser@mnemosyne-systems.ai";
    private static final String TAM = "cf-tam@mnemosyne-systems.ai";
    private static final String USER = "cf-user@mnemosyne-systems.ai";
    private static final String EXTERNAL = "cf-external@mnemosyne-systems.ai";

    @Inject
    TicketCustomFieldService ticketCustomFieldService;

    @Test
    @TestSecurity(user = ADMIN, roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = ADMIN), @Claim(key = "sub", value = "cf-admin") })
    void adminManagesDefinitionsAndNonAdminIsRejected() {
        ensureUser("cf-admin", ADMIN, User.TYPE_ADMIN);
        Long fieldId = createDefinition("CF Admin Field", "TEXT", false, "Help text", 5, null);
        RestAssured.given().get("/api/custom-fields/" + fieldId).then().statusCode(200)
                .body("name", Matchers.equalTo("CF Admin Field")).body("fieldType", Matchers.equalTo("TEXT"))
                .body("required", Matchers.equalTo(false)).body("active", Matchers.equalTo(true))
                .body("sortOrder", Matchers.equalTo(5)).body("description", Matchers.equalTo("Help text"));

        updateDefinition(fieldId, "CF Admin Field", "TEXT", true, "Updated help", 7, null, true);
        RestAssured.given().get("/api/custom-fields/" + fieldId).then().statusCode(200)
                .body("required", Matchers.equalTo(true)).body("sortOrder", Matchers.equalTo(7))
                .body("description", Matchers.equalTo("Updated help"));

        deactivateDefinition(fieldId);
        RestAssured.given().get("/api/custom-fields/" + fieldId).then().statusCode(200).body("active",
                Matchers.equalTo(false));
        updateDefinition(fieldId, "CF Admin Field", "TEXT", true, "Updated help", 7, null, true);
        RestAssured.given().get("/api/custom-fields/" + fieldId).then().statusCode(200).body("active",
                Matchers.equalTo(true));
        deactivateDefinition(fieldId);
    }

    @Test
    @TestSecurity(user = SUPPORT, roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPPORT), @Claim(key = "sub", value = "cf-support") })
    void nonAdminCannotManageDefinitions() {
        ensureUser("cf-support", SUPPORT, User.TYPE_SUPPORT);
        RestAssured.given().get("/api/custom-fields").then().statusCode(403);
        RestAssured.given().redirects().follow(false).multiPart("name", "CF Hacked").multiPart("fieldType", "TEXT")
                .post("/custom-fields").then().statusCode(403);
        RestAssured.given().redirects().follow(false).post("/custom-fields/1/deactivate").then().statusCode(403);
        RestAssured.given().redirects().follow(false).post("/custom-fields/1/delete").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = ADMIN, roles = { "admin", "support" })
    @JwtSecurity(claims = { @Claim(key = "email", value = ADMIN), @Claim(key = "sub", value = "cf-admin") })
    void adminDeletesFieldAndItsTicketValues() {
        ensureUser("cf-admin", ADMIN, User.TYPE_ADMIN);
        ensureUser("cf-support", SUPPORT, User.TYPE_SUPPORT);
        Fixture fixture = fixture("Delete Definition");
        Long fieldId = createDefinition("CF Delete Definition", "SELECT", false, null, 0, "One\nTwo");
        Long ticketId = createTicket(fixture, fields(fieldId, "One"));

        RestAssured.given().redirects().follow(false).post("/custom-fields/" + fieldId + "/delete").then()
                .statusCode(303);
        RestAssured.given().get("/api/custom-fields/" + fieldId).then().statusCode(404);
        RestAssured.given().get("/api/support/tickets/" + ticketId).then().statusCode(200).body("customFields.name",
                Matchers.not(Matchers.hasItem("CF Delete Definition")));
        cleanup(fixture, ticketId);
    }

    @Test
    @TestSecurity(user = ADMIN, roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = ADMIN), @Claim(key = "sub", value = "cf-admin") })
    void duplicateNamesAndTypeChangesAreRejected() {
        ensureUser("cf-admin", ADMIN, User.TYPE_ADMIN);
        Long fieldId = createDefinition("CF Duplicate Name", "TEXT", false, null, 0, null);
        RestAssured.given().redirects().follow(false).multiPart("name", "cf duplicate name")
                .multiPart("fieldType", "TEXT").post("/custom-fields").then().statusCode(400)
                .body(Matchers.containsString("already exists"));
        RestAssured.given().redirects().follow(false).multiPart("name", "CF Duplicate Name")
                .multiPart("fieldType", "NUMBER").post("/custom-fields/" + fieldId).then().statusCode(400)
                .body(Matchers.containsString("Field type cannot be changed"));
        deactivateDefinition(fieldId);
    }

    @Test
    @TestSecurity(user = ADMIN, roles = { "admin", "support" })
    @JwtSecurity(claims = { @Claim(key = "email", value = ADMIN), @Claim(key = "sub", value = "cf-admin") })
    void supportSeesFieldsAndRoundTripsValues() {
        ensureUser("cf-admin", ADMIN, User.TYPE_ADMIN);
        ensureUser("cf-support", SUPPORT, User.TYPE_SUPPORT);
        Fixture fixture = fixture("Round Trip");
        Long textId = createDefinition("CF Text", "TEXT", false, "Text help", 1, null);
        Long numberId = createDefinition("CF Number", "NUMBER", false, "Number help", 2, null);
        Long dateId = createDefinition("CF Date", "DATE", false, "Date help", 3, null);
        Long selectId = createDefinition("CF Select", "SELECT", false, "Select help", 4, "Alpha\nBeta");

        RestAssured.given().get("/api/support/tickets/bootstrap").then().statusCode(200).body("customFields.name",
                Matchers.hasItems("CF Text", "CF Number", "CF Date", "CF Select"));

        String createValues = fields(
                Map.of(textId, "hello", numberId, "123.45", dateId, "2026-10-31", selectId, "Beta"));
        Long ticketId = createTicket(fixture, createValues);
        RestAssured.given().get("/api/support/tickets/" + ticketId).then().statusCode(200)
                .body("editableCustomFields", Matchers.equalTo(true))
                .body("customFields.find { it.name == 'CF Text' }.value", Matchers.equalTo("hello"))
                .body("customFields.find { it.name == 'CF Number' }.value", Matchers.equalTo("123.45"))
                .body("customFields.find { it.name == 'CF Date' }.value", Matchers.equalTo("2026-10-31"))
                .body("customFields.find { it.name == 'CF Select' }.value", Matchers.equalTo("Beta"))
                .body("customFields.find { it.name == 'CF Text' }.description", Matchers.equalTo("Text help"));

        String updateValues = fields(Map.of(textId, "updated", numberId, "7", dateId, "2026-11-01", selectId, "Alpha"));
        supportUpdate(ticketId, fixture, "Assigned", updateValues);
        RestAssured.given().get("/api/support/tickets/" + ticketId).then().statusCode(200)
                .body("customFields.find { it.name == 'CF Text' }.value", Matchers.equalTo("updated"))
                .body("customFields.find { it.name == 'CF Number' }.value", Matchers.equalTo("7"))
                .body("customFields.find { it.name == 'CF Date' }.value", Matchers.equalTo("2026-11-01"))
                .body("customFields.find { it.name == 'CF Select' }.value", Matchers.equalTo("Alpha"));
        cleanup(fixture, ticketId, textId, numberId, dateId, selectId);
    }

    @Test
    @TestSecurity(user = ADMIN, roles = { "admin", "support" })
    @JwtSecurity(claims = { @Claim(key = "email", value = ADMIN), @Claim(key = "sub", value = "cf-admin") })
    void requiredFieldsAreEnforcedOnCreateAndCannotBeCleared() {
        ensureUser("cf-admin", ADMIN, User.TYPE_ADMIN);
        ensureUser("cf-support", SUPPORT, User.TYPE_SUPPORT);
        Fixture fixture = fixture("Required");
        Long fieldId = createDefinition("CF Required", "TEXT", true, "Required help", 0, null);

        postTicketRaw(fixture, null).then().statusCode(400).body(Matchers.containsString("is required"));
        postTicketRaw(fixture, fields(fieldId, " ")).then().statusCode(400)
                .body(Matchers.containsString("is required"));
        Long ticketId = createTicket(fixture, fields(fieldId, "value"));
        supportUpdateRaw(ticketId, fixture, "Assigned", fields(fieldId, " ")).then().statusCode(400)
                .body(Matchers.containsString("is required"));
        supportUpdate(ticketId, fixture, "Assigned", fields(fieldId, "value2"));
        Assertions.assertEquals("value2", detailValue(ticketId, "CF Required"));

        Ticket oldTicket = ensureTicket(fixture.companyId());
        supportUpdateSeededTicket(oldTicket.id, "Assigned", null);
        cleanup(fixture, ticketId, fieldId);
    }

    @Test
    @TestSecurity(user = ADMIN, roles = { "admin", "support" })
    @JwtSecurity(claims = { @Claim(key = "email", value = ADMIN), @Claim(key = "sub", value = "cf-admin") })
    void invalidValuesAreRejectedWithBadRequest() {
        ensureUser("cf-admin", ADMIN, User.TYPE_ADMIN);
        ensureUser("cf-support", SUPPORT, User.TYPE_SUPPORT);
        Fixture fixture = fixture("Validation");
        Long numberId = createDefinition("CF Bad Number", "NUMBER", false, null, 1, null);
        Long dateId = createDefinition("CF Bad Date", "DATE", false, null, 2, null);
        Long selectId = createDefinition("CF Bad Select", "SELECT", false, null, 3, "One\nTwo");

        postTicketRaw(fixture, "{not-json").then().statusCode(400).body(Matchers.containsString("must be valid JSON"));
        postTicketRaw(fixture, "{\"abc\":\"x\"}").then().statusCode(400)
                .body(Matchers.containsString("Invalid custom field id"));
        postTicketRaw(fixture, fields(999999L, "x")).then().statusCode(400)
                .body(Matchers.containsString("Unknown or inactive custom field"));
        postTicketRaw(fixture, fields(numberId, "not-a-number")).then().statusCode(400)
                .body(Matchers.containsString("must be a number"));
        postTicketRaw(fixture, fields(numberId, "123456789012345678901234567890")).then().statusCode(400)
                .body(Matchers.containsString("too large"));
        postTicketRaw(fixture, fields(numberId, "1.12345678901")).then().statusCode(400)
                .body(Matchers.containsString("at most 10 decimal places"));
        postTicketRaw(fixture, fields(dateId, "2026-13-40")).then().statusCode(400)
                .body(Matchers.containsString("valid date"));
        postTicketRaw(fixture, fields(selectId, "Three")).then().statusCode(400)
                .body(Matchers.containsString("must be one of"));
        cleanup(fixture, null, numberId, dateId, selectId);
    }

    @Test
    @TestSecurity(user = SUPPORT, roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPPORT), @Claim(key = "sub", value = "cf-support") })
    void closedTicketRejectsValueChanges() {
        ensureUser("cf-support", SUPPORT, User.TYPE_SUPPORT);
        Fixture fixture = fixture("Closed");
        Long fieldId = seedDefinition("CF Closed", "TEXT", false, null, 0, List.of());
        Long ticketId = createTicket(fixture, fields(fieldId, "locked"));
        supportUpdate(ticketId, fixture, "Closed", fields(fieldId, "locked"));
        supportUpdateRaw(ticketId, fixture, "Closed", fields(fieldId, "changed")).then().statusCode(400)
                .body(Matchers.containsString("closed ticket"));
        RestAssured.given().get("/api/support/tickets/" + ticketId).then().statusCode(200)
                .body("editableCustomFields", Matchers.equalTo(false))
                .body("customFields.find { it.name == 'CF Closed' }.value", Matchers.equalTo("locked"));
        supportUpdate(ticketId, fixture, "Assigned", fields(fieldId, "reopened"));
        Assertions.assertEquals("reopened", detailValue(ticketId, "CF Closed"));
        cleanup(fixture, ticketId, fieldId);
    }

    @Test
    @TestSecurity(user = ADMIN, roles = { "admin", "support" })
    @JwtSecurity(claims = { @Claim(key = "email", value = ADMIN), @Claim(key = "sub", value = "cf-admin") })
    void deactivatedFieldIsHiddenButValueRemainsVisible() {
        ensureUser("cf-admin", ADMIN, User.TYPE_ADMIN);
        ensureUser("cf-support", SUPPORT, User.TYPE_SUPPORT);
        Fixture fixture = fixture("Deactivated");
        Long fieldId = createDefinition("CF Deactivated", "TEXT", false, null, 0, null);
        Long ticketId = createTicket(fixture, fields(fieldId, "kept"));

        deactivateDefinition(fieldId);
        RestAssured.given().get("/api/support/tickets/bootstrap").then().statusCode(200).body("customFields.name",
                Matchers.not(Matchers.hasItem("CF Deactivated")));
        supportUpdateRaw(ticketId, fixture, "Assigned", fields(fieldId, "new")).then().statusCode(400)
                .body(Matchers.containsString("Unknown or inactive custom field"));
        RestAssured.given().get("/api/support/tickets/" + ticketId).then().statusCode(200)
                .body("customFields.find { it.name == 'CF Deactivated' }.value", Matchers.equalTo("kept"));
        cleanup(fixture, ticketId);
    }

    @Test
    @TestSecurity(user = ADMIN, roles = { "admin", "support" })
    @JwtSecurity(claims = { @Claim(key = "email", value = ADMIN), @Claim(key = "sub", value = "cf-admin") })
    void selectOptionInUseCannotBeRemovedOrRenamed() {
        ensureUser("cf-admin", ADMIN, User.TYPE_ADMIN);
        ensureUser("cf-support", SUPPORT, User.TYPE_SUPPORT);
        Fixture fixture = fixture("Select In Use");
        Long fieldId = createDefinition("CF Select In Use", "SELECT", false, null, 0, "Alpha\nBeta");
        Long ticketId = createTicket(fixture, fields(fieldId, "Alpha"));

        RestAssured.given().redirects().follow(false).multiPart("name", "CF Select In Use")
                .multiPart("fieldType", "SELECT").multiPart("required", "false").multiPart("description", "")
                .multiPart("sortOrder", "0").multiPart("active", "true").multiPart("options", "Beta")
                .post("/custom-fields/" + fieldId).then().statusCode(400)
                .body(Matchers.containsString("in use and cannot be removed or renamed"));

        updateDefinition(fieldId, "CF Select In Use", "SELECT", false, null, 0, "Alpha\nGamma", true);
        RestAssured.given().redirects().follow(false).multiPart("name", "CF Select In Use")
                .multiPart("fieldType", "SELECT").multiPart("required", "false").multiPart("description", "")
                .multiPart("sortOrder", "0").multiPart("active", "true").multiPart("options", "Alpha2\nGamma")
                .post("/custom-fields/" + fieldId).then().statusCode(400)
                .body(Matchers.containsString("in use and cannot be removed or renamed"));

        Assertions.assertEquals("Alpha", detailValue(ticketId, "CF Select In Use"));
        cleanup(fixture, ticketId, fieldId);
    }

    @Test
    @TestSecurity(user = SUPPORT, roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPPORT), @Claim(key = "sub", value = "cf-support") })
    void ticketDeletionRemovesValues() {
        ensureUser("cf-support", SUPPORT, User.TYPE_SUPPORT);
        Fixture fixture = fixture("Delete");
        Long fieldId = seedDefinition("CF Delete", "TEXT", false, null, 0, List.of());
        Long ticketId = createTicket(fixture, fields(fieldId, "delete me"));
        RestAssured.given().redirects().follow(false).post("/tickets/" + ticketId + "/delete").then().statusCode(303);
        RestAssured.given().get("/api/support/tickets/" + ticketId).then().statusCode(404);
        cleanup(fixture, null, fieldId);
    }

    @Test
    @TestSecurity(user = SUPERUSER, roles = "superuser")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPERUSER), @Claim(key = "sub", value = "cf-superuser") })
    void superuserSeesValuesReadOnly() {
        ensureUser("cf-superuser", SUPERUSER, User.TYPE_SUPERUSER);
        RoleFixture fixture = roleFixture("Super", SUPERUSER);
        RestAssured.given().get("/api/superuser/tickets/" + fixture.ticketId()).then().statusCode(200)
                .body("editableCustomFields", Matchers.equalTo(false))
                .body("customFields.find { it.name == '" + fixture.fieldName() + "' }.value",
                        Matchers.equalTo("visible"));
        var update = RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("title", "Role write attempt")
                .formParam("customFields", fields(fixture.fieldId(), "hacked"));
        Long versionId = ticketVersionId(fixture.ticketId());
        if (versionId != null) {
            update.formParam("affectsVersionId", versionId);
        }
        update.post("/superuser/tickets/" + fixture.ticketId()).then().statusCode(303);
        Assertions.assertEquals("visible",
                roleDetailValue("/api/superuser/tickets", fixture.ticketId(), fixture.fieldName()));
        cleanupRole(fixture);
    }

    @Test
    @TestSecurity(user = TAM, roles = "tam")
    @JwtSecurity(claims = { @Claim(key = "email", value = TAM), @Claim(key = "sub", value = "cf-tam") })
    void tamSeesValuesReadOnly() {
        ensureUser("cf-tam", TAM, User.TYPE_TAM);
        RoleFixture fixture = roleFixture("Tam", TAM);
        RestAssured.given().get("/api/user/tickets/" + fixture.ticketId()).then().statusCode(200)
                .body("editableCustomFields", Matchers.equalTo(false))
                .body("customFields.find { it.name == '" + fixture.fieldName() + "' }.value",
                        Matchers.equalTo("visible"));
        var update = RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("title", "Role write attempt")
                .formParam("customFields", fields(fixture.fieldId(), "hacked"));
        Long versionId = ticketVersionId(fixture.ticketId());
        if (versionId != null) {
            update.formParam("affectsVersionId", versionId);
        }
        update.post("/user/tickets/" + fixture.ticketId()).then().statusCode(303);
        Assertions.assertEquals("visible",
                roleDetailValue("/api/user/tickets", fixture.ticketId(), fixture.fieldName()));
        cleanupRole(fixture);
    }

    @Test
    @TestSecurity(user = USER, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = USER), @Claim(key = "sub", value = "cf-user") })
    void userBootstrapAndCreateIncludeCustomFields() {
        ensureUser("cf-user", USER, User.TYPE_USER);
        Fixture fixture = fixture("User Create");
        ensureCompanyUsers(fixture.companyId(), USER);
        Long fieldId = seedDefinition("CF User Create", "TEXT", false, "User help", 0, List.of());

        RestAssured.given().get("/api/user/tickets/bootstrap").then().statusCode(200).body("customFields.name",
                Matchers.hasItem("CF User Create"));

        Response response = RestAssured.given().redirects().follow(false).contentType(ContentType.MULTIPART)
                .multiPart("status", "Open").multiPart("title", "User custom field ticket")
                .multiPart("message", "User message").multiPart("companyId", fixture.companyId())
                .multiPart("companyEntitlementId", fixture.entitlementId())
                .multiPart("categoryId", Category.findDefault().id).multiPart("affectsVersionId", fixture.versionId())
                .multiPart("customFields", fields(fieldId, "user value")).post("/user/tickets");
        response.then().statusCode(303);
        Long ticketId = ticketIdFrom(response);

        RestAssured.given().get("/api/user/tickets/" + ticketId).then().statusCode(200)
                .body("editableCustomFields", Matchers.equalTo(false))
                .body("customFields.find { it.name == 'CF User Create' }.value", Matchers.equalTo("user value"));
        cleanup(fixture, ticketId, fieldId);
    }

    @Test
    @TestSecurity(user = SUPERUSER, roles = "superuser")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPERUSER), @Claim(key = "sub", value = "cf-superuser") })
    void superuserBootstrapAndCreateIncludeCustomFields() {
        ensureUser("cf-superuser", SUPERUSER, User.TYPE_SUPERUSER);
        Fixture fixture = fixture("Superuser Create");
        ensureCompanyUsers(fixture.companyId(), SUPERUSER);
        Long fieldId = seedDefinition("CF Superuser Create", "TEXT", false, "Superuser help", 0, List.of());

        RestAssured.given().get("/api/superuser/tickets/bootstrap").then().statusCode(200).body("customFields.name",
                Matchers.hasItem("CF Superuser Create"));

        Response response = RestAssured.given().redirects().follow(false).contentType(ContentType.MULTIPART)
                .multiPart("status", "Open").multiPart("title", "Superuser custom field ticket")
                .multiPart("message", "Superuser message").multiPart("companyId", fixture.companyId())
                .multiPart("companyEntitlementId", fixture.entitlementId())
                .multiPart("categoryId", Category.findDefault().id).multiPart("affectsVersionId", fixture.versionId())
                .multiPart("customFields", fields(fieldId, "superuser value")).post("/superuser/tickets");
        response.then().statusCode(303);
        Long ticketId = ticketIdFrom(response);

        RestAssured.given().get("/api/superuser/tickets/" + ticketId).then().statusCode(200)
                .body("editableCustomFields", Matchers.equalTo(false))
                .body("customFields.find { it.name == 'CF Superuser Create' }.value",
                        Matchers.equalTo("superuser value"));
        cleanup(fixture, ticketId, fieldId);
    }

    @Test
    @TestSecurity(user = USER, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = USER), @Claim(key = "sub", value = "cf-user") })
    void userSeesValuesReadOnly() {
        ensureUser("cf-user", USER, User.TYPE_USER);
        RoleFixture fixture = roleFixture("User", USER);
        RestAssured.given().get("/api/user/tickets/" + fixture.ticketId()).then().statusCode(200)
                .body("editableCustomFields", Matchers.equalTo(false))
                .body("customFields.find { it.name == '" + fixture.fieldName() + "' }.value",
                        Matchers.equalTo("visible"));
        var update = RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("title", "Role write attempt")
                .formParam("customFields", fields(fixture.fieldId(), "hacked"));
        Long versionId = ticketVersionId(fixture.ticketId());
        if (versionId != null) {
            update.formParam("affectsVersionId", versionId);
        }
        update.post("/user/tickets/" + fixture.ticketId()).then().statusCode(303);
        Assertions.assertEquals("visible",
                roleDetailValue("/api/user/tickets", fixture.ticketId(), fixture.fieldName()));
        cleanupRole(fixture);
    }

    @Test
    @TestSecurity(user = EXTERNAL, roles = "external")
    @JwtSecurity(claims = { @Claim(key = "email", value = EXTERNAL), @Claim(key = "sub", value = "cf-external") })
    void externalHasNoTicketAccess() {
        ensureUser("cf-external", EXTERNAL, User.TYPE_EXTERNAL);
        RestAssured.given().get("/api/support/tickets/1").then().statusCode(403);
        RestAssured.given().get("/api/user/tickets/1").then().statusCode(403);
    }

    private Fixture fixture(String suffix) {
        Long companyId = ensureCompany("CF Company " + suffix);
        ai.mnemosyne_systems.model.Entitlement entitlement = ensureEntitlement("CF Entitlement " + suffix,
                "Custom field test");
        CompanyEntitlement entry = ensureCompanyEntitlement(companyId, entitlement);
        return new Fixture(companyId, entry.id, firstVersionId(entry.id));
    }

    private RoleFixture roleFixture(String suffix, String memberEmail) {
        ensureUser("cf-support", SUPPORT, User.TYPE_SUPPORT);
        ensureUser("cf-user", USER, User.TYPE_USER);
        Fixture fixture = fixture(suffix);
        ensureCompanyUsers(fixture.companyId(), memberEmail);
        String fieldName = "CF Role Field " + suffix;
        Long fieldId = seedDefinition(fieldName, "TEXT", false, null, 0, List.of());
        Ticket ticket = ensureTicket(fixture.companyId());
        assignRequester(ticket.id, memberEmail);
        setCustomValue(ticket.id, fieldId, "visible", null, null);
        return new RoleFixture(fixture, ticket.id, fieldId, fieldName);
    }

    private void cleanup(Fixture fixture, Long ticketId, Long... fieldIds) {
        if (ticketId != null) {
            deleteTicketDirect(ticketId);
        }
        for (Long fieldId : fieldIds) {
            deactivateDefinitionDirect(fieldId);
        }
        ticketCustomFieldService.invalidateDefinitions();
    }

    private void cleanupRole(RoleFixture fixture) {
        cleanup(fixture.fixture(), fixture.ticketId(), fixture.fieldId());
    }

    private String detailValue(Long ticketId, String name) {
        return RestAssured.given().get("/api/support/tickets/" + ticketId).then().statusCode(200).extract().jsonPath()
                .getString("customFields.find { it.name == '" + name + "' }.value");
    }

    private String roleDetailValue(String detailPath, Long ticketId, String name) {
        return RestAssured.given().get(detailPath + "/" + ticketId).then().statusCode(200).extract().jsonPath()
                .getString("customFields.find { it.name == '" + name + "' }.value");
    }

    private Long createDefinition(String name, String type, boolean required, String description, int sortOrder,
            String options) {
        var request = RestAssured.given().redirects().follow(false).multiPart("name", name).multiPart("fieldType", type)
                .multiPart("required", Boolean.toString(required))
                .multiPart("description", description == null ? "" : description)
                .multiPart("sortOrder", Integer.toString(sortOrder)).multiPart("active", "true");
        if (options != null) {
            request.multiPart("options", options);
        }
        request.post("/custom-fields").then().statusCode(303);
        return definitionId(name);
    }

    private void updateDefinition(Long fieldId, String name, String type, boolean required, String description,
            int sortOrder, String options, boolean active) {
        var request = RestAssured.given().redirects().follow(false).multiPart("name", name).multiPart("fieldType", type)
                .multiPart("required", Boolean.toString(required))
                .multiPart("description", description == null ? "" : description)
                .multiPart("sortOrder", Integer.toString(sortOrder)).multiPart("active", Boolean.toString(active));
        if (options != null) {
            request.multiPart("options", options);
        }
        request.post("/custom-fields/" + fieldId).then().statusCode(303);
    }

    private void deactivateDefinition(Long fieldId) {
        RestAssured.given().redirects().follow(false).post("/custom-fields/" + fieldId + "/deactivate").then()
                .statusCode(303);
    }

    private Long definitionId(String name) {
        return RestAssured.given().get("/api/custom-fields").then().statusCode(200).extract().jsonPath()
                .getLong("items.find { it.name == '" + name + "' }.id");
    }

    private Response postTicketRaw(Fixture fixture, String customFieldsJson) {
        var request = RestAssured.given().redirects().follow(false).multiPart("status", "Open")
                .multiPart("title", "CF ticket").multiPart("message", "Custom field body")
                .multiPart("companyId", fixture.companyId()).multiPart("companyEntitlementId", fixture.entitlementId());
        if (fixture.versionId() != null) {
            request.multiPart("affectsVersionId", fixture.versionId());
        }
        if (customFieldsJson != null) {
            request.multiPart("customFields", customFieldsJson);
        }
        return request.post("/support/tickets");
    }

    private Long createTicket(Fixture fixture, String customFieldsJson) {
        Response response = postTicketRaw(fixture, customFieldsJson);
        response.then().statusCode(303);
        return ticketIdFrom(response);
    }

    private Response supportUpdateRaw(Long ticketId, Fixture fixture, String status, String customFieldsJson) {
        var request = RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("title", "CF updated").formParam("status", status)
                .formParam("companyId", fixture.companyId()).formParam("companyEntitlementId", fixture.entitlementId());
        if (fixture.versionId() != null) {
            request.formParam("affectsVersionId", fixture.versionId());
        }
        if (customFieldsJson != null) {
            request.formParam("customFields", customFieldsJson);
        }
        return request.post("/support/tickets/" + ticketId);
    }

    private void supportUpdate(Long ticketId, Fixture fixture, String status, String customFieldsJson) {
        supportUpdateRaw(ticketId, fixture, status, customFieldsJson).then().statusCode(303);
    }

    private void supportUpdateSeededTicket(Long ticketId, String status, String customFieldsJson) {
        var request = RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("title", "CF old ticket").formParam("status", status)
                .formParam("companyId", ticketCompanyId(ticketId))
                .formParam("companyEntitlementId", ticketEntitlementId(ticketId));
        Long versionId = ticketVersionId(ticketId);
        if (versionId != null) {
            request.formParam("affectsVersionId", versionId);
        }
        if (customFieldsJson != null) {
            request.formParam("customFields", customFieldsJson);
        }
        request.post("/support/tickets/" + ticketId).then().statusCode(303);
    }

    private Long ticketIdFrom(Response response) {
        String location = response.header("Location");
        Assertions.assertNotNull(location);
        return Long
                .valueOf(URI.create(location).getPath().substring(URI.create(location).getPath().lastIndexOf('/') + 1));
    }

    private String fields(Long fieldId, String value) {
        Map<Long, String> values = new LinkedHashMap<>();
        values.put(fieldId, value);
        return fields(values);
    }

    private String fields(Map<Long, String> values) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<Long, String> entry : values.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append('"').append(entry.getKey()).append("\":\"").append(escape(entry.getValue())).append('"');
        }
        return builder.append('}').toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Transactional
    Long seedDefinition(String name, String type, boolean required, String description, int sortOrder,
            List<String> options) {
        CustomField field = new CustomField();
        field.name = name;
        field.nameKey = TicketCustomFieldService.normalizeNameKey(name);
        field.fieldType = type;
        field.required = required;
        field.description = description;
        field.sortOrder = sortOrder;
        field.persist();
        int index = 0;
        for (String label : options) {
            CustomFieldOption option = new CustomFieldOption();
            option.customField = field;
            option.label = label;
            option.sortOrder = index++;
            option.persist();
            field.options.add(option);
        }
        ticketCustomFieldService.invalidateDefinitions();
        return field.id;
    }

    @Transactional
    void assignRequester(Long ticketId, String email) {
        Ticket ticket = Ticket.findById(ticketId);
        ticket.requester = User.find("email", email).firstResult();
    }

    @Transactional
    void setCustomValue(Long ticketId, Long fieldId, String text, BigDecimal number, LocalDate date) {
        Ticket ticket = Ticket.findById(ticketId);
        CustomField field = CustomField.findById(fieldId);
        TicketCustomFieldValue value = TicketCustomFieldValue.find("ticket = ?1 and customField = ?2", ticket, field)
                .firstResult();
        if (value == null) {
            value = new TicketCustomFieldValue();
            value.ticket = ticket;
            value.customField = field;
            value.persist();
            ticket.customFieldValues.add(value);
        }
        value.textValue = text;
        value.numberValue = number;
        value.dateValue = date;
    }

    @Transactional
    void deactivateDefinitionDirect(Long fieldId) {
        CustomField field = fieldId == null ? null : CustomField.findById(fieldId);
        if (field != null) {
            field.active = false;
        }
    }

    @Transactional
    void deleteTicketDirect(Long ticketId) {
        Ticket ticket = Ticket.findById(ticketId);
        if (ticket != null) {
            ticket.delete();
        }
    }

    @Transactional
    Long firstVersionId(Long companyEntitlementId) {
        CompanyEntitlement entry = CompanyEntitlement.findById(companyEntitlementId);
        Version version = Version.find("entitlement = ?1 order by date asc, id asc", entry.entitlement).firstResult();
        return version == null ? null : version.id;
    }

    @Transactional
    Long ticketCompanyId(Long ticketId) {
        Ticket ticket = Ticket.findById(ticketId);
        return ticket.company.id;
    }

    @Transactional
    Long ticketEntitlementId(Long ticketId) {
        Ticket ticket = Ticket.findById(ticketId);
        return ticket.companyEntitlement.id;
    }

    @Transactional
    Long ticketVersionId(Long ticketId) {
        Ticket ticket = Ticket.findById(ticketId);
        Version version = Version
                .find("entitlement = ?1 order by date asc, id asc", ticket.companyEntitlement.entitlement)
                .firstResult();
        return version == null ? null : version.id;
    }

    private record Fixture(Long companyId, Long entitlementId, Long versionId) {
    }

    private record RoleFixture(Fixture fixture, Long ticketId, Long fieldId, String fieldName) {
    }
}
