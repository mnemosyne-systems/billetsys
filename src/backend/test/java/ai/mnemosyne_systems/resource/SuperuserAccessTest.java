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
class SuperuserAccessTest extends AccessTestSupport {

    @Test
    void superuserCanAccessCompanyScopedPages() {
        ensureUser("superuser1", "superuser1@mnemosyne-systems.ai", User.TYPE_SUPERUSER, "superuser1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        Entitlement createPageEntitlement = ensureEntitlement("Superuser Create Versions",
                "Superuser create version list");
        ensureVersion(createPageEntitlement, "7.7.7", java.time.LocalDate.of(2024, 7, 1));
        Long companyId = ensureCompany("Superuser Co");
        Long otherCompanyId = ensureCompany("Other Superuser Co");
        ensureCompanyUsers(companyId, "superuser1@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        ai.mnemosyne_systems.model.Ticket superuserTicket = ensureTicket(companyId);
        ensureMessage(superuserTicket, "Superuser ticket message");
        ai.mnemosyne_systems.model.Ticket otherCompanyTicket = ensureTicket(otherCompanyId);
        String cookie = login("superuser1", "superuser1");

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/").then()
                .statusCode(303).header("Location", "/");

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/superuser").then()
                .statusCode(303).header("Location", Matchers.endsWith("/superuser/tickets"));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/superuser/tickets/create").then().statusCode(303)
                .header("Location", Matchers.endsWith("/superuser/tickets/new"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/superuser/tickets/bootstrap").then()
                .statusCode(200).body("submitPath", Matchers.equalTo("/superuser/tickets"))
                .body("defaultAffectsVersion.name", Matchers.equalTo("1.0.0"));
        User creatingSuperuser = User.find("email", "superuser1@mnemosyne-systems.ai").firstResult();
        CompanyEntitlement superuserCreateEntitlement = CompanyEntitlement
                .find("company.id = ?1 order by id asc", companyId).firstResult();
        Assertions.assertNotNull(superuserCreateEntitlement);
        Version superuserCreateVersion = Version
                .find("entitlement = ?1 order by date asc, id asc", superuserCreateEntitlement.entitlement)
                .firstResult();
        Assertions.assertNotNull(superuserCreateVersion);
        String superuserCreateMessage = "Superuser create redirect coverage";
        String superuserCreateRedirect = RestAssured.given().redirects().follow(false)
                .cookie(AuthHelper.AUTH_COOKIE, cookie).multiPart("status", "Open")
                .multiPart("title", "Superuser create redirect title").multiPart("message", superuserCreateMessage)
                .multiPart("companyId", companyId).multiPart("companyEntitlementId", superuserCreateEntitlement.id)
                .multiPart("categoryId", Category.findDefault().id)
                .multiPart("affectsVersionId", superuserCreateVersion.id).post("/superuser/tickets").then()
                .statusCode(303).extract().header("Location");
        Message createdSuperuserMessage = Message.find("body", superuserCreateMessage).firstResult();
        Assertions.assertNotNull(createdSuperuserMessage);
        Ticket createdSuperuserTicket = createdSuperuserMessage.ticket;
        Assertions.assertNotNull(createdSuperuserTicket);
        Assertions.assertNotNull(createdSuperuserTicket.affectsVersion);
        Assertions.assertEquals(superuserCreateVersion.id, createdSuperuserTicket.affectsVersion.id);
        Assertions.assertTrue(superuserCreateRedirect.endsWith("/superuser/tickets/" + createdSuperuserTicket.id));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/superuser/users/" + companyId).then().statusCode(303)
                .header("Location", Matchers.endsWith("/superuser/users?companyId=" + companyId));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).queryParam("companyId", companyId)
                .get("/api/superuser/users").then().statusCode(200).body("title", Matchers.equalTo("Users"))
                .body("selectedCompanyId", Matchers.equalTo(companyId.intValue()))
                .body("items.username", Matchers.hasItems("superuser1", "tam1"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/reports/superuser")
                .then().statusCode(303).header("Location", Matchers.endsWith("/reports"));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/rss/superuser").then().statusCode(200)
                .contentType(Matchers.containsString("application/rss+xml"))
                .body(Matchers.containsString("Superuser tickets feed"));

        Long ticketId = superuserTicket == null ? null : superuserTicket.id;
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/superuser/tickets/" + ticketId).then().statusCode(303)
                .header("Location", Matchers.endsWith("/superuser/tickets/" + ticketId));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/superuser/tickets/" + ticketId).then()
                .statusCode(200).body("title", Matchers.equalTo(superuserTicket.displayTitle()))
                .body("secondaryUsersLabel", Matchers.equalTo("Superusers"))
                .body("secondaryUsers.username", Matchers.hasItem("superuser1"))
                .body("supportUsers.username", Matchers.hasItem("support1"))
                .body("editableResolvedVersion", Matchers.equalTo(true))
                .body("exportPath", Matchers.equalTo("/tickets/export/" + ticketId));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/tickets/" + ticketId)
                .then().statusCode(303).header("Location", Matchers.endsWith("/superuser/tickets/" + ticketId));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/superuser/tickets").then().statusCode(200)
                .body("items.name", Matchers.hasItem(superuserTicket.name))
                .body("items.name", Matchers.not(Matchers.hasItem(otherCompanyTicket.name)));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/superuser/companies/" + otherCompanyId).then().statusCode(303)
                .header("Location", Matchers.endsWith("/"));
    }

    @Test
    void superuserReplyUsesLatestPendingMessageForTicketSlaColor() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureUser("superuser1", "superuser1@mnemosyne-systems.ai", User.TYPE_SUPERUSER, "superuser1");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Superuser SLA Co");
        ensureCompanyUsers(companyId, "superuser1@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyId);
        Level urgent = ensureLevel("Normal", "Immediate response", 1, "Red");
        setTicketSupportLevel(ticket.id, urgent.id);
        ensureTimedMessage(ticket, "Initial superuser message", "superuser1@mnemosyne-systems.ai",
                java.time.LocalDateTime.now().minusMinutes(10));

        String cookie = login("superuser1", "superuser1");
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/superuser/tickets").then().statusCode(200)
                .body("items.find { it.id == " + ticket.id + " }.slaColor", Matchers.equalTo("Red"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .multiPart("body", "Follow-up from superuser").post("/superuser/tickets/" + ticket.id + "/messages")
                .then().statusCode(303);

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/superuser/tickets").then().statusCode(200)
                .body("items.find { it.id == " + ticket.id + " }.slaColor", Matchers.equalTo("White"));

        ensureTimedMessage(ticket, "Support follow-up", "support1@mnemosyne-systems.ai",
                java.time.LocalDateTime.now().minusMinutes(1));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/superuser/tickets").then().statusCode(200)
                .body("items.find { it.id == " + ticket.id + " }.slaColor", Matchers.equalTo("White"));
    }

    @Test
    void superuserTicketSearchMatchesVisibleMessagesOnly() {
        ensureUser("superuser1", "superuser1@mnemosyne-systems.ai", User.TYPE_SUPERUSER, "superuser1");
        Long companyId = ensureCompany("Superuser Search Co");
        ensureCompanyUsers(companyId, "superuser1@mnemosyne-systems.ai");
        Ticket visibleTicket = ensureTicket(companyId);
        String visibleBody = "superuser-visible-search-" + System.nanoTime();
        ensureMessageWithBody(visibleTicket, visibleBody);
        String cookie = login("superuser1", "superuser1");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).queryParam("q", visibleBody)
                .get("/api/superuser/tickets").then().statusCode(200).body("searchTerm", Matchers.equalTo(visibleBody))
                .body("items.name", Matchers.hasItem(visibleTicket.name));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).queryParam("q", "superuser-search-no-match")
                .get("/api/superuser/tickets").then().statusCode(200).body("items.size()", Matchers.equalTo(0));
    }

    @Test
    void reactAppSessionReturnsSuperuserTicketLink() {
        ensureUser("superuser1", "superuser1@mnemosyne-systems.ai", User.TYPE_SUPERUSER, "superuser1");
        String cookie = login("superuser1", "superuser1");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/app/session").then().statusCode(200)
                .body("role", Matchers.equalTo("superuser"))
                .body("navigation.href", Matchers.hasItem("/superuser/tickets"));
    }

    @Test
    void reactSuperuserMutationsReturnJsonRedirects() {
        ensureUser("superuser-json", "superuser-json@mnemosyne-systems.ai", User.TYPE_SUPERUSER, "superuser-json");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Superuser Json Co");
        ensureCompanyUsers(companyId, "superuser-json@mnemosyne-systems.ai");
        String cookie = login("superuser-json", "superuser-json");
        Entitlement entitlement = ensureEntitlement("Superuser Json Entitlement", "Superuser json detail");
        Version version = ensureVersion(entitlement, "11.0.0", java.time.LocalDate.of(2026, 3, 1));
        CompanyEntitlement entry = ensureCompanyEntitlement(companyId, entitlement);
        Assertions.assertNotNull(entry);
        Assertions.assertNotNull(version);

        String redirectTo = RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie)
                .header("X-Billetsys-Client", "react").multiPart("status", "Open")
                .multiPart("title", "Superuser json redirect title")
                .multiPart("message", "Superuser json redirect create").multiPart("companyId", companyId)
                .multiPart("companyEntitlementId", entry.id).multiPart("categoryId", Category.findDefault().id)
                .multiPart("affectsVersionId", version.id).post("/superuser/tickets").then().statusCode(200)
                .body("redirectTo", Matchers.matchesPattern("/superuser/tickets/\\d+")).extract().path("redirectTo");
        Long createdTicketId = Long.valueOf(redirectTo.substring(redirectTo.lastIndexOf('/') + 1));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).header("X-Billetsys-Client", "react")
                .contentType(ContentType.URLENC).formParam("title", "Superuser json redirect title")
                .formParam("affectsVersionId", version.id).post("/superuser/tickets/" + createdTicketId).then()
                .statusCode(200).body("redirectTo", Matchers.equalTo("/superuser/tickets/" + createdTicketId));
    }

}
