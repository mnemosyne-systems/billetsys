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
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
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
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.imageio.ImageIO;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UserAccessTest extends AccessTestSupport {

    @Test
    void userCanAccessUserTicketsMenu() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureDefaultCategories();
        Entitlement createPageEntitlement = ensureEntitlement("Create Page Versions", "Create page version list");
        ensureVersion(createPageEntitlement, "0.9.0", java.time.LocalDate.of(2024, 1, 1));
        ensureVersion(createPageEntitlement, "9.9.9", java.time.LocalDate.of(2024, 12, 31));
        Long companyId = ensureCompany("Test Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "superuser1@mnemosyne-systems.ai");
        ai.mnemosyne_systems.model.Ticket userTicket = ensureTicket(companyId);
        String userTicketName = userTicket == null ? "" : userTicket.name;
        String cookie = login("user", "user");

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/user/tickets").then()
                .statusCode(303).header("Location", Matchers.endsWith("/user/tickets"));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/user/tickets/open")
                .then().statusCode(303).header("Location", Matchers.endsWith("/user/tickets/open"));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/user/tickets/closed")
                .then().statusCode(303).header("Location", Matchers.endsWith("/user/tickets/closed"));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/user/tickets/create")
                .then().statusCode(303).header("Location", Matchers.endsWith("/user/tickets/new"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/user/tickets").then().statusCode(200)
                .body("title", Matchers.equalTo("Tickets")).body("createPath", Matchers.equalTo("/user/tickets/new"))
                .body("items.name", Matchers.hasItem(userTicketName));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/user/tickets/bootstrap").then()
                .statusCode(200).body("submitPath", Matchers.equalTo("/user/tickets"))
                .body("defaultAffectsVersion.name", Matchers.equalTo("1.0.0"))
                .body("categories.name", Matchers.hasItem("Question"));
        User creatingUser = User.find("email", "user@mnemosyne-systems.ai").firstResult();
        CompanyEntitlement userCreateEntitlement = CompanyEntitlement.find("company.id = ?1 order by id asc", companyId)
                .firstResult();
        Assertions.assertNotNull(userCreateEntitlement);
        Version userCreateVersion = Version
                .find("entitlement = ?1 order by date asc, id asc", userCreateEntitlement.entitlement).firstResult();
        Assertions.assertNotNull(userCreateVersion);
        String userCreateMessage = "User create redirect coverage";
        String userCreateRedirect = RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .multiPart("status", "Open").multiPart("title", "User create redirect title")
                .multiPart("message", userCreateMessage).multiPart("companyId", companyId)
                .multiPart("companyEntitlementId", userCreateEntitlement.id)
                .multiPart("categoryId", Category.findDefault().id).multiPart("affectsVersionId", userCreateVersion.id)
                .post("/user/tickets").then().statusCode(303).extract().header("Location");
        Message createdUserMessage = Message.find("body", userCreateMessage).firstResult();
        Assertions.assertNotNull(createdUserMessage);
        Ticket createdUserTicket = createdUserMessage.ticket;
        Assertions.assertNotNull(createdUserTicket);
        Assertions.assertNotNull(createdUserTicket.affectsVersion);
        Assertions.assertEquals(userCreateVersion.id, createdUserTicket.affectsVersion.id);
        Assertions.assertTrue(userCreateRedirect.endsWith("/user/tickets/" + createdUserTicket.id));

        Long ticketId = userTicket == null ? null : userTicket.id;
        User supportUser = User.find("email", "support1@mnemosyne-systems.ai").firstResult();
        User tamUser = User.find("email", "tam1@mnemosyne-systems.ai").firstResult();
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/user/tickets/" + ticketId).then().statusCode(303)
                .header("Location", Matchers.endsWith("/user/tickets/" + ticketId));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/user/tickets/" + ticketId).then()
                .statusCode(200).body("name", Matchers.equalTo(userTicketName))
                .body("title", Matchers.equalTo(userTicket.displayTitle()))
                .body("supportUsers.username", Matchers.hasItem("support1"))
                .body("secondaryUsersLabel", Matchers.equalTo("TAM")).body("editableStatus", Matchers.equalTo(false))
                .body("editableResolvedVersion", Matchers.equalTo(false))
                .body("exportPath", Matchers.equalTo("/tickets/export/" + ticketId));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/user/companies/" + companyId).then().statusCode(303)
                .header("Location", Matchers.endsWith("/user/companies/" + companyId));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/user/companies/" + companyId).then()
                .statusCode(200).body("id", Matchers.equalTo(companyId.intValue()))
                .body("users.username", Matchers.hasItem("user"))
                .body("superusers.username", Matchers.hasItem("superuser1")).body("tamUsers", Matchers.empty());
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/tickets/" + ticketId)
                .then().statusCode(303).header("Location", Matchers.endsWith("/user/tickets/" + ticketId));
        User superuser = User.find("email", "superuser1@mnemosyne-systems.ai").firstResult();
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/user/superuser-users/" + superuser.id).then().statusCode(303)
                .header("Location", Matchers.endsWith("/user/superuser-users/" + superuser.id));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/user/superuser-users/" + superuser.id)
                .then().statusCode(200).body("typeLabel", Matchers.equalTo("Superuser"))
                .body("username", Matchers.equalTo("superuser1"));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/user/support-users/" + supportUser.id).then().statusCode(303)
                .header("Location", Matchers.endsWith("/user/support-users/" + supportUser.id));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/user/support-users/" + supportUser.id)
                .then().statusCode(200).body("typeLabel", Matchers.equalTo("Support"))
                .body("username", Matchers.equalTo("support1"));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/user/tam-users/" + tamUser.id).then().statusCode(303)
                .header("Location", Matchers.endsWith("/user/tam-users/" + tamUser.id));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/user/tam-users/" + tamUser.id).then()
                .statusCode(200).body("typeLabel", Matchers.equalTo("TAM")).body("username", Matchers.equalTo("tam1"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/user/tickets/" + ticketId + "/edit").then().statusCode(303)
                .header("Location", Matchers.endsWith("/user/tickets/" + ticketId));
    }

    @Test
    void userReplyUsesLatestPendingMessageForTicketSlaColor() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureDefaultCategories();
        Long companyId = ensureCompany("User SLA Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyId);
        Level urgent = ensureLevel("Normal", "Immediate response", 1, "Red");
        setTicketSupportLevel(ticket.id, urgent.id);
        ensureTimedMessage(ticket, "Initial requester message", "user@mnemosyne-systems.ai",
                java.time.LocalDateTime.now().minusMinutes(10));

        String cookie = login("user", "user");
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/user/tickets").then().statusCode(200)
                .body("items.find { it.id == " + ticket.id + " }.slaColor", Matchers.equalTo("Red"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .multiPart("body", "Follow-up from requester").post("/user/tickets/" + ticket.id + "/messages").then()
                .statusCode(303);

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/user/tickets").then().statusCode(200)
                .body("items.find { it.id == " + ticket.id + " }.slaColor", Matchers.equalTo("White"));

        ensureTimedMessage(ticket, "Support follow-up", "support1@mnemosyne-systems.ai",
                java.time.LocalDateTime.now().minusMinutes(1));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/user/tickets").then().statusCode(200)
                .body("items.find { it.id == " + ticket.id + " }.slaColor", Matchers.equalTo("White"));
    }

    @Test
    void userTicketSearchMatchesVisibleMessagesOnly() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        Long companyId = ensureCompany("User Search Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai");
        Ticket visibleTicket = ensureTicket(companyId);
        String visibleBody = "user-visible-search-" + System.nanoTime();
        ensureMessageWithBody(visibleTicket, visibleBody);
        String cookie = login("user", "user");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).queryParam("q", visibleBody).get("/api/user/tickets")
                .then().statusCode(200).body("searchTerm", Matchers.equalTo(visibleBody))
                .body("items.name", Matchers.hasItem(visibleTicket.name));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).queryParam("q", "user-search-no-match")
                .get("/api/user/tickets").then().statusCode(200).body("items.size()", Matchers.equalTo(0));
    }

    @Test
    void reactLoginRedirectsUsersToUserTickets() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC).formParam("username", "user")
                .formParam("password", "user").post("/login").then().statusCode(303)
                .header("Location", Matchers.equalTo("/user/tickets"));
    }

    @Test
    void userTicketPdfExportIncludesAttachmentTitlesAndContent() throws Exception {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureDefaultCategories();
        Long companyId = ensureCompany("PDF Export Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyId);
        Message message = ensureMessageWithBody(ticket, "Export this ticket");
        ensureAttachment(message, "notes.txt", "text/plain",
                "Attachment line one\nAttachment line two".getBytes(StandardCharsets.UTF_8));
        ensureAttachment(message, "diagram.png", "image/png", smallPngBytes());
        ensureAttachment(message, "embedded.pdf", "application/pdf", simplePdfBytes("Embedded PDF body"));

        String cookie = login("user", "user");
        byte[] pdfBytes = RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/tickets/export/" + ticket.id)
                .then().statusCode(200).contentType("application/pdf").extract().asByteArray();

        try (PdfReader reader = new PdfReader(pdfBytes)) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder allText = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                allText.append(extractor.getTextFromPage(page));
            }
            String exportText = allText.toString();
            Assertions.assertTrue(exportText.contains("notes.txt"));
            Assertions.assertTrue(exportText.contains("Attachment line one"));
            Assertions.assertTrue(exportText.contains("diagram.png"));
            Assertions.assertTrue(exportText.contains("embedded.pdf"));
        }
    }

    @Test
    void reactArticlesApiRequiresLogin() {
        RestAssured.given().get("/api/articles").then().statusCode(401);
    }

    private byte[] simplePdfBytes(String text) throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph(text));
            document.close();
            return output.toByteArray();
        }
    }

    private byte[] smallPngBytes() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xCC0000);
        image.setRGB(1, 0, 0xFFFFFF);
        image.setRGB(0, 1, 0xFFFFFF);
        image.setRGB(1, 1, 0xCC0000);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }

    @Test
    void reactArticlesApiReturnsArticleListAndDetail() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        String cookie = login("support1", "support1");
        ensureArticle("Zulu Runbook", "ops,prod", "## Zulu article\n\nOperational notes");
        Article article = ensureArticle("Alpha Runbook", "ops,prod", "## Seed article\n\nOperational notes");
        Attachment attachment = ensureArticleAttachment(article, "runbook.txt", "Attachment body");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/articles").then().statusCode(200)
                .body("canCreate", Matchers.equalTo(true)).body("createPath", Matchers.equalTo("/articles/new"))
                .body("items.title", Matchers.hasItems("Alpha Runbook", "Zulu Runbook"));
        List<String> titles = RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/articles").then()
                .statusCode(200).extract().jsonPath().getList("items.title");
        Assertions.assertTrue(titles.indexOf("Alpha Runbook") < titles.indexOf("Zulu Runbook"));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/articles/" + article.id).then()
                .statusCode(200).body("title", Matchers.equalTo("Alpha Runbook"))
                .body("tags", Matchers.equalTo("ops,prod")).body("body", Matchers.containsString("Seed article"))
                .body("canEdit", Matchers.equalTo(true)).body("canDelete", Matchers.equalTo(false))
                .body("editPath", Matchers.equalTo("/articles/" + article.id + "/edit"))
                .body("attachments.name", Matchers.hasItem("runbook.txt"))
                .body("attachments.downloadPath", Matchers.hasItem("/attachments/" + attachment.id + "/data"));
    }

    @Test
    void reactUserMutationsReturnJsonRedirects() {
        ensureUser("user-json", "user-json@mnemosyne-systems.ai", User.TYPE_USER, "user-json");
        ensureDefaultCategories();
        String cookie = login("user-json", "user-json");
        Long companyId = ensureCompany("User Json Co");
        ensureCompanyUsers(companyId, "user-json@mnemosyne-systems.ai");
        Entitlement entitlement = ensureEntitlement("User Json Entitlement", "User json detail");
        Version version = ensureVersion(entitlement, "10.0.0", java.time.LocalDate.of(2026, 2, 1));
        CompanyEntitlement entry = ensureCompanyEntitlement(companyId, entitlement);
        Assertions.assertNotNull(entry);
        Assertions.assertNotNull(version);

        String redirectTo = RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie)
                .header("X-Billetsys-Client", "react").multiPart("status", "Open")
                .multiPart("title", "User json redirect title").multiPart("message", "User json redirect create")
                .multiPart("companyId", companyId).multiPart("companyEntitlementId", entry.id)
                .multiPart("categoryId", Category.findDefault().id).multiPart("affectsVersionId", version.id)
                .post("/user/tickets").then().statusCode(200)
                .body("redirectTo", Matchers.matchesPattern("/user/tickets/\\d+")).extract().path("redirectTo");
        Long createdTicketId = Long.valueOf(redirectTo.substring(redirectTo.lastIndexOf('/') + 1));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).header("X-Billetsys-Client", "react")
                .contentType(ContentType.URLENC).formParam("title", "User json redirect title")
                .formParam("affectsVersionId", version.id).post("/user/tickets/" + createdTicketId).then()
                .statusCode(200).body("redirectTo", Matchers.equalTo("/user/tickets/" + createdTicketId));
    }

}
