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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Company isolation for the cached directory atoms.
 * <p>
 * All assertions go through HTTP (fresh persistence context per request), so any staleness observed can only come from
 * the directory caches — never from Hibernate L1 session reuse in the test JVM. Each method uses its own companies and
 * users: bypass seeding writes no events and therefore cannot invalidate, so sharing cache keys across methods would
 * poison entries.
 */
@QuarkusTest
class DirectoryCacheTest extends AccessTestSupport {

    @Test
    @TestSecurity(user = "dir-cache-super@mnemosyne-systems.ai", roles = "superuser")
    @JwtSecurity(claims = { @Claim(key = "email", value = "dir-cache-super@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "dir-cache-super") })
    void companySnapshotsAreIsolatedAndInvalidated() {
        String companyAName = "Directory Cache SU Company A";
        String companyBName = "Directory Cache SU Company B";
        String companyCName = "Directory Cache SU Company C";
        ensureUser("dir-cache-super", "dir-cache-super@mnemosyne-systems.ai", User.TYPE_SUPERUSER);
        ensureUser("dir-cache-user-a", "dir-cache-user-a@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("dir-cache-user-b", "dir-cache-user-b@mnemosyne-systems.ai", User.TYPE_USER);
        Long companyA = ensureCompany(companyAName);
        Long companyB = ensureCompany(companyBName);
        ensureCompany(companyCName);
        ensureCompanyUsers(companyA, "dir-cache-super@mnemosyne-systems.ai", "dir-cache-user-a@mnemosyne-systems.ai");
        ensureCompanyUsers(companyB, "dir-cache-super@mnemosyne-systems.ai", "dir-cache-user-b@mnemosyne-systems.ai");

        // Populate both atoms.
        assertDirectoryHas(companyA, "dir-cache-user-a@mnemosyne-systems.ai");
        assertDirectoryHas(companyB, "dir-cache-user-b@mnemosyne-systems.ai");
        assertDirectoryLacks(companyB, "dir-cache-user-a@mnemosyne-systems.ai");

        // Bypass membership write (no events): the cached user list for A must
        // stay unchanged (cache-hit proof).
        ensureUser("dir-cache-new-a", "dir-cache-new-a@mnemosyne-systems.ai", User.TYPE_USER);
        ensureCompanyUsers(companyA, "dir-cache-new-a@mnemosyne-systems.ai");
        assertDirectoryLacks(companyA, "dir-cache-new-a@mnemosyne-systems.ai");
        assertDirectoryHas(companyB, "dir-cache-user-b@mnemosyne-systems.ai");
        assertDirectoryLacks(companyB, "dir-cache-new-a@mnemosyne-systems.ai");

        // Bypass write to the actor's company set: selecting C must still
        // fall back to A (stale-set proof for the companies atom).
        ensureCompanyUsers(ensureCompany(companyCName), "dir-cache-super@mnemosyne-systems.ai");
        RestAssured.given().queryParam("companyId", ensureCompany(companyCName)).get("/api/superuser/users").then()
                .statusCode(200).body("selectedCompanyId", Matchers.equalTo(companyA.intValue()));

        // Hooked write (setActive records USER_DEACTIVATED): A must refresh.
        User userA = User.find("email", "dir-cache-user-a@mnemosyne-systems.ai").firstResult();
        Assertions.assertNotNull(userA);
        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC).formParam("active", false)
                .post("/api/superuser/users/" + userA.id + "/active").then().statusCode(303);
        RestAssured.given().queryParam("companyId", companyA).get("/api/superuser/users").then().statusCode(200).body(
                "items.find { it.email == 'dir-cache-user-a@mnemosyne-systems.ai' }.active", Matchers.equalTo(false));
        // B recomputes after the evict but still sees only its own data.
        assertDirectoryHas(companyB, "dir-cache-user-b@mnemosyne-systems.ai");
        assertDirectoryLacks(companyB, "dir-cache-user-a@mnemosyne-systems.ai");
    }

    @Test
    @TestSecurity(user = "dir-cache-admin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "dir-cache-admin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "dir-cache-admin") })
    void membershipMoveRefreshesBothCompanies() {
        String companyAName = "Directory Cache Admin Company A";
        String companyBName = "Directory Cache Admin Company B";
        ensureUser("dir-cache-admin", "dir-cache-admin@mnemosyne-systems.ai", User.TYPE_ADMIN);
        ensureUser("dir-cache-mover", "dir-cache-mover@mnemosyne-systems.ai", User.TYPE_USER);
        Long companyA = ensureCompany(companyAName);
        Long companyB = ensureCompany(companyBName);
        ensureCompanyUsers(companyA, "dir-cache-mover@mnemosyne-systems.ai");

        // Populate the shared users atom through the admin directory.
        assertAdminDirectoryHas(companyA, "dir-cache-mover@mnemosyne-systems.ai");

        // Move the user A -> B via the admin edit (records no event, so this
        // exercises the direct invalidation hook).
        User mover = User.find("email", "dir-cache-mover@mnemosyne-systems.ai").firstResult();
        Assertions.assertNotNull(mover);
        RestAssured.given().header("X-Billetsys-Client", "react").contentType(ContentType.URLENC)
                .formParam("name", mover.name).formParam("email", "dir-cache-mover@mnemosyne-systems.ai")
                .formParam("type", User.TYPE_USER).formParam("companyId", companyB).post("/user/" + mover.id).then()
                .statusCode(200);

        assertAdminDirectoryLacks(companyA, "dir-cache-mover@mnemosyne-systems.ai");
        assertAdminDirectoryHas(companyB, "dir-cache-mover@mnemosyne-systems.ai");
    }

    void assertDirectoryHas(Long companyId, String email) {
        RestAssured.given().queryParam("companyId", companyId).get("/api/superuser/users").then().statusCode(200)
                .body("items.email", Matchers.hasItem(email));
    }

    void assertDirectoryLacks(Long companyId, String email) {
        RestAssured.given().queryParam("companyId", companyId).get("/api/superuser/users").then().statusCode(200)
                .body("items.email", Matchers.not(Matchers.hasItem(email)));
    }

    void assertAdminDirectoryHas(Long companyId, String email) {
        RestAssured.given().queryParam("companyId", companyId).get("/api/admin/users").then().statusCode(200)
                .body("items.email", Matchers.hasItem(email));
    }

    void assertAdminDirectoryLacks(Long companyId, String email) {
        RestAssured.given().queryParam("companyId", companyId).get("/api/admin/users").then().statusCode(200)
                .body("items.email", Matchers.not(Matchers.hasItem(email)));
    }
}
