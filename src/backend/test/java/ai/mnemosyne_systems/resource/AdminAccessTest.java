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
class AdminAccessTest extends AccessTestSupport {

    @Test
    void adminCanAccessAdminUsers() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN, "admin");
        ensureCompanyIfMissing("Test Co");
        String cookie = login("admin", "admin");

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/users").then()
                .statusCode(303).header("Location", Matchers.endsWith("/users"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/admin/users").then().statusCode(200)
                .body("title", Matchers.equalTo("Users")).body("showCompanySelector", Matchers.equalTo(true))
                .body("selectedCompanyId", Matchers.notNullValue()).body("items", Matchers.not(Matchers.empty()));

        ai.mnemosyne_systems.model.User adminUser = ai.mnemosyne_systems.model.User
                .find("email", "admin@mnemosyne-systems.ai").firstResult();
        Long adminId = adminUser == null ? null : adminUser.id;
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/user/" + adminId)
                .then().statusCode(303).header("Location", Matchers.endsWith("/users/" + adminId));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/admin/users/" + adminId).then()
                .statusCode(200).body("username", Matchers.equalTo("admin"))
                .body("editPath", Matchers.equalTo("/users/" + adminId + "/edit"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/admin").then()
                .statusCode(303).header("Location", Matchers.endsWith("/"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/companies").then()
                .statusCode(303).header("Location", Matchers.endsWith("/companies"));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/owner").then()
                .statusCode(303).header("Location", Matchers.endsWith("/owner"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/owner").then().statusCode(200)
                .body("name", Matchers.equalTo("mnemosyne systems"))
                .body("supportOptions", Matchers.not(Matchers.empty()))
                .body("tamOptions", Matchers.not(Matchers.empty()));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/owner/edit").then()
                .statusCode(303).header("Location", Matchers.endsWith("/owner/edit"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/users/create").then()
                .statusCode(303).header("Location", Matchers.endsWith("/users/new"));
        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/admin/users/bootstrap").then()
                .statusCode(200).body("title", Matchers.equalTo("New user"))
                .body("passwordRequired", Matchers.equalTo(true)).body("user.type", Matchers.equalTo("user"))
                .body("user.name", Matchers.equalTo("")).body("user.email", Matchers.equalTo(""));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/entitlements").then()
                .statusCode(303).header("Location", Matchers.endsWith("/entitlements"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/levels").then()
                .statusCode(303).header("Location", Matchers.endsWith("/levels"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/companies/create")
                .then().statusCode(303).header("Location", Matchers.endsWith("/companies/new"));

        Long companyId = createCompany(cookie, "Cycle Co");
        deleteCompany(cookie, companyId);
    }

    @Test
    void adminCanManageSupportCatalogAndCompanies() {
        ensureUser("admin2", "admin2@mnemosyne-systems.ai", User.TYPE_ADMIN, "admin2");
        String cookie = login("admin2", "admin2");

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/entitlements").then()
                .statusCode(303).header("Location", Matchers.endsWith("/entitlements"));

        String entitlementName = "Test Entitlement";
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("name", entitlementName)
                .formParam("description", "Test description").formParam("versionNames", "1.0.0")
                .formParam("versionDates", "2026-01-01").post("/entitlements").then().statusCode(303);
        Entitlement entitlement = Entitlement.find("name", entitlementName).firstResult();
        Assertions.assertNotNull(entitlement);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("name", "Updated Entitlement")
                .formParam("description", "Updated description").formParam("versionNames", "1.0.1")
                .formParam("versionDates", "2026-02-01").post("/entitlements/" + entitlement.id).then().statusCode(303);
        Entitlement updatedEntitlement = refreshedEntitlement(entitlement.id);
        Assertions.assertEquals("Updated Entitlement", updatedEntitlement.name);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/entitlements/" + entitlement.id).then().statusCode(303)
                .header("Location", Matchers.endsWith("/entitlements/" + entitlement.id));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/entitlements/" + entitlement.id + "/edit").then().statusCode(303)
                .header("Location", Matchers.endsWith("/entitlements/" + entitlement.id + "/edit"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .post("/entitlements/" + entitlement.id + "/delete").then().statusCode(303);
        Assertions.assertNull(refreshedEntitlement(entitlement.id));

        String levelName = "Test Level";
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("name", levelName)
                .formParam("description", "Level description").formParam("level", 30).formParam("color", "Red")
                .post("/levels").then().statusCode(303);
        Level level = Level.find("name", levelName).firstResult();
        Assertions.assertNotNull(level);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("name", "Updated Level")
                .formParam("description", "Updated level").formParam("level", 45).formParam("color", "Yellow")
                .post("/levels/" + level.id).then().statusCode(303);
        Level updatedLevel = refreshedLevel(level.id);
        Assertions.assertEquals("Updated Level", updatedLevel.name);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie).get("/levels").then()
                .statusCode(303).header("Location", Matchers.endsWith("/levels"));
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .get("/levels/" + level.id + "/edit").then().statusCode(303)
                .header("Location", Matchers.endsWith("/levels/" + level.id + "/edit"));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .post("/levels/" + level.id + "/delete").then().statusCode(303);
        Assertions.assertNull(refreshedLevel(level.id));

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("name", "Coverage Co")
                .formParam("country", "United States of America").formParam("superuserUsername", "username")
                .formParam("superuserEmail", "user@@email").formParam("superuserPassword", "password")
                .post("/companies").then().statusCode(303);
        Company company = Company.find("name", "Coverage Co").firstResult();
        Assertions.assertNotNull(company);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("name", "Coverage Co Updated")
                .formParam("country", "United States of America").formParam("superuserId", company.superuser.id)
                .post("/companies/" + company.id).then().statusCode(303);
        Company updatedCompany = refreshedCompany(company.id);
        Assertions.assertEquals("Coverage Co Updated", updatedCompany.name);

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .post("/companies/" + company.id + "/delete").then().statusCode(303);
        Assertions.assertNull(refreshedCompany(company.id));
    }

    @Test
    void reactAppSessionReturnsAdminCategoryLink() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN, "admin");
        String cookie = login("admin", "admin");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/app/session").then().statusCode(200)
                .body("role", Matchers.equalTo("admin")).body("navigation.href", Matchers.hasItem("/categories"));
    }

    @Test
    void reactAppSessionReturnsAdminOwnerLink() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN, "admin");
        String cookie = login("admin", "admin");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/app/session").then().statusCode(200)
                .body("role", Matchers.equalTo("admin")).body("navigation.href", Matchers.hasItem("/owner"));
    }

    @Test
    void reactAppSessionReturnsAdminCompanyLink() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN, "admin");
        String cookie = login("admin", "admin");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/app/session").then().statusCode(200)
                .body("role", Matchers.equalTo("admin")).body("navigation.href", Matchers.hasItem("/companies"));
    }

    @Test
    void reactLoginRedirectsAdminToAdminDashboard() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN, "admin");

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC).formParam("username", "admin")
                .formParam("password", "admin").post("/login").then().statusCode(303)
                .header("Location", Matchers.equalTo("/"));
    }

    @Test
    void reactCategoriesApiRequiresAdminPrivileges() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        String cookie = login("support1", "support1");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/categories").then().statusCode(401);
    }

    @Test
    void reactCategoriesApiReturnsAdminCategoryListAndDetail() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN, "admin");
        String cookie = login("admin", "admin");
        Category category = ensureCategory("Escalation", "First line\n\nExtra detail", true);
        Attachment attachment = ensureCategoryAttachment(category, "category-note.txt", "Category attachment");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/categories").then().statusCode(200)
                .body("canCreate", Matchers.equalTo(true)).body("createPath", Matchers.equalTo("/categories/new"))
                .body("items.name", Matchers.hasItem("Escalation"))
                .body("items.find { it.name == 'Escalation' }.descriptionPreview", Matchers.equalTo("First line"))
                .body("items.find { it.name == 'Escalation' }.isDefault", Matchers.equalTo(true));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/categories/" + category.id).then()
                .statusCode(200).body("name", Matchers.equalTo("Escalation"))
                .body("description", Matchers.containsString("Extra detail")).body("isDefault", Matchers.equalTo(true))
                .body("editPath", Matchers.equalTo("/categories/" + category.id + "/edit"))
                .body("attachments.name", Matchers.hasItem("category-note.txt"))
                .body("attachments.downloadPath", Matchers.hasItem("/attachments/" + attachment.id + "/data"));
    }

    @Test
    void reactArticlesApiReturnsAdminArticleListAndDetail() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN, "admin");
        String cookie = login("admin", "admin");
        Article article = ensureArticle("Admin Runbook", "ops,admin", "## Admin article\n\nSteps");
        Attachment attachment = ensureArticleAttachment(article, "admin-runbook.txt", "Admin attachment");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/articles").then().statusCode(200)
                .body("canCreate", Matchers.equalTo(true)).body("createPath", Matchers.equalTo("/articles/new"))
                .body("items.title", Matchers.hasItem("Admin Runbook"));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/articles/" + article.id).then()
                .statusCode(200).body("title", Matchers.equalTo("Admin Runbook"))
                .body("tags", Matchers.equalTo("ops,admin")).body("body", Matchers.containsString("Admin article"))
                .body("canEdit", Matchers.equalTo(true)).body("canDelete", Matchers.equalTo(true))
                .body("editPath", Matchers.equalTo("/articles/" + article.id + "/edit"))
                .body("attachments.name", Matchers.hasItem("admin-runbook.txt"))
                .body("attachments.downloadPath", Matchers.hasItem("/attachments/" + attachment.id + "/data"));
    }

    @Test
    void reactCompaniesApiRequiresAdminPrivileges() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        String cookie = login("support1", "support1");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/companies").then().statusCode(401);
    }

    @Test
    void reactCompaniesApiReturnsAdminCompanyListAndDetail() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN, "admin");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER, "user");
        String cookie = login("admin", "admin");
        Long companyId = ensureCompany("React Company");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Company company = Company.findById(companyId);
        Entitlement entitlement = ensureEntitlement("React Entitlement", "React detail");
        Level level = ensureLevel("React Level", "React level detail", 60, "Blue");
        ensureCompanyEntitlement(company, entitlement, level);

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/companies").then().statusCode(200)
                .body("createPath", Matchers.equalTo("/companies/new"))
                .body("items.name", Matchers.hasItem("React Company"))
                .body("items.find { it.name == 'React Company' }.detailPath",
                        Matchers.equalTo("/companies/" + companyId));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/companies/" + companyId).then()
                .statusCode(200).body("name", Matchers.equalTo("React Company"))
                .body("selectedUsers.email", Matchers.hasItem("user@mnemosyne-systems.ai"))
                .body("selectedTams.email", Matchers.hasItem("tam1@mnemosyne-systems.ai"))
                .body("entitlementAssignments.entitlementName", Matchers.hasItem("React Entitlement"))
                .body("countries.size()", Matchers.greaterThan(0)).body("timezones.size()", Matchers.greaterThan(0));
    }

    @Test
    void adminCanUpdateCompanyWithEntitlementAssignments() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN, "admin");
        String cookie = login("admin", "admin");
        Long companyId = createCompany(cookie, "Entitlement Update Co");
        Company company = refreshedCompany(companyId);
        Assertions.assertNotNull(company);

        Entitlement entitlement = ensureEntitlement("Company Update Entitlement", "Update detail");
        Level level = ensureLevel("Company Update Level", "Update level", 50, "Orange");

        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("name", company.name)
                .formParam("superuserId", company.superuser.id).formParam("entitlementIds", entitlement.id)
                .formParam("levelIds", level.id).formParam("entitlementDates", "2026-03-27")
                .formParam("entitlementDurations", CompanyEntitlement.DURATION_YEARLY).post("/companies/" + company.id)
                .then().statusCode(303);

        CompanyEntitlement savedEntitlement = CompanyEntitlement
                .find("company = ?1 and entitlement = ?2 and supportLevel = ?3", company, entitlement, level)
                .firstResult();
        Assertions.assertNotNull(savedEntitlement);
        Assertions.assertEquals("2026-03-27", savedEntitlement.date.toString());
        Assertions.assertEquals(CompanyEntitlement.DURATION_YEARLY, savedEntitlement.duration);

        Company updatedCompany = refreshedCompany(company.id);
        Assertions.assertNotNull(updatedCompany.superuser);
        Assertions.assertTrue(companyHasUser(updatedCompany.id, updatedCompany.superuser.email));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/companies/" + company.id).then()
                .statusCode(200).body("selectedSuperusers.email", Matchers.hasItem(updatedCompany.superuser.email));
    }

    @Test
    void reactOwnerApiRequiresAdminPrivileges() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        String cookie = login("support1", "support1");

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/owner").then().statusCode(401);
    }

    @Test
    void reactOwnerApiReturnsAndUpdatesOwnerDetails() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN, "admin");
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT, "support1");
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM, "tam1");
        String cookie = login("admin", "admin");
        Company ownerCompany = ownerCompany();
        Country country = Country.find("code", "US").firstResult();
        Timezone timezone = Timezone.find("country = ?1 order by name", country).firstResult();
        User supportUser = User.find("email", "support1@mnemosyne-systems.ai").firstResult();
        User tamUser = User.find("email", "tam1@mnemosyne-systems.ai").firstResult();

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).get("/api/owner").then().statusCode(200)
                .body("id", Matchers.equalTo(ownerCompany.id.intValue()))
                .body("name", Matchers.equalTo(ownerCompany.name)).body("countries.size()", Matchers.greaterThan(0))
                .body("timezones.size()", Matchers.greaterThan(0))
                .body("supportOptions.email", Matchers.hasItem("support1@mnemosyne-systems.ai"))
                .body("tamOptions.email", Matchers.hasItem("tam1@mnemosyne-systems.ai"));

        RestAssured.given().cookie(AuthHelper.AUTH_COOKIE, cookie).contentType(ContentType.JSON)
                .body(Map.ofEntries(Map.entry("name", "mnemosyne systems"), Map.entry("address1", "Owner Street 1"),
                        Map.entry("address2", "Owner Street 2"), Map.entry("city", "Owner City"),
                        Map.entry("state", "Owner State"), Map.entry("zip", "12345"),
                        Map.entry("phoneNumber", "+45 12345678"), Map.entry("countryId", country.id),
                        Map.entry("timezoneId", timezone.id), Map.entry("supportIds", List.of(supportUser.id)),
                        Map.entry("tamIds", List.of(tamUser.id))))
                .post("/api/owner").then().statusCode(200).body("city", Matchers.equalTo("Owner City"))
                .body("supportUsers.email", Matchers.hasItem("support1@mnemosyne-systems.ai"))
                .body("tamUsers.email", Matchers.hasItem("tam1@mnemosyne-systems.ai"));

        Company updated = refreshedCompany(ownerCompany.id);
        Assertions.assertEquals("Owner City", updated.city);
        Assertions.assertEquals("+45 12345678", updated.phoneNumber);
        Assertions.assertEquals(country.id, updated.country.id);
        Assertions.assertEquals(timezone.id, updated.timezone.id);
        Assertions.assertTrue(companyHasUser(ownerCompany.id, "support1@mnemosyne-systems.ai"));
        Assertions.assertTrue(companyHasUser(ownerCompany.id, "tam1@mnemosyne-systems.ai"));
    }

}
