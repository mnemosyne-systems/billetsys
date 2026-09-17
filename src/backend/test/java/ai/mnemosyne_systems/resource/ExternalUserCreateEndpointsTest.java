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
class ExternalUserCreateEndpointsTest extends AccessTestSupport {

    private Long seedExternalCreateCompany() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureUser("super1", "super1@mnemosyne-systems.ai", User.TYPE_SUPERUSER);
        ensureUser("admin1", "admin1@mnemosyne-systems.ai", User.TYPE_ADMIN);
        Long companyId = ensureCompany("External Create Endpoints Co");
        ensureCompanyUsers(companyId, "support1@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai",
                "super1@mnemosyne-systems.ai", "admin1@mnemosyne-systems.ai");
        return companyId;
    }

    @Test
    @TestSecurity(user = "support1@mnemosyne-systems.ai", roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = "support1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "support1") })
    void supportUsersEndpointCreatesExternalWithoutPassword() {
        Long companyId = seedExternalCreateCompany();
        checkCreate("/support/users", companyId);
    }

    @Test
    @TestSecurity(user = "tam1@mnemosyne-systems.ai", roles = "tam")
    @JwtSecurity(claims = { @Claim(key = "email", value = "tam1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "tam1") })
    void tamUsersEndpointCreatesExternalWithoutPassword() {
        Long companyId = seedExternalCreateCompany();
        checkCreate("/tam/users", companyId);
    }

    @Test
    @TestSecurity(user = "super1@mnemosyne-systems.ai", roles = "superuser")
    @JwtSecurity(claims = { @Claim(key = "email", value = "super1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "super1") })
    void superuserUsersEndpointCreatesExternalWithoutPassword() {
        Long companyId = seedExternalCreateCompany();
        checkCreate("/superuser/users", companyId);
    }

    @Test
    @TestSecurity(user = "admin1@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "admin1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "admin1") })
    void adminUsersEndpointCreatesExternalWithoutPassword() {
        Long companyId = seedExternalCreateCompany();
        checkCreate("/users", companyId);
    }

    void checkCreate(String path, Long companyId) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "users-endpoint-" + suffix + "@mnemosyne-systems.ai";

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("name", "extprobe" + suffix).formParam("fullName", "Endpoint External")
                .formParam("email", email).formParam("type", "external").formParam("companyId", companyId).post(path)
                .then().statusCode(303);

        User created = User.find("email", email).firstResult();
        Assertions.assertNotNull(created);
        Assertions.assertEquals(User.TYPE_EXTERNAL, created.type);
        Assertions.assertEquals(User.DISABLED_PASSWORD_HASH, created.passwordHash);
    }
}
