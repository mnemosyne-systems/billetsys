/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.CustomField;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.service.TicketCustomFieldService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CustomFieldCacheTest extends AccessTestSupport {

    private static final String ADMIN = "cf-cache-admin@mnemosyne-systems.ai";

    @Inject
    TicketCustomFieldService ticketCustomFieldService;

    @Test
    @TestSecurity(user = ADMIN, roles = { "admin", "support" })
    @JwtSecurity(claims = { @Claim(key = "email", value = ADMIN), @Claim(key = "sub", value = "cf-cache-admin") })
    void hookedDefinitionWritesEvictTheCachedDefinitions() {
        ensureUser("cf-cache-admin", ADMIN, User.TYPE_ADMIN);
        bootstrapNames();

        createDefinition("CF Cache Hooked", 0);
        RestAssured.given().get("/api/support/tickets/bootstrap").then().statusCode(200).body("customFields.name",
                Matchers.hasItem("CF Cache Hooked"));

        updateDefinition("CF Cache Hooked", "CF Cache Hooked Renamed", 1);
        RestAssured.given().get("/api/support/tickets/bootstrap").then().statusCode(200)
                .body("customFields.name", Matchers.hasItem("CF Cache Hooked Renamed"))
                .body("customFields.name", Matchers.not(Matchers.hasItem("CF Cache Hooked")));

        RestAssured.given().redirects().follow(false)
                .post("/custom-fields/" + definitionId("CF Cache Hooked Renamed") + "/deactivate").then()
                .statusCode(303);
        RestAssured.given().get("/api/support/tickets/bootstrap").then().statusCode(200).body("customFields.name",
                Matchers.not(Matchers.hasItem("CF Cache Hooked Renamed")));
    }

    @Test
    @TestSecurity(user = ADMIN, roles = { "admin", "support" })
    @JwtSecurity(claims = { @Claim(key = "email", value = ADMIN), @Claim(key = "sub", value = "cf-cache-admin") })
    void directDefinitionWritesStayCachedUntilInvalidated() {
        ensureUser("cf-cache-admin", ADMIN, User.TYPE_ADMIN);
        bootstrapNames();

        Long fieldId = seedDefinitionDirect("CF Cache Bypass");
        RestAssured.given().get("/api/support/tickets/bootstrap").then().statusCode(200).body("customFields.name",
                Matchers.not(Matchers.hasItem("CF Cache Bypass")));

        ticketCustomFieldService.invalidateDefinitions();
        RestAssured.given().get("/api/support/tickets/bootstrap").then().statusCode(200).body("customFields.name",
                Matchers.hasItem("CF Cache Bypass"));

        deactivateDefinitionDirect(fieldId);
        ticketCustomFieldService.invalidateDefinitions();
        RestAssured.given().get("/api/support/tickets/bootstrap").then().statusCode(200).body("customFields.name",
                Matchers.not(Matchers.hasItem("CF Cache Bypass")));
    }

    private List<String> bootstrapNames() {
        return RestAssured.given().get("/api/support/tickets/bootstrap").then().statusCode(200).extract().jsonPath()
                .getList("customFields.name");
    }

    private void createDefinition(String name, int sortOrder) {
        RestAssured.given().redirects().follow(false).multiPart("name", name).multiPart("fieldType", "TEXT")
                .multiPart("required", "false").multiPart("description", "")
                .multiPart("sortOrder", Integer.toString(sortOrder)).multiPart("active", "true").post("/custom-fields")
                .then().statusCode(303);
    }

    private void updateDefinition(String name, String newName, int sortOrder) {
        RestAssured.given().redirects().follow(false).multiPart("name", newName).multiPart("fieldType", "TEXT")
                .multiPart("required", "false").multiPart("description", "")
                .multiPart("sortOrder", Integer.toString(sortOrder)).multiPart("active", "true")
                .post("/custom-fields/" + definitionId(name)).then().statusCode(303);
    }

    private Long definitionId(String name) {
        return RestAssured.given().get("/api/custom-fields").then().statusCode(200).extract().jsonPath()
                .getLong("items.find { it.name == '" + name + "' }.id");
    }

    @Transactional
    Long seedDefinitionDirect(String name) {
        CustomField field = new CustomField();
        field.name = name;
        field.nameKey = TicketCustomFieldService.normalizeNameKey(name);
        field.fieldType = CustomField.TYPE_TEXT;
        field.sortOrder = 0;
        field.persist();
        return field.id;
    }

    @Transactional
    void deactivateDefinitionDirect(Long fieldId) {
        CustomField field = CustomField.findById(fieldId);
        if (field != null) {
            field.active = false;
        }
    }
}
