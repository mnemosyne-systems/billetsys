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
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;

@QuarkusTest
class TAMAccessTest extends AccessTestSupport {

    @Test
    @TestSecurity(user = "tam1", roles = "tam")
    @JwtSecurity(claims = { @Claim(key = "email", value = "tam1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "tam1") })
    void tamCanAccessUserTicketsMenu() {
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        Entitlement createPageEntitlement = ensureEntitlement("TAM Create Versions", "TAM create version list");
        ensureVersion(createPageEntitlement, "8.8.8", java.time.LocalDate.of(2024, 6, 1));
        Long companyId = ensureCompany("TAM Co");
        ensureCompanyUsers(companyId, "tam1@mnemosyne-systems.ai");
        Ticket tamTicket = ensureTicket(companyId);
        String tamTicketName = tamTicket == null ? null : tamTicket.name;

        RestAssured.given().redirects().follow(false).get("/user/tickets").then().statusCode(303).header("Location",
                Matchers.endsWith("/user/tickets"));
        RestAssured.given().get("/api/user/tickets").then().statusCode(200).body("items.supportUser.username",
                Matchers.hasItem("support1"));
        RestAssured.given().get("/api/user/tickets/bootstrap").then().statusCode(200)
                .body("submitPath", Matchers.equalTo("/user/tickets"))
                .body("defaultAffectsVersion.name", Matchers.equalTo("1.0.0"));

        RestAssured.given().get("/rss/tam").then().statusCode(200)
                .contentType(Matchers.containsString("application/rss+xml")).body(Matchers.containsString("<rss"))
                .body(Matchers.containsString("TAM tickets feed"));

        Long ticketId = tamTicket == null ? null : tamTicket.id;
        User supportUser = User.find("email", "support1@mnemosyne-systems.ai").firstResult();
        RestAssured.given().get("/api/user/tickets/" + ticketId).then().statusCode(200)
                .body("name", Matchers.equalTo(tamTicketName)).body("title", Matchers.equalTo(tamTicket.displayTitle()))
                .body("levelName", Matchers.equalTo("Critical")).body("secondaryUsersLabel", Matchers.equalTo("TAM"))
                .body("supportUsers.username", Matchers.hasItem("support1"))
                .body("editableResolvedVersion", Matchers.equalTo(true));

        RestAssured.given().redirects().follow(false).get("/tickets/" + ticketId).then().statusCode(303)
                .header("Location", Matchers.endsWith("/user/tickets/" + ticketId));
        RestAssured.given().redirects().follow(false).get("/user/support-users/" + supportUser.id).then()
                .statusCode(303).header("Location", Matchers.endsWith("/user/support-users/" + supportUser.id));
        RestAssured.given().get("/api/user/support-users/" + supportUser.id).then().statusCode(200)
                .body("typeLabel", Matchers.equalTo("Support")).body("username", Matchers.equalTo("support1"))
                .body("backPath", Matchers.equalTo("/user/tickets"));
    }

    @Test
    @TestSecurity(user = "profile", roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = "profile@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "profile") })
    void profileUpdatesName() {
        ensureUser("profile", "profile@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC).post("/profile").then()
                .statusCode(303).header("Location", Matchers.endsWith("/profile?error=Username+is+required"));

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("name", "Profile Updated").post("/profile").then().statusCode(303);
        User updatedProfile = User.find("email", "profile@mnemosyne-systems.ai").firstResult();
        Assertions.assertEquals("profile", updatedProfile.name);

    }

    @Test
    @TestSecurity(user = "tam2", roles = "tam")
    @JwtSecurity(claims = { @Claim(key = "email", value = "tam2@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "tam2") })
    void tamCreatesUsers() {
        ensureUser("tam2", "tam2@mnemosyne-systems.ai", User.TYPE_TAM);
        Long tamCompanyId = ensureCompany("TAM Create Co");
        ensureCompanyUsers(tamCompanyId, "tam2@mnemosyne-systems.ai");

        RestAssured.given().redirects().follow(false).get("/tam/users/" + tamCompanyId).then().statusCode(303)
                .header("Location", Matchers.endsWith("/tam/users?companyId=" + tamCompanyId));
        RestAssured.given().queryParam("companyId", tamCompanyId).get("/api/tam/users").then().statusCode(200)
                .body("title", Matchers.equalTo("Users"))
                .body("selectedCompanyId", Matchers.equalTo(tamCompanyId.intValue()))
                .body("items.username", Matchers.hasItem("tam2"));

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC).formParam("name", "TAM User")
                .formParam("email", "tam-user@mnemosyne-systems.ai").formParam("password", "tam-user")
                .formParam("type", "user").formParam("companyId", tamCompanyId).post("/tam/users").then()
                .statusCode(303);
        User tamCreated = User.find("email", "tam-user@mnemosyne-systems.ai").firstResult();
        Assertions.assertNotNull(tamCreated);
        Assertions.assertEquals(User.TYPE_USER, tamCreated.type);
        Company tamCompany = Company.findById(tamCompanyId);
        tamCompany.users.size();
        Assertions.assertTrue(tamCompany.users.stream().anyMatch(entry -> entry.id.equals(tamCreated.id)));

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC).formParam("name", "TAM User")
                .formParam("email", "tam-user-duplicate@mnemosyne-systems.ai").formParam("password", "tam-user")
                .formParam("type", "user").formParam("companyId", tamCompanyId).post("/tam/users").then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "admin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "admin") })
    void adminCanAccessReports() {
        // [CHANGE] ensureUser — removed password argument
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        Long companyId = ensureCompany("Report Co");
        ensureCompanyUsers(companyId, "tam1@mnemosyne-systems.ai");
        ensureTicket(companyId);

        RestAssured.given().redirects().follow(false).get("/reports").then().statusCode(303).header("Location",
                Matchers.endsWith("/reports"));

        RestAssured.given().redirects().follow(false).queryParam("companyId", companyId).get("/reports").then()
                .statusCode(303).header("Location", Matchers.endsWith("/reports?companyId=" + companyId));
    }

    @Test
    @TestSecurity(user = "tam1", roles = "tam")
    @JwtSecurity(claims = { @Claim(key = "email", value = "tam1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "tam") })
    void tamCanAccessReports() {
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        Long companyId = ensureCompany("Report Co TAM");
        ensureCompanyUsers(companyId, "tam1@mnemosyne-systems.ai");
        ensureTicket(companyId);

        RestAssured.given().redirects().follow(false).get("/reports/tam").then().statusCode(303).header("Location",
                Matchers.endsWith("/reports"));
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = "user@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "user") })
    void userCannotAccessReports() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        Long companyId = ensureCompany("Report Co User");
        ensureTicket(companyId);

        RestAssured.given().redirects().follow(false).get("/reports").then().statusCode(403);

        RestAssured.given().redirects().follow(false).get("/reports/tam").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "support1", roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = "support1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "support") })
    void supportCannotAccessReports() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        Long companyId = ensureCompany("Report Co Support");
        ensureTicket(companyId);

        RestAssured.given().redirects().follow(false).get("/reports").then().statusCode(403);
    }

}
