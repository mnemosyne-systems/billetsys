/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.User;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SupportExternalUserRoutingTest extends AccessTestSupport {

    @Test
    @TestSecurity(user = "support1@mnemosyne-systems.ai", roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = "support1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "support1") })
    void supportExternalUserPostEndpointsDispatch() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        Long companyId = ensureCompany("Support Externals Routing Co");
        ensureCompanyUsers(companyId, "support1@mnemosyne-systems.ai");
        String email = "routing-" + UUID.randomUUID() + "@mnemosyne-systems.ai";

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("fullName", "Routing Target").formParam("email", email).formParam("companyId", companyId)
                .post("/support/externals").then().statusCode(303);
        User created = User.find("email", email).firstResult();
        Assertions.assertNotNull(created);

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("fullName", "Routing Target").formParam("email", email)
                .post("/support/externals/" + created.id).then().statusCode(303);

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .post("/support/externals/" + created.id + "/delete").then().statusCode(303);
        Assertions.assertNull(refreshedUser(created.id));
    }

    @Test
    @TestSecurity(user = "support1@mnemosyne-systems.ai", roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = "support1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "support1") })
    void supportExternalUserCreateRequiresCompany() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureCompany("Support Externals Routing Co");

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("fullName", "Routing Target")
                .formParam("email", "routing-" + UUID.randomUUID() + "@mnemosyne-systems.ai").post("/support/externals")
                .then().statusCode(400);
    }

    private Long seedRoleRoutingCompany() {
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureUser("super1", "super1@mnemosyne-systems.ai", User.TYPE_SUPERUSER);
        ensureUser("plainuser1", "plainuser1@mnemosyne-systems.ai", User.TYPE_USER);
        Long companyId = ensureCompany("Routing Externals Co");
        ensureCompanyUsers(companyId, "tam1@mnemosyne-systems.ai", "super1@mnemosyne-systems.ai",
                "plainuser1@mnemosyne-systems.ai");
        return companyId;
    }

    @Test
    @TestSecurity(user = "tam1@mnemosyne-systems.ai", roles = "tam")
    @JwtSecurity(claims = { @Claim(key = "email", value = "tam1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "tam1") })
    void tamExternalUserPostEndpointsDispatch() {
        Long companyId = seedRoleRoutingCompany();
        checkRoleCrud("tam", companyId);
    }

    @Test
    @TestSecurity(user = "super1@mnemosyne-systems.ai", roles = "superuser")
    @JwtSecurity(claims = { @Claim(key = "email", value = "super1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "super1") })
    void superuserExternalUserPostEndpointsDispatch() {
        Long companyId = seedRoleRoutingCompany();
        checkRoleCrud("superuser", companyId);
    }

    @Test
    @TestSecurity(user = "plainuser1@mnemosyne-systems.ai", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = "plainuser1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "plainuser1") })
    void userExternalUserPostEndpointsDispatch() {
        Long companyId = seedRoleRoutingCompany();
        checkRoleCrud("user", companyId);
    }

    void checkRoleCrud(String role, Long companyId) {
        String email = "routing-" + role + "-" + UUID.randomUUID() + "@mnemosyne-systems.ai";

        RestAssured.given().get("/api/" + role + "/externals").then().statusCode(200);

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("fullName", "Routing Target").formParam("email", email).formParam("companyId", companyId)
                .post("/" + role + "/externals").then().statusCode(303);
        User created = User.find("email", email).firstResult();
        Assertions.assertNotNull(created);

        RestAssured.given().get("/api/" + role + "/externals/" + created.id).then().statusCode(200);

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("fullName", "Routing Target").formParam("email", email)
                .post("/" + role + "/externals/" + created.id).then().statusCode(303);

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .post("/" + role + "/externals/" + created.id + "/delete").then().statusCode(303);
        Assertions.assertNull(refreshedUser(created.id));
    }
}
