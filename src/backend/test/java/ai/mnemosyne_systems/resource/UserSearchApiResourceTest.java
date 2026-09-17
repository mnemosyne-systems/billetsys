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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UserSearchApiResourceTest extends AccessTestSupport {

    private void seedScopedSearchData() {
        ensureUser("search-user", "search-user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("same-company-user", "same-company@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("other-company-user", "other-company@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("search-superuser", "search-superuser@mnemosyne-systems.ai", User.TYPE_SUPERUSER);
        ensureUser("search-support", "search-support@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("search-tam", "search-tam@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureUser("search-admin", "search-admin@mnemosyne-systems.ai", User.TYPE_ADMIN);

        Long companyId = ensureCompany("User Search Company");
        Long otherCompanyId = ensureCompany("User Search Other Company");

        ensureCompanyUsers(companyId, "search-user@mnemosyne-systems.ai", "same-company@mnemosyne-systems.ai",
                "search-superuser@mnemosyne-systems.ai");

        ensureCompanyUsers(otherCompanyId, "other-company@mnemosyne-systems.ai");
    }

    @Test
    @TestSecurity(user = "search-user@mnemosyne-systems.ai", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = "search-user@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "search-user") })
    void userSearchIsScopedToOwnCompanyForUserRole() {
        seedScopedSearchData();

        RestAssured.given().queryParam("q", "same-company").get("/api/users/suggest").then().statusCode(200)
                .body("items.title", Matchers.hasItem("same-company@mnemosyne-systems.ai"));

        RestAssured.given().queryParam("q", "other-company").get("/api/users/suggest").then().statusCode(200)
                .body("items.title", Matchers.not(Matchers.hasItem("other-company@mnemosyne-systems.ai")));
    }

    @Test
    @TestSecurity(user = "search-superuser@mnemosyne-systems.ai", roles = "superuser")
    @JwtSecurity(claims = { @Claim(key = "email", value = "search-superuser@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "search-superuser") })
    void userSearchIsScopedToOwnCompanyForSuperuserRole() {
        seedScopedSearchData();

        RestAssured.given().queryParam("q", "other-company").get("/api/users/suggest").then().statusCode(200)
                .body("items.title", Matchers.not(Matchers.hasItem("other-company@mnemosyne-systems.ai")));
    }

    @Test
    @TestSecurity(user = "search-support@mnemosyne-systems.ai", roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = "search-support@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "search-support") })
    void userSearchSeesAllCompaniesForSupportRole() {
        seedScopedSearchData();

        RestAssured.given().queryParam("q", "other-company").get("/api/users/suggest").then().statusCode(200)
                .body("items.title", Matchers.hasItem("other-company@mnemosyne-systems.ai"));
    }

    @Test
    @TestSecurity(user = "search-tam@mnemosyne-systems.ai", roles = "tam")
    @JwtSecurity(claims = { @Claim(key = "email", value = "search-tam@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "search-tam") })
    void userSearchSeesAllCompaniesForTamRole() {
        seedScopedSearchData();

        RestAssured.given().queryParam("q", "other-company").get("/api/users/suggest").then().statusCode(200)
                .body("items.title", Matchers.hasItem("other-company@mnemosyne-systems.ai"));
    }

    @Test
    @TestSecurity(user = "search-admin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "search-admin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "search-admin") })
    void userSearchSeesAllCompaniesForAdminRole() {
        seedScopedSearchData();

        RestAssured.given().queryParam("q", "other-company").get("/api/users/suggest").then().statusCode(200)
                .body("items.title", Matchers.hasItem("other-company@mnemosyne-systems.ai"));
    }

    @Test
    @TestSecurity(user = "search-user-fullname@mnemosyne-systems.ai", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = "search-user-fullname@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "search-user-fullname") })
    void userSearchMatchesFullName() {
        ensureUser("fullname-user", "fullname-user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("search-user-fullname", "search-user-fullname@mnemosyne-systems.ai", User.TYPE_USER);

        Long companyId = ensureCompany("User Search Full Name Company");

        ensureCompanyUsers(companyId, "fullname-user@mnemosyne-systems.ai",
                "search-user-fullname@mnemosyne-systems.ai");

        setUserFullName("fullname-user@mnemosyne-systems.ai", "Technical Account Manager Full Name");

        RestAssured.given().queryParam("q", "Technical Account Manager").get("/api/users/suggest").then()
                .statusCode(200).body("items.title", Matchers.hasItem("fullname-user@mnemosyne-systems.ai"));
    }
}
