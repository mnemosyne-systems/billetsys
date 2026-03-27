/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.Article;
import ai.mnemosyne_systems.model.Attachment;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Entitlement;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Level;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.Version;
import ai.mnemosyne_systems.service.MailboxPollingService;
import ai.mnemosyne_systems.service.TicketEmailService;
import ai.mnemosyne_systems.util.AuthHelper;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TAMAccessTest extends AccessTestSupport {

    @Test
    void tamCanAccessUserTicketsMenu() {
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        Entitlement createPageEntitlement = ensureEntitlement("TAM Create Versions", "TAM create version list");
        ensureVersion(createPageEntitlement, "8.8.8", java.time.LocalDate.of(2024, 6, 1));
        Long companyId = ensureCompany("TAM Co");
        ensureCompanyUsers(companyId, "tam1@mnemosyne-systems.ai");
        ai.mnemosyne_systems.model.Ticket tamTicket = ensureTicket(companyId);
        String tamTicketName = tamTicket == null ? null : tamTicket.name;
        String cookie = login("tam1", "tam1");

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/user/tickets").then()
                .statusCode(303).header("Location", Matchers.endsWith("/user/tickets"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/user/tickets").then().statusCode(200)
                .body("items.supportUser.username", Matchers.hasItem("support1"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/user/tickets/bootstrap").then()
                .statusCode(200).body("submitPath", Matchers.equalTo("/user/tickets"))
                .body("defaultAffectsVersion.name", Matchers.equalTo("1.0.0"));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/rss/tam").then().statusCode(200)
                .contentType(Matchers.containsString("application/rss+xml")).body(Matchers.containsString("<rss"))
                .body(Matchers.containsString("TAM tickets feed"));

        Long ticketId = tamTicket == null ? null : tamTicket.id;
        User supportUser = User.find("email", "support1@mnemosyne-systems.ai").firstResult();
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/user/tickets/" + ticketId).then()
                .statusCode(200).body("name", Matchers.equalTo(tamTicketName))
                .body("levelName", Matchers.equalTo("Critical")).body("secondaryUsersLabel", Matchers.equalTo("TAM"))
                .body("supportUsers.username", Matchers.hasItem("support1"))
                .body("editableResolvedVersion", Matchers.equalTo(true));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/tickets/" + ticketId)
                .then().statusCode(303).header("Location", Matchers.endsWith("/user/tickets/" + ticketId));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/user/support-users/" + supportUser.id).then().statusCode(303)
                .header("Location", Matchers.endsWith("/user/support-users/" + supportUser.id));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/user/support-users/" + supportUser.id)
                .then().statusCode(200).body("typeLabel", Matchers.equalTo("Support"))
                .body("username", Matchers.equalTo("support1")).body("backPath", Matchers.equalTo("/user/tickets"));
    }

    @Test
    void profileUpdatesAndTamCreatesUsers() {
        ensureUser("profile", "profile@mnemosyne-systems.ai", User.TYPE_SUPPORT, "profile");
        String profileCookie = login("profile", "profile");
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, profileCookie)
                .contentType(ContentType.URLENC).post("/profile").then().statusCode(303)
                .header("Location", Matchers.endsWith("/profile?error=Username+is+required"));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, profileCookie)
                .contentType(ContentType.URLENC).formParam("name", "Profile Updated").post("/profile").then()
                .statusCode(303);
        User updatedProfile = User.find("email", "profile@mnemosyne-systems.ai").firstResult();
        Assertions.assertEquals("Profile Updated", updatedProfile.name);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, profileCookie)
                .contentType(ContentType.URLENC).post("/profile/password").then().statusCode(303)
                .header("Location", Matchers.endsWith("/profile/password?error=Old+password+is+required"));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, profileCookie)
                .contentType(ContentType.URLENC).formParam("oldPassword", "profile")
                .formParam("newPassword", "profile2").formParam("confirmPassword", "profile2").post("/profile/password")
                .then().statusCode(303);
        User refreshedProfile = refreshedUser(updatedProfile.id);
        Assertions.assertTrue(BcryptUtil.matches("profile2", refreshedProfile.passwordHash));

        ensureUser("tam2", "tam2@mnemosyne-systems.ai", User.TYPE_TAM, "tam2");
        Long tamCompanyId = ensureCompany("TAM Create Co");
        ensureCompanyUsers(tamCompanyId, "tam2@mnemosyne-systems.ai");
        String tamCookie = login("tam2", "tam2");

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, tamCookie)
                .get("/tam/users/" + tamCompanyId).then().statusCode(303)
                .header("Location", Matchers.endsWith("/tam/users?companyId=" + tamCompanyId));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, tamCookie).queryParam("companyId", tamCompanyId)
                .get("/api/tam/users").then().statusCode(200).body("title", Matchers.equalTo("Users"))
                .body("selectedCompanyId", Matchers.equalTo(tamCompanyId.intValue()))
                .body("items.username", Matchers.hasItem("tam2"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, tamCookie)
                .contentType(ContentType.URLENC).formParam("name", "TAM User")
                .formParam("email", "tam-user@mnemosyne-systems.ai").formParam("password", "tam-user")
                .formParam("type", "user").formParam("companyId", tamCompanyId).post("/tam/users").then()
                .statusCode(303);
        User tamCreated = User.find("email", "tam-user@mnemosyne-systems.ai").firstResult();
        Assertions.assertNotNull(tamCreated);
        Assertions.assertEquals(User.TYPE_USER, tamCreated.type);
        Company tamCompany = Company.findById(tamCompanyId);
        tamCompany.users.size();
        Assertions.assertTrue(tamCompany.users.stream().anyMatch(entry -> entry.id.equals(tamCreated.id)));
    }

    @Test
    void reportsRespectRolePermissions() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN, "admin");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        Long companyId = ensureCompany("Report Co");
        ensureCompanyUsers(companyId, "tam1@mnemosyne-systems.ai");
        ensureTicket(companyId);
        String adminCookie = login("admin", "admin");

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, adminCookie).get("/reports").then()
                .statusCode(303).header("Location", Matchers.endsWith("/reports"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, adminCookie)
                .queryParam("companyId", companyId).get("/reports").then().statusCode(303)
                .header("Location", Matchers.endsWith("/reports?companyId=" + companyId));

        String tamCookie = login("tam1", "tam1");
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, tamCookie).get("/reports/tam")
                .then().statusCode(303).header("Location", Matchers.endsWith("/reports"));

        String userCookie = login("user", "user");
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, userCookie).get("/reports").then()
                .statusCode(303);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, userCookie).get("/reports/tam")
                .then().statusCode(303);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, tamCookie).get("/reports").then()
                .statusCode(303);

        String supportCookie = login("support1", "support1");
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, supportCookie).get("/reports")
                .then().statusCode(303);
    }

}
