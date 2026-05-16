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

@QuarkusTest
class TicketRatingAccessTest extends AccessTestSupport {

    @Test
    void userCanRateOwnResolvedTicket() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating User Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);

        String cookie = login("user", "user");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).header("X-Billetsys-Client", "react")
                .contentType(ContentType.URLENC).formParam("rating", 8).formParam("ratingComment", "Great support!")
                .post("/tickets/" + ticket.id + "/rating").then().statusCode(200)
                .body("redirectTo", Matchers.equalTo(""));

        Ticket rated = refreshedTicket(ticket.id);
        Assertions.assertEquals(8, rated.rating);
        Assertions.assertEquals("Great support!", rated.ratingComment);
    }

    @Test
    void superuserCanRateCompanyTicket() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("superuser1", "superuser1@mnemosyne-systems.ai", User.TYPE_SUPERUSER, "superuser1");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating Superuser Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "superuser1@mnemosyne-systems.ai",
                "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);

        String cookie = login("superuser1", "superuser1");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).header("X-Billetsys-Client", "react")
                .contentType(ContentType.URLENC).formParam("rating", 10).formParam("ratingComment", "Excellent!")
                .post("/tickets/" + ticket.id + "/rating").then().statusCode(200)
                .body("redirectTo", Matchers.equalTo(""));

        Ticket rated = refreshedTicket(ticket.id);
        Assertions.assertEquals(10, rated.rating);
        Assertions.assertEquals("Excellent!", rated.ratingComment);
    }

    @Test
    void supportCannotSubmitRating() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating Support Deny Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);

        String cookie = login("support1", "support1");

        // Support role should be redirected to login (role not allowed)
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("rating", 7).post("/tickets/" + ticket.id + "/rating").then()
                .statusCode(303).header("Location", Matchers.endsWith("/login"));
    }

    @Test
    void tamCannotSubmitRating() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating TAM Deny Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);

        String cookie = login("tam1", "tam1");

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("rating", 7).post("/tickets/" + ticket.id + "/rating").then()
                .statusCode(303).header("Location", Matchers.endsWith("/login"));
    }

    @Test
    void superuserCannotRateOtherCompanyTicket() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureUser("other-su", "other-su@mnemosyne-systems.ai", User.TYPE_SUPERUSER, "other-su");
        ensureDefaultCategories();
        Long companyA = ensureCompany("Rating Company A");
        ensureCompanyUsers(companyA, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Long companyB = ensureCompany("Rating Company B");
        ensureCompanyUsers(companyB, "other-su@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyA);

        // Superuser from Company B tries to rate Company A's ticket
        String cookie = login("other-su", "other-su");

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("rating", 5).post("/tickets/" + ticket.id + "/rating").then()
                .statusCode(303).header("Location", Matchers.endsWith("/"));
    }

    @Test
    void ratingBelowOneIsRejected() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating Range Low Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);

        String cookie = login("user", "user");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).contentType(ContentType.URLENC)
                .formParam("rating", 0).post("/tickets/" + ticket.id + "/rating").then().statusCode(400);
    }

    @Test
    void ratingAboveTenIsRejected() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating Range High Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);

        String cookie = login("user", "user");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).contentType(ContentType.URLENC)
                .formParam("rating", 11).post("/tickets/" + ticket.id + "/rating").then().statusCode(400);
    }

    @Test
    void cannotRateNonResolvedTicket() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating Not Resolved Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyId); // status is "Assigned", not "Resolved"

        String cookie = login("user", "user");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).contentType(ContentType.URLENC)
                .formParam("rating", 5).post("/tickets/" + ticket.id + "/rating").then().statusCode(400);
    }

    @Test
    void cannotRateAlreadyRatedTicket() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating Already Rated Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);
        setTicketRating(ticket.id, 7, "Already rated");

        String cookie = login("user", "user");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).contentType(ContentType.URLENC)
                .formParam("rating", 9).formParam("ratingComment", "Try again")
                .post("/tickets/" + ticket.id + "/rating").then().statusCode(400);

        // Verify original rating is unchanged
        Ticket unchanged = refreshedTicket(ticket.id);
        Assertions.assertEquals(7, unchanged.rating);
        Assertions.assertEquals("Already rated", unchanged.ratingComment);
    }

    @Test
    void ratingWithoutCommentSetsCommentToNull() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating No Comment Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);

        String cookie = login("user", "user");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).header("X-Billetsys-Client", "react")
                .contentType(ContentType.URLENC).formParam("rating", 5).formParam("ratingComment", "")
                .post("/tickets/" + ticket.id + "/rating").then().statusCode(200);

        Ticket rated = refreshedTicket(ticket.id);
        Assertions.assertEquals(5, rated.rating);
        Assertions.assertNull(rated.ratingComment);
    }

    @Test
    void ratingIsVisibleInUserTicketDetailResponse() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating Visible User Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);
        setTicketRating(ticket.id, 9, "Wonderful service");

        String cookie = login("user", "user");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/user/tickets/" + ticket.id).then()
                .statusCode(200).body("rating", Matchers.equalTo(9))
                .body("ratingComment", Matchers.equalTo("Wonderful service"));
    }

    @Test
    void ratingIsVisibleInSupportTicketDetailResponse() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating Visible Support Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);
        setTicketRating(ticket.id, 4, "Could be better");

        String cookie = login("support1", "support1");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/support/tickets/" + ticket.id).then()
                .statusCode(200).body("rating", Matchers.equalTo(4))
                .body("ratingComment", Matchers.equalTo("Could be better"));
    }

    @Test
    void unauthenticatedUserCannotRate() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Rating Unauth Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureResolvedTicket(companyId);

        // No cookie — should redirect to login
        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC).formParam("rating", 5)
                .post("/tickets/" + ticket.id + "/rating").then().statusCode(303)
                .header("Location", Matchers.endsWith("/login"));
    }
}
