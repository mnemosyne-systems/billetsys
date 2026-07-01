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
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;

@QuarkusTest
class TicketRatingAccessTest extends AccessTestSupport {
    @Test
    @TestSecurity(user = "user", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = "user@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "user") })
    void userCanRateOwnResolvedTicket() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating User Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);

        RestAssured.given().header("X-Billetsys-Client", "react").contentType(ContentType.URLENC).formParam("rating", 8)
                .formParam("ratingComment", "Great support!").post("/tickets/" + ticket.id + "/rating").then()
                .statusCode(200).body("redirectTo", Matchers.equalTo(""));

        Ticket rated = refreshedTicket(ticket.id);
        Assertions.assertEquals(8, rated.rating);
        Assertions.assertEquals("Great support!", rated.ratingComment);
    }

    @Test
    @TestSecurity(user = "superuser1", roles = "superuser")
    @JwtSecurity(claims = { @Claim(key = "email", value = "superuser1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "superuser") })
    void superuserCanRateCompanyTicket() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("superuser1", "superuser1@mnemosyne-systems.ai", User.TYPE_SUPERUSER);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating Superuser Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "superuser1@mnemosyne-systems.ai",
                "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);

        RestAssured.given().header("X-Billetsys-Client", "react").contentType(ContentType.URLENC)
                .formParam("rating", 10).formParam("ratingComment", "Excellent!")
                .post("/tickets/" + ticket.id + "/rating").then().statusCode(200)
                .body("redirectTo", Matchers.equalTo(""));

        Ticket rated = refreshedTicket(ticket.id);
        Assertions.assertEquals(10, rated.rating);
        Assertions.assertEquals("Excellent!", rated.ratingComment);
    }

    @Test
    @TestSecurity(user = "support1", roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = "support1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "support") })
    void supportCannotSubmitRating() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating Support Deny Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC).formParam("rating", 7)
                .post("/tickets/" + ticket.id + "/rating").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "tam1", roles = "tam")
    @JwtSecurity(claims = { @Claim(key = "email", value = "tam1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "tam") })
    void tamCannotSubmitRating() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating TAM Deny Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC).formParam("rating", 7)
                .post("/tickets/" + ticket.id + "/rating").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "other-su", roles = "superuser")
    @JwtSecurity(claims = { @Claim(key = "email", value = "other-su@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "superuser") })
    void superuserCannotRateOtherCompanyTicket() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureUser("other-su", "other-su@mnemosyne-systems.ai", User.TYPE_SUPERUSER);
        ensureDefaultCategories();
        Long companyA = ensureCompany("Rating Company A");
        ensureCompanyUsers(companyA, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Long companyB = ensureCompany("Rating Company B");
        ensureCompanyUsers(companyB, "other-su@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyA);

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC).formParam("rating", 5)
                .post("/tickets/" + ticket.id + "/rating").then().statusCode(303)
                .header("Location", Matchers.endsWith("/"));
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = "user@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "user") })
    void ratingBelowOneIsRejected() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating Range Low Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);

        RestAssured.given().contentType(ContentType.URLENC).formParam("rating", 0)
                .post("/tickets/" + ticket.id + "/rating").then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = "user@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "user") })
    void ratingAboveTenIsRejected() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating Range High Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);

        RestAssured.given().contentType(ContentType.URLENC).formParam("rating", 11)
                .post("/tickets/" + ticket.id + "/rating").then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = "user@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "user") })
    void cannotRateNonResolvedTicket() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating Not Resolved Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyId); // status is "Assigned", not "Resolved"

        RestAssured.given().contentType(ContentType.URLENC).formParam("rating", 5)
                .post("/tickets/" + ticket.id + "/rating").then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = "user@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "user") })
    void cannotRateAlreadyRatedTicket() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating Already Rated Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);
        setTicketRating(ticket.id, 7, "Already rated");

        RestAssured.given().contentType(ContentType.URLENC).formParam("rating", 9)
                .formParam("ratingComment", "Try again").post("/tickets/" + ticket.id + "/rating").then()
                .statusCode(400);

        // Verify original rating is unchanged
        Ticket unchanged = refreshedTicket(ticket.id);
        Assertions.assertEquals(7, unchanged.rating);
        Assertions.assertEquals("Already rated", unchanged.ratingComment);
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = "user@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "user") })
    void ratingWithoutCommentSetsCommentToNull() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating No Comment Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);

        RestAssured.given().header("X-Billetsys-Client", "react").contentType(ContentType.URLENC).formParam("rating", 5)
                .formParam("ratingComment", "").post("/tickets/" + ticket.id + "/rating").then().statusCode(200);

        Ticket rated = refreshedTicket(ticket.id);
        Assertions.assertEquals(5, rated.rating);
        Assertions.assertNull(rated.ratingComment);
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = "user@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "user") })
    void ratingIsVisibleInUserTicketDetailResponse() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating Visible User Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);
        setTicketRating(ticket.id, 9, "Wonderful service");

        RestAssured.given().get("/api/user/tickets/" + ticket.id).then().statusCode(200)
                .body("rating", Matchers.equalTo(9)).body("ratingComment", Matchers.equalTo("Wonderful service"));
    }

    @Test
    @TestSecurity(user = "support1", roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = "support1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "support1") })
    void ratingIsVisibleInSupportTicketDetailResponse() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating Visible Support Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);
        setTicketRating(ticket.id, 4, "Could be better");

        RestAssured.given().get("/api/support/tickets/" + ticket.id).then().statusCode(200)
                .body("rating", Matchers.equalTo(4)).body("ratingComment", Matchers.equalTo("Could be better"));
    }

    @Test
    void unauthenticatedUserCannotRate() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating Unauth Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC).formParam("rating", 5)
                .post("/tickets/" + ticket.id + "/rating").then().statusCode(401);
    }
}
