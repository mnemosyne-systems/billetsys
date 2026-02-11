/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.Attachment;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Entitlement;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.SupportLevel;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UserAccessTest {

    @Test
    void adminCanAccessAdminUsers() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN, "admin");
        ensureCompanyIfMissing("Test Co");
        String cookie = login("admin", "admin");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/admin/users").then().statusCode(200)
                .body(Matchers.containsString("Users"));

        ai.mnemosyne_systems.model.User adminUser = ai.mnemosyne_systems.model.User
                .find("email", "admin@mnemosyne-systems.ai").firstResult();
        Long adminId = adminUser == null ? null : adminUser.id;
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/admin/users/" + adminId).then().statusCode(200)
                .body(Matchers.containsString("Edit"));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/admin/companies").then().statusCode(200)
                .body(Matchers.containsString("Companies")).body(Matchers.containsString("Name"))
                .body(Matchers.containsString("Country"));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/admin/users/create").then().statusCode(200)
                .body(Matchers.containsString("New user")).body(Matchers.containsString("value=\"user\""))
                .body(Matchers.containsString("name=\"name\" value=\"\""))
                .body(Matchers.containsString("name=\"email\" value=\"\""));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/admin/entitlements").then().statusCode(200)
                .body(Matchers.containsString("Entitlements"));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/admin/support-levels").then().statusCode(200)
                .body(Matchers.containsString("Support Levels"));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/admin/companies/create").then().statusCode(200)
                .body(Matchers.containsString("Entitlement")).body(Matchers.containsString("Service level"));

        Long companyId = createCompany(cookie, "Cycle Co");
        deleteCompany(cookie, companyId);
    }

    @Test
    void supportCanAccessSupportUsersMenu() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("support2", "support2@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support2");
        Long companyId = ensureCompany("Support Co");
        ai.mnemosyne_systems.model.Ticket supportTicket = ensureTicket(companyId);
        ensureMessage(supportTicket, "Sample ticket created.");
        String supportTicketName = supportTicket == null ? "" : supportTicket.name;
        String cookie = login("support1", "support1");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/support/users/" + companyId).then()
                .statusCode(200).body(Matchers.containsString("Users"));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/support").then().statusCode(200)
                .body(Matchers.containsString("Tickets")).body(Matchers.containsString("Open tickets"))
                .body(Matchers.containsString("Closed tickets")).body(Matchers.containsString(supportTicketName))
                .body(Matchers.containsString("Assigned")).body(Matchers.containsString("Create"))
                .body(Matchers.containsString("A-0000")).body(Matchers.containsString("support1@mnemosyne-systems.ai"));
        ai.mnemosyne_systems.model.User supportUser = ai.mnemosyne_systems.model.User
                .find("email", "support1@mnemosyne-systems.ai").firstResult();
        int assignedCount = ai.mnemosyne_systems.model.Ticket.find(
                "select distinct t from Ticket t join t.supportUsers u where u = ?1 and (t.status is null or lower(t.status) <> 'closed')",
                supportUser).list().size();
        int openCount = ai.mnemosyne_systems.model.Ticket
                .find("select distinct t from Ticket t where t.supportUsers is empty").list().size();
        String expectedTicketLabel = "Tickets (" + assignedCount + "/" + openCount + ")";
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/support").then().statusCode(200)
                .body(Matchers.containsString("Tickets"))
                .body(Matchers.containsString("(" + assignedCount + "/" + openCount + ")"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/support/open").then().statusCode(200)
                .body(Matchers.containsString("Open tickets")).body(Matchers.containsString("A-00002"))
                .body(Matchers.containsString("A-00003"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/support/closed").then().statusCode(200)
                .body(Matchers.containsString("Closed tickets")).body(Matchers.containsString("A-00004"));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/support/tickets/create").then().statusCode(200)
                .body(Matchers.containsString("Create")).body(Matchers.containsString("Message"))
                .body(Matchers.not(Matchers.containsString("Status"))).body(Matchers.containsString("Entitlement"))
                .body(Matchers.containsString("form-card full-width"));

        Long ticketId = supportTicket == null ? null : supportTicket.id;
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/tickets/" + ticketId + "/edit").then()
                .statusCode(200).body(Matchers.containsString("Company")).body(Matchers.containsString("Entitlement"))
                .body(Matchers.containsString("Service level")).body(Matchers.containsString("Starter"))
                .body(Matchers.containsString("Normal")).body(Matchers.containsString("Messages"))
                .body(Matchers.containsString("Sample ticket created."));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/support/tickets/" + ticketId).then()
                .statusCode(200).body(Matchers.containsString("Sample ticket created."))
                .body(Matchers.containsString("support1@mnemosyne-systems.ai")).body(Matchers.containsString("Ticket"))
                .body(Matchers.containsString("Support users")).body(Matchers.containsString("Company"))
                .body(Matchers.containsString("Entitlement")).body(Matchers.containsString("Service Level"))
                .body(Matchers.containsString("TAMs")).body(Matchers.containsString("tam@mnemosyne-systems.ai"))
                .body(Matchers.not(Matchers.containsString("Cancel")))
                .body(Matchers.not(Matchers.containsString("Back")));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("status", "Assigned")
                .formParam("companyId", supportTicket.company.id)
                .formParam("companyEntitlementId", supportTicket.companyEntitlement.id)
                .post("/support/tickets/" + ticketId).then().statusCode(303);
        Ticket updatedSupportTicket = refreshedTicket(ticketId);
        Assertions.assertEquals("Assigned", updatedSupportTicket.status);
        Assertions.assertTrue(ticketHasSupportUser(ticketId, supportUser.id));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/profile").then().statusCode(200)
                .body(Matchers.containsString("Profile")).body(Matchers.containsString("Upload logo"))
                .body(Matchers.not(Matchers.containsString("Change password")))
                .body(Matchers.not(Matchers.containsString("Cancel")))
                .body(Matchers.matchesPattern("(?s).*Tickets\\s*\\(\\d+/\\d+\\).*"));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/profile/password").then().statusCode(200)
                .body(Matchers.containsString("Change Password"))
                .body(Matchers.matchesPattern("(?s).*Tickets\\s*\\(\\d+/\\d+\\).*"))
                .body(Matchers.not(Matchers.containsString("Cancel")));
    }

    @Test
    void userCanAccessUserTicketsMenu() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        Long companyId = ensureCompany("Test Co");
        ai.mnemosyne_systems.model.Ticket userTicket = ensureTicket(companyId);
        String userTicketName = userTicket == null ? "" : userTicket.name;
        String cookie = login("user", "user");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/user/tickets").then().statusCode(200)
                .body(Matchers.containsString("Tickets")).body(Matchers.containsString("Open tickets"))
                .body(Matchers.containsString("Closed tickets")).body(Matchers.containsString("Create"))
                .body(Matchers.containsString("/user/tickets/"));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/user/tickets/open").then().statusCode(200)
                .body(Matchers.containsString("Open tickets")).body(Matchers.containsString("Create"));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/user/tickets/closed").then().statusCode(200)
                .body(Matchers.containsString("Closed tickets")).body(Matchers.containsString("Create"));

        Long ticketId = userTicket == null ? null : userTicket.id;
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/user/tickets/" + ticketId).then()
                .statusCode(200).body(Matchers.containsString(userTicketName)).body(Matchers.containsString("Ticket"))
                .body(Matchers.containsString("Support users")).body(Matchers.containsString("Company"))
                .body(Matchers.containsString("Entitlement")).body(Matchers.containsString("Service Level"))
                .body(Matchers.containsString("TAMs")).body(Matchers.containsString("Reply"))
                .body(Matchers.not(Matchers.containsString("Back")));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/user/tickets/" + ticketId + "/edit").then()
                .statusCode(200).body(Matchers.containsString("In Progress")).body(Matchers.containsString("Resolved"))
                .body(Matchers.containsString("Closed"));
    }

    @Test
    void tamCanAccessUserTicketsMenu() {
        ensureUser("tam", "tam@mnemosyne-systems.ai", User.TYPE_TAM, "tam");
        Long companyId = ensureCompany("TAM Co");
        ensureCompanyUsers(companyId, "tam@mnemosyne-systems.ai");
        ai.mnemosyne_systems.model.Ticket tamTicket = ensureTicket(companyId);
        String tamTicketName = tamTicket == null ? null : tamTicket.name;
        String cookie = login("tam", "tam");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/user/tickets").then().statusCode(200)
                .body(Matchers.containsString("Tickets")).body(Matchers.containsString("Open tickets"))
                .body(Matchers.containsString("Closed tickets")).body(Matchers.containsString("Create"))
                .body(Matchers.containsString("Open"));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/user/tickets/open").then().statusCode(200)
                .body(Matchers.containsString("Open tickets")).body(Matchers.containsString("Create"));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/user/tickets/closed").then().statusCode(200)
                .body(Matchers.containsString("Closed tickets")).body(Matchers.containsString("Create"));

        Long ticketId = tamTicket == null ? null : tamTicket.id;
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/user/tickets/" + ticketId).then()
                .statusCode(200).body(Matchers.containsString(tamTicketName)).body(Matchers.containsString("Ticket"))
                .body(Matchers.containsString("Support users")).body(Matchers.containsString("Company"))
                .body(Matchers.containsString("Entitlement")).body(Matchers.containsString("Service Level"))
                .body(Matchers.containsString("tam@mnemosyne-systems.ai")).body(Matchers.containsString("Reply"))
                .body(Matchers.not(Matchers.containsString("Back")));
    }

    @Test
    void adminCanManageSupportCatalogAndCompanies() {
        ensureUser("admin2", "admin2@mnemosyne-systems.ai", User.TYPE_ADMIN, "admin2");
        String cookie = login("admin2", "admin2");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/admin/entitlements").then().statusCode(200)
                .body(Matchers.containsString("Entitlements"));

        String entitlementName = "Test Entitlement";
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("name", entitlementName)
                .formParam("description", "Test description").formParam("price", 123).post("/admin/entitlements").then()
                .statusCode(303);
        Entitlement entitlement = Entitlement.find("name", entitlementName).firstResult();
        Assertions.assertNotNull(entitlement);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("name", "Updated Entitlement")
                .formParam("description", "Updated description").formParam("price", 456)
                .post("/admin/entitlements/" + entitlement.id).then().statusCode(303);
        Entitlement updatedEntitlement = refreshedEntitlement(entitlement.id);
        Assertions.assertEquals("Updated Entitlement", updatedEntitlement.name);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .post("/admin/entitlements/" + entitlement.id + "/delete").then().statusCode(303);
        Assertions.assertNull(refreshedEntitlement(entitlement.id));

        String levelName = "Test Level";
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("name", levelName)
                .formParam("description", "Level description").formParam("critical", 30)
                .formParam("criticalColor", "Red").formParam("escalate", 60).formParam("escalateColor", "Yellow")
                .formParam("normal", 120).formParam("normalColor", "White").post("/admin/support-levels").then()
                .statusCode(303);
        SupportLevel level = SupportLevel.find("name", levelName).firstResult();
        Assertions.assertNotNull(level);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("name", "Updated Level")
                .formParam("description", "Updated level").formParam("critical", 45).formParam("criticalColor", "Red")
                .formParam("escalate", 90).formParam("escalateColor", "Yellow").formParam("normal", 180)
                .formParam("normalColor", "White").post("/admin/support-levels/" + level.id).then().statusCode(303);
        SupportLevel updatedLevel = refreshedSupportLevel(level.id);
        Assertions.assertEquals("Updated Level", updatedLevel.name);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .post("/admin/support-levels/" + level.id + "/delete").then().statusCode(303);
        Assertions.assertNull(refreshedSupportLevel(level.id));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("name", "Coverage Co")
                .formParam("country", "United States of America").post("/companies").then().statusCode(303);
        Company company = Company.find("name", "Coverage Co").firstResult();
        Assertions.assertNotNull(company);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("name", "Coverage Co Updated")
                .formParam("country", "United States of America").post("/companies/" + company.id).then()
                .statusCode(303);
        Company updatedCompany = refreshedCompany(company.id);
        Assertions.assertEquals("Coverage Co Updated", updatedCompany.name);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .post("/companies/" + company.id + "/delete").then().statusCode(303);
        Assertions.assertNull(refreshedCompany(company.id));
    }

    @Test
    void supportCanManageTicketsAndMessages() {
        ensureUser("support3", "support3@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support3");
        String cookie = login("support3", "support3");
        Long companyId = ensureCompany("Support CRUD Co");
        Company company = Company.findById(companyId);
        Entitlement entitlement = ensureEntitlement("Starter", "Email support", 99);
        SupportLevel level = ensureSupportLevel("Normal", "Default response window", 60, "Red", 120, "Yellow", 720,
                "White");
        CompanyEntitlement entry = ensureCompanyEntitlement(company, entitlement, level);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("status", "Open").formParam("companyId", company.id)
                .formParam("companyEntitlementId", entry.id).post("/tickets").then().statusCode(303);
        Ticket ticket = Ticket.find("company = ?1 order by id desc", company).firstResult();
        Assertions.assertNotNull(ticket);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("status", "In Progress").formParam("companyId", company.id)
                .formParam("companyEntitlementId", entry.id).post("/tickets/" + ticket.id).then().statusCode(303);
        Ticket updatedTicket = refreshedTicket(ticket.id);
        Assertions.assertEquals("In Progress", updatedTicket.status);

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

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/support/tickets/" + ticket.id).then()
                .statusCode(200).body(Matchers.containsString("note.txt")).body(Matchers.containsString("note-2.txt"))
                .body(Matchers.containsString("text/plain")).body(Matchers.containsString("bytes"));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/attachments/" + attachment.id).then()
                .statusCode(200).body(Matchers.containsString("note.txt"))
                .body(Matchers.containsString("Attachment line two"));

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
    void attachmentsPageUsesRoleHeader() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam", "tam@mnemosyne-systems.ai", User.TYPE_TAM, "tam");
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        Long companyId = ensureCompany("Attachment Role Co");
        ensureCompanyUsers(companyId, "tam@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyId);
        Message message = ensureMessageWithBody(ticket, "Attachment role message");
        Attachment attachment = ensureAttachment(message, "role-attachment.txt");

        String supportCookie = login("support1", "support1");
        User supportUser = User.find("email", "support1@mnemosyne-systems.ai").firstResult();
        SupportResource.SupportTicketCounts counts = SupportResource.loadTicketCounts(supportUser);
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, supportCookie).get("/attachments/" + attachment.id).then()
                .statusCode(200).body(Matchers.matchesPattern(
                        "(?s).*Tickets\\s*\\(" + counts.assignedCount + "/" + counts.openCount + "\\).*"));

        String tamCookie = login("tam", "tam");
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, tamCookie).get("/attachments/" + attachment.id).then()
                .statusCode(200).body(Matchers.matchesPattern("(?s).*Tickets\\s*\\(\\d+/\\d+\\).*"));

        String userCookie = login("user", "user");
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, userCookie).get("/attachments/" + attachment.id).then()
                .statusCode(200).body(Matchers.matchesPattern("(?s).*Tickets\\s*\\(\\d+/\\d+\\).*"));
    }

    @Test
    void profileUpdatesAndTamCreatesUsers() {
        ensureUser("profile", "profile@mnemosyne-systems.ai", User.TYPE_SUPPORT, "profile");
        String profileCookie = login("profile", "profile");
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, profileCookie)
                .contentType(ContentType.URLENC).formParam("name", "Profile Updated").post("/profile").then()
                .statusCode(303);
        User updatedProfile = User.find("email", "profile@mnemosyne-systems.ai").firstResult();
        Assertions.assertEquals("Profile Updated", updatedProfile.name);

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

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, tamCookie).get("/tam/users/" + tamCompanyId).then()
                .statusCode(200).body(Matchers.containsString("Users"))
                .body(Matchers.containsString("tam2@mnemosyne-systems.ai"));

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

    @Transactional
    void ensureUser(String name, String email, String type, String password) {
        User user = User.find("email", email).firstResult();
        if (user == null) {
            user = new User();
            user.name = name;
            user.email = email;
            user.type = type;
            user.passwordHash = BcryptUtil.bcryptHash(password);
            user.persist();
            return;
        }
        user.name = name;
        user.type = type;
        user.passwordHash = BcryptUtil.bcryptHash(password);
    }

    @Transactional
    void ensureCompanyIfMissing(String name) {
        if (ai.mnemosyne_systems.model.Company.find("name", name).firstResult() != null) {
            return;
        }
        ai.mnemosyne_systems.model.Company company = new ai.mnemosyne_systems.model.Company();
        company.name = name;
        company.persist();
    }

    @Transactional
    Long ensureCompany(String name) {
        ai.mnemosyne_systems.model.Company company = ai.mnemosyne_systems.model.Company.find("name", name)
                .firstResult();
        if (company != null) {
            return company.id;
        }
        company = new ai.mnemosyne_systems.model.Company();
        company.name = name;
        company.country = ai.mnemosyne_systems.model.Country.find("code", "US").firstResult();
        company.persist();
        return company.id;
    }

    @Transactional
    ai.mnemosyne_systems.model.Ticket ensureTicket(Long companyId) {
        ai.mnemosyne_systems.model.Company company = ai.mnemosyne_systems.model.Company.findById(companyId);
        ai.mnemosyne_systems.model.User user = ai.mnemosyne_systems.model.User
                .find("email", "user@mnemosyne-systems.ai").firstResult();
        ai.mnemosyne_systems.model.Entitlement entitlement = ai.mnemosyne_systems.model.Entitlement
                .find("name", "Starter").firstResult();
        ai.mnemosyne_systems.model.SupportLevel level = ai.mnemosyne_systems.model.SupportLevel.find("name", "Normal")
                .firstResult();
        ai.mnemosyne_systems.model.CompanyEntitlement entry = new ai.mnemosyne_systems.model.CompanyEntitlement();
        entry.company = company;
        entry.entitlement = entitlement;
        entry.supportLevel = level;
        entry.persist();
        ai.mnemosyne_systems.model.Ticket ticket = new ai.mnemosyne_systems.model.Ticket();
        ticket.name = ai.mnemosyne_systems.model.Ticket.nextName(company);
        ticket.status = "Open";
        ticket.company = company;
        ticket.requester = user;
        ticket.companyEntitlement = entry;
        ticket.supportUsers
                .add(ai.mnemosyne_systems.model.User.find("email", "support1@mnemosyne-systems.ai").firstResult());
        ticket.tamUsers.add(ai.mnemosyne_systems.model.User.find("email", "tam@mnemosyne-systems.ai").firstResult());
        ticket.persist();
        return ticket;
    }

    @Transactional
    void ensureMessage(ai.mnemosyne_systems.model.Ticket ticket, String body) {
        if (ai.mnemosyne_systems.model.Message.find("ticket = ?1", ticket).firstResult() != null) {
            return;
        }
        ai.mnemosyne_systems.model.Message message = new ai.mnemosyne_systems.model.Message();
        message.ticket = ticket;
        message.body = body;
        message.date = java.time.LocalDateTime.now();
        message.author = ai.mnemosyne_systems.model.User.find("email", "support1@mnemosyne-systems.ai").firstResult();
        message.persist();
    }

    @Transactional
    Message ensureMessageWithBody(Ticket ticket, String body) {
        Message message = Message.find("ticket = ?1 and body = ?2", ticket, body).firstResult();
        if (message != null) {
            return message;
        }
        message = new Message();
        message.ticket = ticket;
        message.body = body;
        message.date = java.time.LocalDateTime.now();
        message.author = User.find("email", "support1@mnemosyne-systems.ai").firstResult();
        message.persist();
        return message;
    }

    @Transactional
    Attachment ensureAttachment(Message message, String name) {
        Attachment attachment = Attachment.find("message = ?1 and name = ?2", message, name).firstResult();
        if (attachment != null) {
            return attachment;
        }
        attachment = new Attachment();
        attachment.message = message;
        attachment.name = name;
        attachment.mimeType = "text/plain";
        attachment.data = "Attachment data".getBytes(StandardCharsets.UTF_8);
        attachment.persist();
        return attachment;
    }

    @Transactional
    void ensureCompanyUsers(Long companyId, String... emails) {
        ai.mnemosyne_systems.model.Company company = ai.mnemosyne_systems.model.Company.findById(companyId);
        if (company == null) {
            return;
        }
        for (String email : emails) {
            ai.mnemosyne_systems.model.User user = ai.mnemosyne_systems.model.User.find("email", email).firstResult();
            if (user == null) {
                continue;
            }
            boolean exists = company.users.stream()
                    .anyMatch(existing -> existing.id != null && existing.id.equals(user.id));
            if (!exists) {
                company.users.add(user);
            }
        }
    }

    String login(String username, String password) {
        return RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("username", username).formParam("password", password).post("/login").then().statusCode(303)
                .extract().cookie(AuthHelper.AUTH_COOKIE);
    }

    @Transactional
    Entitlement ensureEntitlement(String name, String description, int price) {
        Entitlement entitlement = Entitlement.find("name", name).firstResult();
        if (entitlement == null) {
            entitlement = new Entitlement();
            entitlement.name = name;
            entitlement.description = description;
            entitlement.price = price;
            entitlement.persist();
        }
        return entitlement;
    }

    @Transactional
    SupportLevel ensureSupportLevel(String name, String description, int critical, String criticalColor, int escalate,
            String escalateColor, int normal, String normalColor) {
        SupportLevel level = SupportLevel.find("name", name).firstResult();
        if (level == null) {
            level = new SupportLevel();
            level.name = name;
            level.description = description;
            level.critical = critical;
            level.criticalColor = criticalColor;
            level.escalate = escalate;
            level.escalateColor = escalateColor;
            level.normal = normal;
            level.normalColor = normalColor;
            level.persist();
        }
        return level;
    }

    @Transactional
    CompanyEntitlement ensureCompanyEntitlement(Company company, Entitlement entitlement, SupportLevel level) {
        CompanyEntitlement entry = CompanyEntitlement.find("company = ?1 and entitlement = ?2", company, entitlement)
                .firstResult();
        if (entry == null) {
            entry = new CompanyEntitlement();
            entry.company = company;
            entry.entitlement = entitlement;
            entry.supportLevel = level;
            entry.persist();
        }
        return entry;
    }

    @Transactional
    Entitlement refreshedEntitlement(Long id) {
        Panache.getEntityManager().clear();
        return Entitlement.findById(id);
    }

    @Transactional
    Ticket refreshedTicket(Long id) {
        Panache.getEntityManager().clear();
        return Ticket.findById(id);
    }

    @Transactional
    boolean ticketHasSupportUser(Long ticketId, Long userId) {
        Long count = Ticket.count("select distinct t from Ticket t join t.supportUsers u where t.id = ?1 and u.id = ?2",
                ticketId, userId);
        return count != null && count > 0;
    }

    @Transactional
    SupportLevel refreshedSupportLevel(Long id) {
        Panache.getEntityManager().clear();
        return SupportLevel.findById(id);
    }

    @Transactional
    Company refreshedCompany(Long id) {
        Panache.getEntityManager().clear();
        return Company.findById(id);
    }

    @Transactional
    User refreshedUser(Long id) {
        Panache.getEntityManager().clear();
        return User.findById(id);
    }

    @Transactional
    Message refreshedMessage(Long id) {
        Panache.getEntityManager().clear();
        return Message.findById(id);
    }

    @Transactional
    Long createCompany(String cookie, String name) {
        String location = RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("name", name).post("/admin/companies").then().statusCode(303)
                .extract().header("Location");
        ai.mnemosyne_systems.model.Company company = ai.mnemosyne_systems.model.Company.find("name", name)
                .firstResult();
        return company == null ? null : company.id;
    }

    @Transactional
    void deleteCompany(String cookie, Long companyId) {
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .post("/admin/companies/" + companyId + "/delete").then().statusCode(303);
    }
}
