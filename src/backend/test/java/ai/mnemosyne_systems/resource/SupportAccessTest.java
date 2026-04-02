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
import io.restassured.response.Response;
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
class SupportAccessTest extends AccessTestSupport {

    @Test
    void supportCanAccessSupportUsersMenu() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("support2", "support2@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support2");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Support Co");
        ensureCompanyUsers(companyId, "tam1@mnemosyne-systems.ai");
        ai.mnemosyne_systems.model.Ticket supportTicket = ensureTicket(companyId);
        ensureMessage(supportTicket, "Sample ticket created.");
        String supportTicketName = supportTicket == null ? "" : supportTicket.name;
        String cookie = login("support1", "support1");

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/support/users/" + companyId).then().statusCode(303)
                .header("Location", Matchers.endsWith("/support/users?companyId=" + companyId));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).queryParam("companyId", companyId)
                .get("/api/support/users").then().statusCode(200).body("title", Matchers.equalTo("Users"))
                .body("selectedCompanyId", Matchers.equalTo(companyId.intValue()))
                .body("createPath", Matchers.equalTo("/support/users/new?companyId=" + companyId))
                .body("items.username", Matchers.hasItem("tam1"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/support").then()
                .statusCode(303).header("Location", Matchers.endsWith("/support/tickets"));
        ai.mnemosyne_systems.model.User supportUser = ai.mnemosyne_systems.model.User
                .find("email", "support1@mnemosyne-systems.ai").firstResult();
        int assignedCount = ai.mnemosyne_systems.model.Ticket.find(
                "select distinct t from Ticket t join t.supportUsers u where u = ?1 and (t.status is null or lower(t.status) <> 'closed')",
                supportUser).list().size();
        int openCount = ai.mnemosyne_systems.model.Ticket
                .find("select distinct t from Ticket t where t.supportUsers is empty").list().size();
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/support/tickets").then().statusCode(200)
                .body("title", Matchers.equalTo("Tickets")).body("assignedCount", Matchers.equalTo(assignedCount))
                .body("openCount", Matchers.equalTo(openCount)).body("items.name", Matchers.hasItem(supportTicketName))
                .body("items.status", Matchers.hasItem("Assigned"))
                .body("items.supportUser.username", Matchers.hasItem("support1"));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/support/open").then()
                .statusCode(303).header("Location", Matchers.endsWith("/support/tickets/open"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).queryParam("view", "open")
                .get("/api/support/tickets").then().statusCode(200).body("title", Matchers.equalTo("Open tickets"))
                .body("items.name", Matchers.hasItems("A-00002", "A-00003"));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/support/closed")
                .then().statusCode(303).header("Location", Matchers.endsWith("/support/tickets/closed"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).queryParam("view", "closed")
                .get("/api/support/tickets").then().statusCode(200).body("title", Matchers.equalTo("Closed tickets"))
                .body("items.name", Matchers.hasItem("A-00004"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/support/tickets/create").then().statusCode(303)
                .header("Location", Matchers.endsWith("/support/tickets/new"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/support/tickets/bootstrap").then()
                .statusCode(200).body("submitPath", Matchers.equalTo("/support/tickets"))
                .body("categories.name", Matchers.hasItem("Question"))
                .body("companyEntitlements.size()", Matchers.greaterThanOrEqualTo(1))
                .body("ticketName", Matchers.not(Matchers.isEmptyOrNullString()));
        CompanyEntitlement supportCreateEntitlement = CompanyEntitlement
                .find("company.id = ?1 order by id asc", companyId).firstResult();
        Assertions.assertNotNull(supportCreateEntitlement);
        Version supportCreateVersion = Version
                .find("entitlement = ?1 order by date asc, id asc", supportCreateEntitlement.entitlement).firstResult();
        Assertions.assertNotNull(supportCreateVersion);
        String supportCreateMessage = "Support create redirect coverage";
        String supportCreateRedirect = RestAssured.given().redirects().follow(false)
                .cookie(AuthHelper.AUTH_COOKIE, cookie).multiPart("status", "Open")
                .multiPart("title", "Support create redirect title").multiPart("message", supportCreateMessage)
                .multiPart("companyId", companyId).multiPart("companyEntitlementId", supportCreateEntitlement.id)
                .multiPart("categoryId", Category.findDefault().id)
                .multiPart("affectsVersionId", supportCreateVersion.id).post("/support/tickets").then().statusCode(303)
                .extract().header("Location");
        Message createdSupportMessage = Message.find("body", supportCreateMessage).firstResult();
        Assertions.assertNotNull(createdSupportMessage);
        Ticket createdSupportTicket = createdSupportMessage.ticket;
        Assertions.assertNotNull(createdSupportTicket);
        Assertions.assertNotNull(createdSupportTicket.affectsVersion);
        Assertions.assertEquals(supportCreateVersion.id, createdSupportTicket.affectsVersion.id);
        Assertions.assertTrue(supportCreateRedirect.endsWith("/support/tickets/" + createdSupportTicket.id));

        Long ticketId = supportTicket == null ? null : supportTicket.id;
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/tickets/" + ticketId + "/edit").then().statusCode(303)
                .header("Location", Matchers.endsWith("/tickets/" + ticketId + "/edit"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/ticket-workbench/" + ticketId).then()
                .statusCode(200).body("entitlements.name", Matchers.hasItem(Matchers.containsString("Starter")))
                .body("messages.body", Matchers.hasItem("Sample ticket created."));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/tickets/1/edit")
                .then().statusCode(303).header("Location", Matchers.endsWith("/tickets/1/edit"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/ticket-workbench/1").then().statusCode(200)
                .body("ticket.externalIssueLink",
                        Matchers.equalTo("https://github.com/mnemosyne-systems/billetsys/issues/6"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie)
                .queryParam("url", "https://github.com/mnemosyne-systems/billetsys/issues/6")
                .get("/tickets/external-preview").then().statusCode(200).body(Matchers
                        .containsString("<title>https://github.com/mnemosyne-systems/billetsys/issues/6</title>"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/support/tickets/" + ticketId).then().statusCode(303)
                .header("Location", Matchers.endsWith("/support/tickets/" + ticketId));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/support/tickets/" + ticketId).then()
                .statusCode(200).body("messages.body", Matchers.hasItem("Sample ticket created."))
                .body("supportUsers.username", Matchers.hasItem("support1"))
                .body("tamUsers.username", Matchers.hasItem("tam1")).body("companyName", Matchers.equalTo("Support Co"))
                .body("entitlementName", Matchers.not(Matchers.isEmptyOrNullString()))
                .body("levelName", Matchers.equalTo("Normal")).body("categoryName", Matchers.nullValue())
                .body("statusOptions", Matchers.hasItems("Open", "Assigned", "In Progress", "Resolved", "Closed"));
        ai.mnemosyne_systems.model.Ticket unassignedTicket = ensureUnassignedOpenTicket(companyId);
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .multiPart("body", "Auto assign on support reply")
                .post("/support/tickets/" + unassignedTicket.id + "/messages").then().statusCode(303);
        Ticket autoAssignedTicket = refreshedTicket(unassignedTicket.id);
        Assertions.assertEquals("Assigned", autoAssignedTicket.status);
        Assertions.assertTrue(ticketHasSupportUser(unassignedTicket.id, supportUser.id));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("status", "Assigned")
                .formParam("companyId", supportTicket.company.id)
                .formParam("companyEntitlementId", supportTicket.companyEntitlement.id)
                .formParam("affectsVersionId", ensureVersion(supportTicket.companyEntitlement.entitlement, "1.0.0").id)
                .post("/support/tickets/" + ticketId).then().statusCode(303);
        Ticket updatedSupportTicket = refreshedTicket(ticketId);
        Assertions.assertEquals("Assigned", updatedSupportTicket.status);
        Assertions.assertTrue(ticketHasSupportUser(ticketId, supportUser.id));
        String userCookie = login("user", "user");
        String tamCookie = login("tam1", "tam1");
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, userCookie)
                .get("/tickets/" + ticketId).then().statusCode(303)
                .header("Location", Matchers.endsWith("/user/tickets/" + ticketId));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, userCookie).get("/api/user/tickets/" + ticketId).then()
                .statusCode(200).body("displayStatus", Matchers.equalTo("Assigned"));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, tamCookie)
                .get("/tickets/" + ticketId).then().statusCode(303)
                .header("Location", Matchers.endsWith("/user/tickets/" + ticketId));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, tamCookie).get("/api/user/tickets/" + ticketId).then()
                .statusCode(200).body("displayStatus", Matchers.equalTo("Assigned"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/profile").then()
                .statusCode(303).header("Location", Matchers.endsWith("/profile"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/profile/password")
                .then().statusCode(303).header("Location", Matchers.endsWith("/profile/password"));
    }

    @Test
    void articlesRespectRolePermissions() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN, "admin");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        Article seeded = ensureArticle("Runbook", "ops,prod", "Seed article");
        String supportCookie = login("support1", "support1");
        String tamCookie = login("tam1", "tam1");
        String userCookie = login("user", "user");
        String adminCookie = login("admin", "admin");

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, supportCookie).get("/articles")
                .then().statusCode(303).header("Location", Matchers.endsWith("/articles"));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, supportCookie)
                .get("/articles/create").then().statusCode(303).header("Location", Matchers.endsWith("/articles/new"));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, supportCookie)
                .contentType("multipart/form-data").multiPart("title", "Article One").multiPart("tags", "guide")
                .multiPart("body", "![pic](attachment://article.png)\n\n**Important**")
                .multiPart("attachments", "article.png", "img".getBytes(StandardCharsets.UTF_8), "image/png")
                .post("/articles").then().statusCode(303);

        Article created = Article.find("title", "Article One").firstResult();
        Assertions.assertNotNull(created);
        Assertions.assertTrue(created.body.contains("/attachments/"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, tamCookie).get("/articles/create")
                .then().statusCode(303).header("Location", Matchers.endsWith("/articles/new"));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, userCookie)
                .get("/articles/" + created.id).then().statusCode(303)
                .header("Location", Matchers.endsWith("/articles/" + created.id));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, supportCookie)
                .get("/articles/" + created.id).then().statusCode(303)
                .header("Location", Matchers.endsWith("/articles/" + created.id));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, userCookie).get("/articles/create")
                .then().statusCode(303);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, adminCookie).get("/articles")
                .then().statusCode(303).header("Location", Matchers.endsWith("/articles"));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, adminCookie)
                .get("/articles/create").then().statusCode(303);
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, adminCookie)
                .get("/articles/" + seeded.id).then().statusCode(303)
                .header("Location", Matchers.endsWith("/articles/" + seeded.id));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, adminCookie)
                .post("/articles/" + seeded.id + "/delete").then().statusCode(303);
        Assertions.assertNull(refreshedArticle(seeded.id));
    }

    @Test
    void supportCanManageTicketsAndMessages() {
        ensureUser("support3", "support3@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support3");
        ensureDefaultCategories();
        String cookie = login("support3", "support3");
        Long companyId = ensureCompany("Support CRUD Co");
        Company company = Company.findById(companyId);
        Entitlement entitlement = ensureEntitlement("Starter", "Email support");
        Level level = ensureLevel("Normal", "Normal response level", 1440, "White");
        CompanyEntitlement entry = ensureCompanyEntitlement(company, entitlement, level);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("status", "Open").formParam("title", "CRUD create title")
                .formParam("companyId", company.id).formParam("companyEntitlementId", entry.id).post("/tickets").then()
                .statusCode(303);
        Ticket ticket = Ticket.find("company = ?1 order by id desc", company).firstResult();
        Assertions.assertNotNull(ticket);
        Assertions.assertNotNull(ticket.category);
        Assertions.assertEquals("Question", ticket.category.name);

        Category bugCategory = Category.find("name", "Bug").firstResult();
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("status", "In Progress").formParam("companyId", company.id)
                .formParam("companyEntitlementId", entry.id).formParam("categoryId", bugCategory.id)
                .formParam("affectsVersionId", ensureVersion(entry.entitlement, "1.0.0").id)
                .formParam("externalIssueLink", "https://github.com/example/issue/1").post("/tickets/" + ticket.id)
                .then().statusCode(303);
        Ticket updatedTicket = refreshedTicket(ticket.id);
        Assertions.assertEquals("In Progress", updatedTicket.status);
        Assertions.assertNotNull(updatedTicket.category);
        Assertions.assertEquals("Bug", updatedTicket.category.name);
        Assertions.assertEquals("https://github.com/example/issue/1", updatedTicket.externalIssueLink);
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/tickets").then()
                .statusCode(303).header("Location", Matchers.endsWith("/tickets"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/ticket-workbench").then().statusCode(200)
                .body("items.name", Matchers.hasItem(ticket.name));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/tickets/new").then()
                .statusCode(303).header("Location", Matchers.endsWith("/tickets/new"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/ticket-workbench/bootstrap").then()
                .statusCode(200).body("title", Matchers.equalTo("New ticket"));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/tickets/" + ticket.id + "/edit").then().statusCode(303)
                .header("Location", Matchers.endsWith("/tickets/" + ticket.id + "/edit"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/ticket-workbench/" + ticket.id).then()
                .statusCode(200)
                .body("ticket.externalIssueLink", Matchers.equalTo("https://github.com/example/issue/1"));

        byte[] attachmentData = "Attachment line one\nAttachment line two".getBytes(StandardCharsets.UTF_8);
        byte[] attachmentDataTwo = "Second attachment".getBytes(StandardCharsets.UTF_8);
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .multiPart("body", "Support note").multiPart("date", "2024-01-01T10:00")
                .multiPart("ticketId", String.valueOf(ticket.id))
                .multiPart("attachments", "note.txt", attachmentData, "text/plain")
                .multiPart("attachments", "note-2.txt", attachmentDataTwo, "text/plain").post("/messages").then()
                .statusCode(303);
        Message message = Message.find("ticket = ?1 and body = ?2", ticket, "Support note").firstResult();
        Assertions.assertNotNull(message);
        List<Attachment> attachments = Attachment.list("message = ?1 order by id", message);
        Assertions.assertEquals(2, attachments.size());
        Attachment attachment = attachments.get(0);
        Assertions.assertEquals("note.txt", attachment.name);
        Assertions.assertEquals("text/plain", attachment.mimeType);
        Assertions.assertEquals(attachmentData.length, attachment.data.length);
        Attachment secondAttachment = attachments.get(1);
        Assertions.assertEquals("note-2.txt", secondAttachment.name);
        Assertions.assertEquals(attachmentDataTwo.length, secondAttachment.data.length);
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/messages").then()
                .statusCode(303).header("Location", Matchers.endsWith("/messages"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/messages").then().statusCode(200)
                .body("items.preview", Matchers.hasItem(Matchers.containsString("Support note")));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/messages/new").then()
                .statusCode(303).header("Location", Matchers.endsWith("/messages/new"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/messages/bootstrap").then().statusCode(200)
                .body("title", Matchers.equalTo("New message"));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/messages/" + message.id + "/edit").then().statusCode(303)
                .header("Location", Matchers.endsWith("/messages/" + message.id + "/edit"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).queryParam("messageId", message.id)
                .get("/api/messages/bootstrap").then().statusCode(200)
                .body("message.body", Matchers.equalTo("Support note"))
                .body("attachments.name", Matchers.hasItems("note.txt", "note-2.txt"));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/support/tickets/" + ticket.id).then()
                .statusCode(200)
                .body("messages.attachments.name.flatten()", Matchers.hasItems("note.txt", "note-2.txt"))
                .body("messages.attachments.mimeType.flatten()", Matchers.hasItem("text/plain"))
                .body("categoryName", Matchers.equalTo("Bug"))
                .body("externalIssueLink", Matchers.equalTo("https://github.com/example/issue/1"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/attachments/" + attachment.id).then().statusCode(303)
                .header("Location", Matchers.endsWith("/attachments/" + attachment.id));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/attachments/" + attachment.id).then()
                .statusCode(200).body("name", Matchers.equalTo("note.txt"))
                .body("lines.content", Matchers.hasItem("Attachment line two"));

        byte[] replyData = "Reply attachment".getBytes(StandardCharsets.UTF_8);
        byte[] replyDataTwo = "Second reply attachment".getBytes(StandardCharsets.UTF_8);
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .multiPart("body", "Reply with attachments")
                .multiPart("attachments", "reply.txt", replyData, "text/plain")
                .multiPart("attachments", "reply-2.txt", replyDataTwo, "text/plain")
                .post("/support/tickets/" + ticket.id + "/messages").then().statusCode(303);
        Message replyMessage = Message.find("ticket = ?1 and body = ?2", ticket, "Reply with attachments")
                .firstResult();
        Assertions.assertNotNull(replyMessage);
        List<Attachment> replyAttachments = Attachment.list("message = ?1 order by id", replyMessage);
        Assertions.assertEquals(2, replyAttachments.size());
        Assertions.assertEquals("reply.txt", replyAttachments.get(0).name);
        Assertions.assertEquals("reply-2.txt", replyAttachments.get(1).name);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .multiPart("body", "Support note updated").multiPart("date", "2024-01-01T11:00")
                .multiPart("ticketId", String.valueOf(ticket.id)).post("/messages/" + message.id).then()
                .statusCode(303);
        Message updatedMessage = refreshedMessage(message.id);
        Assertions.assertEquals("Support note updated", updatedMessage.body);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .post("/messages/" + message.id + "/delete").then().statusCode(303);
        Assertions.assertNull(refreshedMessage(message.id));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .post("/tickets/" + ticket.id + "/delete").then().statusCode(303);
        Assertions.assertNull(refreshedTicket(ticket.id));
    }

    @Test
    void supportTicketDetailUpdatesFieldsAndAddsRepliesWithAndWithoutAttachments() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureDefaultCategories();
        Long companyId = ensureCompany("Support Detail Flow Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyId);
        ensureMessage(ticket, "Original support detail message");
        String cookie = login("support1", "support1");

        Category bugCategory = Category.find("name", "Bug").firstResult();
        Assertions.assertNotNull(bugCategory);
        Version affectsVersion = ensureVersion(ticket.companyEntitlement.entitlement, "2.0.0",
                java.time.LocalDate.of(2024, 2, 1));
        Version resolvedVersion = ensureVersion(ticket.companyEntitlement.entitlement, "2.1.0",
                java.time.LocalDate.of(2024, 3, 1));
        String externalIssueLink = "https://github.com/mnemosyne-systems/billetsys/issues/6";

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("status", "Resolved")
                .formParam("companyId", ticket.company.id)
                .formParam("companyEntitlementId", ticket.companyEntitlement.id).formParam("categoryId", bugCategory.id)
                .formParam("externalIssueLink", externalIssueLink).formParam("affectsVersionId", affectsVersion.id)
                .formParam("resolvedVersionId", resolvedVersion.id).post("/support/tickets/" + ticket.id).then()
                .statusCode(303).header("Location", Matchers.endsWith("/support/tickets/" + ticket.id));

        Ticket updatedTicket = refreshedTicket(ticket.id);
        Assertions.assertEquals("Resolved", updatedTicket.status);
        Assertions.assertEquals(bugCategory.id, updatedTicket.category.id);
        Assertions.assertEquals(externalIssueLink, updatedTicket.externalIssueLink);
        Assertions.assertEquals(affectsVersion.id, updatedTicket.affectsVersion.id);
        Assertions.assertEquals(resolvedVersion.id, updatedTicket.resolvedVersion.id);

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/support/tickets/" + ticket.id).then()
                .statusCode(200).body("actionPath", Matchers.equalTo("/support/tickets/" + ticket.id))
                .body("messageActionPath", Matchers.equalTo("/support/tickets/" + ticket.id + "/messages"))
                .body("displayStatus", Matchers.equalTo("Resolved")).body("categoryName", Matchers.equalTo("Bug"))
                .body("externalIssueLink", Matchers.equalTo(externalIssueLink))
                .body("affectsVersionId", Matchers.equalTo(affectsVersion.id.intValue()))
                .body("resolvedVersionId", Matchers.equalTo(resolvedVersion.id.intValue()))
                .body("messages.body", Matchers.hasItem("Original support detail message"));

        String plainReplyBody = "Support reply without attachments " + System.nanoTime();
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .multiPart("body", plainReplyBody).post("/support/tickets/" + ticket.id + "/messages").then()
                .statusCode(303)
                .header("Location", Matchers.endsWith("/support/tickets/" + ticket.id + "?replyAdded=1"));

        Message plainReply = Message.find("ticket = ?1 and body = ?2", ticket, plainReplyBody).firstResult();
        Assertions.assertNotNull(plainReply);
        Assertions.assertTrue(Attachment.list("message", plainReply).isEmpty());

        byte[] replyAttachmentData = "Support detail attachment".getBytes(StandardCharsets.UTF_8);
        String attachmentReplyBody = "Support reply with attachments " + System.nanoTime();
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .multiPart("body", attachmentReplyBody)
                .multiPart("attachments", "detail-reply.txt", replyAttachmentData, "text/plain")
                .post("/support/tickets/" + ticket.id + "/messages").then().statusCode(303)
                .header("Location", Matchers.endsWith("/support/tickets/" + ticket.id + "?replyAdded=1"));

        Message attachmentReply = Message.find("ticket = ?1 and body = ?2", ticket, attachmentReplyBody).firstResult();
        Assertions.assertNotNull(attachmentReply);
        List<Attachment> replyAttachments = Attachment.list("message = ?1 order by id", attachmentReply);
        Assertions.assertEquals(1, replyAttachments.size());
        Assertions.assertEquals("detail-reply.txt", replyAttachments.get(0).name);
        Assertions.assertEquals("text/plain", replyAttachments.get(0).mimeType);
        Assertions.assertEquals(replyAttachmentData.length, replyAttachments.get(0).data.length);

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/support/tickets/" + ticket.id).then()
                .statusCode(200).body("messages.body", Matchers.hasItems(plainReplyBody, attachmentReplyBody))
                .body("messages.attachments.name.flatten()", Matchers.hasItem("detail-reply.txt"))
                .body("messages.attachments.mimeType.flatten()", Matchers.hasItem("text/plain"));
    }

    @Test
    void attachmentsPageUsesRoleHeader() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        Long companyId = ensureCompany("Attachment Role Co");
        ensureCompanyUsers(companyId, "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyId);
        Message message = ensureMessageWithBody(ticket, "Attachment role message");
        Attachment attachment = ensureAttachment(message, "role-attachment.txt");

        String supportCookie = login("support1", "support1");
        User supportUser = User.find("email", "support1@mnemosyne-systems.ai").firstResult();
        SupportResource.SupportTicketCounts counts = SupportResource.loadTicketCounts(supportUser);
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, supportCookie)
                .get("/attachments/" + attachment.id).then().statusCode(303)
                .header("Location", Matchers.endsWith("/attachments/" + attachment.id));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, supportCookie).get("/api/attachments/" + attachment.id)
                .then().statusCode(200).body("backPath", Matchers.equalTo("/support/tickets/" + ticket.id));

        String tamCookie = login("tam1", "tam1");
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, tamCookie).get("/api/attachments/" + attachment.id).then()
                .statusCode(200).body("backPath", Matchers.equalTo("/user/tickets/" + ticket.id));

        String userCookie = login("user", "user");
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, userCookie).get("/api/attachments/" + attachment.id).then()
                .statusCode(200).body("backPath", Matchers.equalTo("/user/tickets/" + ticket.id));
    }

    @Test
    void sendsEmailsForTicketMessageAndStatusChanges() {
        mailbox.clear();
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        Long companyId = ensureCompany("Email Notify Co");
        ensureCompanyUsers(companyId, "tam1@mnemosyne-systems.ai", "user@mnemosyne-systems.ai");
        Ticket ticket = ensureUnassignedOpenTicket(companyId);
        String supportCookie = login("support1", "support1");

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, supportCookie)
                .multiPart("body", "Email notification reply " + System.nanoTime())
                .multiPart("attachments", "notify.txt", "notify".getBytes(StandardCharsets.UTF_8), "text/plain")
                .post("/support/tickets/" + ticket.id + "/messages").then().statusCode(303);

        List<Mail> userMessages = mailbox.getMailsSentTo("user@mnemosyne-systems.ai");
        Assertions.assertFalse(userMessages.isEmpty());
        Mail firstMail = userMessages.get(userMessages.size() - 1);
        Assertions.assertTrue(firstMail.getSubject().contains("[" + ticket.name + "]"));
        Assertions.assertTrue(firstMail.getText().contains("Message"));
        Assertions.assertEquals(1, firstMail.getAttachments().size());

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, supportCookie)
                .contentType(ContentType.URLENC).formParam("status", "Closed").formParam("companyId", ticket.company.id)
                .formParam("companyEntitlementId", ticket.companyEntitlement.id)
                .formParam("affectsVersionId", ensureVersion(ticket.companyEntitlement.entitlement, "1.0.0").id)
                .post("/support/tickets/" + ticket.id).then().statusCode(303);

        List<Mail> tamMessages = mailbox.getMailsSentTo("tam1@mnemosyne-systems.ai");
        Assertions.assertFalse(tamMessages.isEmpty());
        Mail statusMail = tamMessages.get(tamMessages.size() - 1);
        Assertions.assertTrue(statusMail.getText().contains("Status"));
        Assertions.assertTrue(statusMail.getText().contains("Closed"));
    }

    @Test
    void sendsCorrectEmailsForTicketCreationAcrossRoles() {
        mailbox.clear();
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("superuser1", "superuser1@mnemosyne-systems.ai", User.TYPE_SUPERUSER, "superuser1");
        ensureDefaultCategories();

        Entitlement supportEntitlement = ensureEntitlement("Support Create Email Entitlement",
                "Support create email assertions");
        Version supportVersion = ensureVersion(supportEntitlement, "5.0.0", java.time.LocalDate.of(2025, 1, 1));
        Long supportCompanyId = ensureCompany("Support Email Create Co");
        ensureCompanyUsers(supportCompanyId, "tam1@mnemosyne-systems.ai");
        CompanyEntitlement supportCompanyEntitlement = ensureCompanyEntitlement(supportCompanyId, supportEntitlement);
        String supportBody = "Support create mail body " + System.nanoTime();
        String supportCookie = login("support1", "support1");
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, supportCookie)
                .multiPart("status", "Open").multiPart("title", "Support mail title").multiPart("message", supportBody)
                .multiPart("companyId", supportCompanyId)
                .multiPart("companyEntitlementId", supportCompanyEntitlement.id)
                .multiPart("categoryId", Category.findDefault().id).multiPart("affectsVersionId", supportVersion.id)
                .post("/support/tickets").then().statusCode(303);
        Ticket createdSupportTicket = findMessageByBody(supportBody).ticket;
        assertTicketCreateMail(latestMailTo("support1@mnemosyne-systems.ai"), createdSupportTicket, "support1", "Open",
                supportBody);

        Entitlement userEntitlement = ensureEntitlement("User Create Email Entitlement",
                "User create email assertions");
        Version userVersion = ensureVersion(userEntitlement, "6.0.0", java.time.LocalDate.of(2025, 2, 1));
        Long userCompanyId = ensureCompany("User Email Create Co");
        ensureCompanyUsers(userCompanyId, "user@mnemosyne-systems.ai");
        CompanyEntitlement userCompanyEntitlement = ensureCompanyEntitlement(userCompanyId, userEntitlement);
        String userBody = "User create mail body " + System.nanoTime();
        String userCookie = login("user", "user");
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, userCookie)
                .multiPart("status", "Open").multiPart("title", "User mail title").multiPart("message", userBody)
                .multiPart("companyId", userCompanyId).multiPart("companyEntitlementId", userCompanyEntitlement.id)
                .multiPart("categoryId", Category.findDefault().id).multiPart("affectsVersionId", userVersion.id)
                .post("/user/tickets").then().statusCode(303);
        Ticket createdUserTicket = findMessageByBody(userBody).ticket;
        assertTicketCreateMail(latestMailTo("user@mnemosyne-systems.ai"), createdUserTicket, "user", "Open", userBody);

        Entitlement superuserEntitlement = ensureEntitlement("Superuser Create Email Entitlement",
                "Superuser create email assertions");
        Version superuserVersion = ensureVersion(superuserEntitlement, "7.0.0", java.time.LocalDate.of(2025, 3, 1));
        Long superuserCompanyId = ensureCompany("Superuser Email Create Co");
        ensureCompanyUsers(superuserCompanyId, "superuser1@mnemosyne-systems.ai");
        CompanyEntitlement superuserCompanyEntitlement = ensureCompanyEntitlement(superuserCompanyId,
                superuserEntitlement);
        String superuserBody = "Superuser create mail body " + System.nanoTime();
        String superuserCookie = login("superuser1", "superuser1");
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, superuserCookie)
                .multiPart("status", "Open").multiPart("title", "Superuser mail title")
                .multiPart("message", superuserBody).multiPart("companyId", superuserCompanyId)
                .multiPart("companyEntitlementId", superuserCompanyEntitlement.id)
                .multiPart("categoryId", Category.findDefault().id).multiPart("affectsVersionId", superuserVersion.id)
                .post("/superuser/tickets").then().statusCode(303);
        Ticket createdSuperuserTicket = findMessageByBody(superuserBody).ticket;
        assertTicketCreateMail(latestMailTo("superuser1@mnemosyne-systems.ai"), createdSuperuserTicket, "superuser1",
                "Open", superuserBody);
    }

    @Test
    void reactAppShellRedirectsToStaticIndex() {
        Response response = RestAssured.given().redirects().follow(false).get("/app/");
        int statusCode = response.statusCode();
        if (statusCode == 200) {
            response.then().body(Matchers.containsString("id=\"root\"")).body(Matchers.containsString("/assets/"));
            return;
        }
        response.then().statusCode(Matchers.anyOf(Matchers.equalTo(302), Matchers.equalTo(303))).header("Location",
                Matchers.endsWith("/"));
    }

    @Test
    void reactAppSessionReturnsGuestStateWhenSignedOut() {
        RestAssured.given().get("/api/app/session").then().statusCode(200)
                .body("authenticated", Matchers.equalTo(false)).body("homePath", Matchers.equalTo("/login"))
                .body("notices",
                        Matchers.hasItem("The React shell now uses clean URLs for login, tickets, and admin pages."));
    }

    @Test
    void reactAppSessionReturnsRoleAwareLinksWhenSignedIn() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        String cookie = login("user", "user");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/app/session").then().statusCode(200)
                .body("authenticated", Matchers.equalTo(true)).body("username", Matchers.equalTo("user"))
                .body("role", Matchers.equalTo("user")).body("homePath", Matchers.equalTo("/user/tickets"))
                .body("navigation.href", Matchers.hasItem("/user/tickets"))
                .body("navigation.href", Matchers.hasItem("/articles"))
                .body("navigation.href", Matchers.hasItem("/profile")).body("notices", Matchers
                        .hasItem("The React shell now covers tickets, admin management, profile, and reports."));
    }

    @Test
    void reactAppSessionReturnsSupportTicketLink() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        String cookie = login("support1", "support1");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/app/session").then().statusCode(200)
                .body("role", Matchers.equalTo("support")).body("homePath", Matchers.equalTo("/support/tickets"))
                .body("navigation.href", Matchers.hasItem("/support/tickets"));
    }

    @Test
    void reactLoginRedirectsSupportToSupportTickets() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC).formParam("username", "support1")
                .formParam("password", "support1").post("/login").then().statusCode(303)
                .header("Location", Matchers.equalTo("/support/tickets"));
    }

    @Test
    void incomingEmailWithTicketSubjectAddsMessageAndAttachments() {
        mailbox.clear();
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        Long companyId = ensureCompany("Incoming Existing Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyId);
        String body = "Incoming body " + System.nanoTime();

        RestAssured.given().contentType("multipart/form-data").multiPart("from", "user@mnemosyne-systems.ai")
                .multiPart("subject", "[" + ticket.name + "] Re: update").multiPart("body", body)
                .multiPart("attachments", "incoming.txt", "incoming".getBytes(StandardCharsets.UTF_8), "text/plain")
                .post("/mail/incoming").then().statusCode(200);

        Message saved = findMessageByBody(body);
        Assertions.assertNotNull(saved);
        Assertions.assertEquals(ticket.id, saved.ticket.id);
        List<Attachment> attachments = Attachment.find("message = ?1", saved).list();
        Assertions.assertEquals(1, attachments.size());

        List<Mail> userMessages = mailbox.getMailsSentTo("user@mnemosyne-systems.ai");
        Assertions.assertFalse(userMessages.isEmpty());
        Assertions.assertTrue(userMessages.get(userMessages.size() - 1).getSubject().contains("[" + ticket.name + "]"));
    }

    @Test
    void incomingEmailWithoutTicketSubjectCreatesTicket() {
        mailbox.clear();
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        Long companyId = ensureCompany("Incoming New Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai");
        ensureTicket(companyId);
        String body = "Create ticket from incoming mail " + System.nanoTime();

        RestAssured.given().contentType("multipart/form-data").multiPart("from", "user@mnemosyne-systems.ai")
                .multiPart("subject", "Need support now").multiPart("body", body).post("/mail/incoming").then()
                .statusCode(200);

        Message saved = findMessageByBody(body);
        Assertions.assertNotNull(saved);
        Assertions.assertNotNull(saved.ticket);
        Assertions.assertNotNull(saved.ticket.name);
        Assertions.assertTrue(saved.ticket.name.contains("-"));
    }

    @Test
    void incomingEmailIgnoresUnknownOrMismatchedFrom() {
        mailbox.clear();
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("other", "other@mnemosyne-systems.ai", User.TYPE_USER, "other");
        ensureUser("orphan", "orphan@mnemosyne-systems.ai", User.TYPE_USER, "orphan");
        Long companyA = ensureCompany("Incoming Match Co");
        Long companyB = ensureCompany("Incoming Other Co");
        ensureCompanyUsers(companyA, "user@mnemosyne-systems.ai");
        ensureCompanyUsers(companyB, "other@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyA);
        String unknownBody = "Unknown user incoming " + System.nanoTime();
        String mismatchBody = "Mismatched user incoming " + System.nanoTime();
        String orphanBody = "Orphan user incoming " + System.nanoTime();

        RestAssured.given().contentType("multipart/form-data").multiPart("from", "missing@mnemosyne-systems.ai")
                .multiPart("subject", "[" + ticket.name + "] Not allowed").multiPart("body", unknownBody)
                .post("/mail/incoming").then().statusCode(202);
        Assertions.assertNull(findMessageByBody(unknownBody));

        RestAssured.given().contentType("multipart/form-data").multiPart("from", "other@mnemosyne-systems.ai")
                .multiPart("subject", "[" + ticket.name + "] Not allowed").multiPart("body", mismatchBody)
                .post("/mail/incoming").then().statusCode(202);
        Assertions.assertNull(findMessageByBody(mismatchBody));

        RestAssured.given().contentType("multipart/form-data").multiPart("from", "orphan@mnemosyne-systems.ai")
                .multiPart("subject", "Create ticket").multiPart("body", orphanBody).post("/mail/incoming").then()
                .statusCode(202);
        Assertions.assertNull(findMessageByBody(orphanBody));
    }

    @Test
    void incomingEmailRequiresBody() {
        RestAssured.given().contentType("multipart/form-data").multiPart("from", "user@mnemosyne-systems.ai")
                .multiPart("subject", "Missing body").post("/mail/incoming").then().statusCode(400);

        RestAssured.given().contentType("multipart/form-data").multiPart("from", "user@mnemosyne-systems.ai")
                .multiPart("subject", "Blank body").multiPart("body", "   ").post("/mail/incoming").then()
                .statusCode(400);
    }

    @Test
    void incomingEmailNormalizesSenderAndAssignsCompanyTamsOnNewTicket() {
        mailbox.clear();
        ensureUser("normalize-user", "normalize-user@mnemosyne-systems.ai", User.TYPE_USER, "normalize-user");
        ensureUser("normalize-tam", "normalize-tam@mnemosyne-systems.ai", User.TYPE_TAM, "normalize-tam");
        Long companyId = ensureCompany("Incoming Normalize Co");
        ensureCompanyUsers(companyId, "normalize-user@mnemosyne-systems.ai", "normalize-tam@mnemosyne-systems.ai");
        ensureTicket(companyId);
        String body = "Normalized incoming " + System.nanoTime();

        RestAssured.given().contentType("multipart/form-data")
                .multiPart("from", " NORMALIZE-USER@MNEMOSYNE-SYSTEMS.AI ")
                .multiPart("subject", "Create normalized ticket").multiPart("body", body).post("/mail/incoming").then()
                .statusCode(200);

        Message saved = findMessageByBody(body);
        Assertions.assertNotNull(saved);
        Assertions.assertNotNull(saved.ticket);
        User tamUser = User.find("email", "normalize-tam@mnemosyne-systems.ai").firstResult();
        Assertions.assertNotNull(tamUser);
        Assertions.assertTrue(ticketHasTamUser(saved.ticket.id, tamUser.id));
    }

    @Test
    void mailboxMessageWithTicketSubjectAddsMessageAndAttachments() throws Exception {
        mailbox.clear();
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        Long companyId = ensureCompany("Mailbox Existing Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyId);
        String body = "Mailbox update " + System.nanoTime();

        MimeMessage mailboxMessage = new MimeMessage(Session.getInstance(new Properties()));
        mailboxMessage.setFrom(new InternetAddress("user@mnemosyne-systems.ai"));
        mailboxMessage.setSubject("[" + ticket.name + "] mailbox reply");
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(body, StandardCharsets.UTF_8.name());
        MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.setFileName("mailbox.txt");
        attachmentPart.setContent("mailbox-attachment", "text/plain; charset=UTF-8");
        attachmentPart.setDisposition(MimeBodyPart.ATTACHMENT);
        MimeMultipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(attachmentPart);
        mailboxMessage.setContent(multipart);
        mailboxMessage.saveChanges();

        mailboxPollingService.processMailboxMessage(mailboxMessage);

        Message saved = findMessageByBody(body);
        Assertions.assertNotNull(saved);
        Assertions.assertEquals(ticket.id, saved.ticket.id);
        List<Attachment> attachments = Attachment.find("message = ?1", saved).list();
        Assertions.assertEquals(1, attachments.size());
        Assertions.assertEquals("mailbox.txt", attachments.get(0).name);

        List<Mail> userMessages = mailbox.getMailsSentTo("user@mnemosyne-systems.ai");
        Assertions.assertFalse(userMessages.isEmpty());
        Assertions.assertTrue(userMessages.get(userMessages.size() - 1).getSubject().contains("[" + ticket.name + "]"));
    }

    @Test
    void mailboxMessageWithoutTicketSubjectCreatesTicket() throws Exception {
        mailbox.clear();
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        Long companyId = ensureCompany("Mailbox New Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai");
        ensureTicket(companyId);
        String body = "Create ticket from mailbox " + System.nanoTime();

        MimeMessage mailboxMessage = new MimeMessage(Session.getInstance(new Properties()));
        mailboxMessage.setFrom(new InternetAddress("user@mnemosyne-systems.ai"));
        mailboxMessage.setSubject("Need help from mailbox");
        mailboxMessage.setText(body, StandardCharsets.UTF_8.name());
        mailboxMessage.saveChanges();

        mailboxPollingService.processMailboxMessage(mailboxMessage);

        Message saved = findMessageByBody(body);
        Assertions.assertNotNull(saved);
        Assertions.assertNotNull(saved.ticket);
        Assertions.assertNotNull(saved.ticket.name);
        Assertions.assertTrue(saved.ticket.name.contains("-"));
    }

    @Test
    void mailboxHtmlMessageStripsHtmlAndKeepsInlineAttachments() throws Exception {
        mailbox.clear();
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        Long companyId = ensureCompany("Mailbox Html Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai");
        ensureTicket(companyId);

        MimeMessage mailboxMessage = new MimeMessage(Session.getInstance(new Properties()));
        mailboxMessage.setFrom(new InternetAddress("user@mnemosyne-systems.ai"));
        mailboxMessage.setSubject("HTML only mailbox");
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent("<div>Hello<br/>Mailbox</div><p>Second&nbsp;paragraph</p>", "text/html; charset=UTF-8");
        MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.setFileName("inline.txt");
        attachmentPart.setDisposition(MimeBodyPart.INLINE);
        attachmentPart.setContent("inline-attachment", "text/plain; charset=UTF-8");
        MimeMultipart multipart = new MimeMultipart();
        multipart.addBodyPart(htmlPart);
        multipart.addBodyPart(attachmentPart);
        mailboxMessage.setContent(multipart);
        mailboxMessage.saveChanges();

        mailboxPollingService.processMailboxMessage(mailboxMessage);

        Message saved = findMessageByBody("Hello\nMailbox\nSecond paragraph");
        Assertions.assertNotNull(saved);
        List<Attachment> attachments = Attachment.find("message = ?1", saved).list();
        Assertions.assertEquals(1, attachments.size());
        Assertions.assertEquals("inline.txt", attachments.get(0).name);
        Assertions.assertEquals("text/plain", attachments.get(0).mimeType);
    }

    @Test
    void computeEffectiveStatusUsesSupportAssignmentsAndOpenFallback() {
        Ticket ticket = new Ticket();
        ticket.supportUsers.clear();

        Assertions.assertEquals("Open", ticketEmailService.computeEffectiveStatus(ticket, null));
        Assertions.assertEquals("Open", ticketEmailService.computeEffectiveStatus(ticket, "  "));
        Assertions.assertEquals("Open", ticketEmailService.computeEffectiveStatus(ticket, "Open"));

        User support = new User();
        support.email = "support1@mnemosyne-systems.ai";
        ticket.supportUsers.add(support);

        Assertions.assertEquals("Assigned", ticketEmailService.computeEffectiveStatus(ticket, "Open"));
        Assertions.assertEquals("Closed", ticketEmailService.computeEffectiveStatus(ticket, "Closed"));
    }

    @Test
    void reactSupportMutationsReturnJsonRedirects() {
        ensureUser("support-json", "support-json@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support-json");
        ensureDefaultCategories();
        String cookie = login("support-json", "support-json");
        Long companyId = ensureCompany("Support Json Co");
        Company company = Company.findById(companyId);
        Entitlement entitlement = ensureEntitlement("Support Json Entitlement", "Support json detail");
        Version version = ensureVersion(entitlement, "8.0.0", java.time.LocalDate.of(2026, 1, 1));
        CompanyEntitlement entry = ensureCompanyEntitlement(companyId, entitlement);

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).header("X-Billetsys-Client", "react")
                .multiPart("status", "Open").multiPart("title", "Support json redirect title")
                .multiPart("message", "Support json redirect create").multiPart("companyId", company.id)
                .multiPart("companyEntitlementId", entry.id).multiPart("categoryId", Category.findDefault().id)
                .multiPart("affectsVersionId", version.id).post("/support/tickets").then().statusCode(200)
                .body("redirectTo", Matchers.matchesPattern("/support/tickets/\\d+"));

        Ticket createdTicket = findMessageByBody("Support json redirect create").ticket;
        Assertions.assertNotNull(createdTicket);

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).header("X-Billetsys-Client", "react")
                .contentType(ContentType.URLENC).formParam("status", "Assigned").formParam("companyId", company.id)
                .formParam("companyEntitlementId", entry.id).formParam("categoryId", Category.findDefault().id)
                .formParam("affectsVersionId", version.id).post("/support/tickets/" + createdTicket.id).then()
                .statusCode(200).body("redirectTo", Matchers.equalTo("/support/tickets/" + createdTicket.id));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).header("X-Billetsys-Client", "react")
                .post("/support/tickets/" + createdTicket.id + "/assign").then().statusCode(200)
                .body("redirectTo", Matchers.equalTo("/support/tickets/" + createdTicket.id));
    }

}
