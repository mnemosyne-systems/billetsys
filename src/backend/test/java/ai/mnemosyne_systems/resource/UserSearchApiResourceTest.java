/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UserSearchApiResourceTest extends AccessTestSupport {

    @Test
    void userSearchIsScopedByRole() {
        ensureUser("search-user", "search-user@mnemosyne-systems.ai", User.TYPE_USER, "pass");
        ensureUser("same-company-user", "same-company@mnemosyne-systems.ai", User.TYPE_USER, "pass");
        ensureUser("other-company-user", "other-company@mnemosyne-systems.ai", User.TYPE_USER, "pass");
        ensureUser("search-superuser", "search-superuser@mnemosyne-systems.ai", User.TYPE_SUPERUSER, "pass");
        ensureUser("search-support", "search-support@mnemosyne-systems.ai", User.TYPE_SUPPORT, "pass");
        ensureUser("search-tam", "search-tam@mnemosyne-systems.ai", User.TYPE_TAM, "pass");
        ensureUser("search-admin", "search-admin@mnemosyne-systems.ai", User.TYPE_ADMIN, "pass");

        Long companyId = ensureCompany("User Search Company");
        Long otherCompanyId = ensureCompany("User Search Other Company");

        ensureCompanyUsers(companyId, "search-user@mnemosyne-systems.ai", "same-company@mnemosyne-systems.ai",
                "search-superuser@mnemosyne-systems.ai");

        ensureCompanyUsers(otherCompanyId, "other-company@mnemosyne-systems.ai");

        String userCookie = login("search-user", "pass");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, userCookie).queryParam("q", "same-company")
                .get("/api/users/suggest").then().statusCode(200)
                .body("items.title", Matchers.hasItem("same-company@mnemosyne-systems.ai"));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, userCookie).queryParam("q", "other-company")
                .get("/api/users/suggest").then().statusCode(200)
                .body("items.title", Matchers.not(Matchers.hasItem("other-company@mnemosyne-systems.ai")));

        String superuserCookie = login("search-superuser", "pass");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, superuserCookie).queryParam("q", "other-company")
                .get("/api/users/suggest").then().statusCode(200)
                .body("items.title", Matchers.not(Matchers.hasItem("other-company@mnemosyne-systems.ai")));

        String supportCookie = login("search-support", "pass");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, supportCookie).queryParam("q", "other-company")
                .get("/api/users/suggest").then().statusCode(200)
                .body("items.title", Matchers.hasItem("other-company@mnemosyne-systems.ai"));

        String tamCookie = login("search-tam", "pass");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, tamCookie).queryParam("q", "other-company")
                .get("/api/users/suggest").then().statusCode(200)
                .body("items.title", Matchers.hasItem("other-company@mnemosyne-systems.ai"));

        String adminCookie = login("search-admin", "pass");
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, adminCookie).queryParam("q", "other-company")
                .get("/api/users/suggest").then().statusCode(200)
                .body("items.title", Matchers.hasItem("other-company@mnemosyne-systems.ai"));
    }

    @Test
    void userSearchMatchesFullName() {
        ensureUser("fullname-user", "fullname-user@mnemosyne-systems.ai", User.TYPE_USER, "pass");
        ensureUser("search-user-fullname", "search-user-fullname@mnemosyne-systems.ai", User.TYPE_USER, "pass");

        Long companyId = ensureCompany("User Search Full Name Company");

        ensureCompanyUsers(companyId, "fullname-user@mnemosyne-systems.ai",
                "search-user-fullname@mnemosyne-systems.ai");

        setUserFullName("fullname-user@mnemosyne-systems.ai", "Technical Account Manager Full Name");

        String cookie = login("search-user-fullname", "pass");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).queryParam("q", "Technical Account Manager")
                .get("/api/users/suggest").then().statusCode(200)
                .body("items.title", Matchers.hasItem("fullname-user@mnemosyne-systems.ai"));
    }
}
