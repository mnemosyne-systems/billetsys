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
import ai.mnemosyne_systems.model.Installation;
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
class AdminAccessTest extends AccessTestSupport {

    @Test
    @TestSecurity(user = "admin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "admin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "admin") })
    void adminCanAccessAdminUsers() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN);
        ensureUser("floating-user", "floating-user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureCompanyIfMissing("Test Co");

        RestAssured.given().redirects().follow(false).get("/users").then().statusCode(303).header("Location",
                Matchers.endsWith("/users"));
        RestAssured.given().get("/api/admin/users").then().statusCode(200).body("title", Matchers.equalTo("Users"))
                .body("showCompanySelector", Matchers.equalTo(true)).body("selectedCompanyId", Matchers.notNullValue())
                .body("items", Matchers.not(Matchers.empty()));
        RestAssured.given().queryParam("companyId", 0).get("/api/admin/users").then().statusCode(200)
                .body("selectedCompanyId", Matchers.equalTo(0)).body("companies.name", Matchers.hasItem("Unassigned"))
                .body("items.email", Matchers.hasItem("floating-user@mnemosyne-systems.ai"));

        ai.mnemosyne_systems.model.User adminUser = ai.mnemosyne_systems.model.User
                .find("email", "admin@mnemosyne-systems.ai").firstResult();
        Long adminId = adminUser == null ? null : adminUser.id;
        RestAssured.given().redirects().follow(false).get("/user/" + adminId).then().statusCode(303).header("Location",
                Matchers.endsWith("/users/" + adminId));
        RestAssured.given().get("/api/admin/users/" + adminId).then().statusCode(200)
                .body("username", Matchers.equalTo("admin"))
                .body("editPath", Matchers.equalTo("/users/" + adminId + "/edit"));

        RestAssured.given().redirects().follow(false).get("/admin").then().statusCode(303).header("Location",
                Matchers.endsWith("/"));

        RestAssured.given().redirects().follow(false).get("/companies").then().statusCode(303).header("Location",
                Matchers.endsWith("/companies"));
        RestAssured.given().redirects().follow(false).get("/owner").then().statusCode(303).header("Location",
                Matchers.endsWith("/owner"));
        RestAssured.given().get("/api/owner").then().statusCode(200).body("name", Matchers.equalTo("mnemosyne systems"))
                .body("use24HourClock", Matchers.equalTo(false)).body("supportOptions", Matchers.not(Matchers.empty()))
                .body("tamOptions", Matchers.not(Matchers.empty()));
        RestAssured.given().redirects().follow(false).get("/owner/edit").then().statusCode(303).header("Location",
                Matchers.endsWith("/owner/edit"));

        RestAssured.given().redirects().follow(false).get("/users/create").then().statusCode(303).header("Location",
                Matchers.endsWith("/users/new"));
        RestAssured.given().get("/api/admin/users/bootstrap").then().statusCode(200)
                .body("title", Matchers.equalTo("New user")).body("passwordRequired", Matchers.equalTo(true))
                .body("user.type", Matchers.equalTo("user")).body("user.name", Matchers.equalTo(""))
                .body("user.email", Matchers.equalTo(""));

        RestAssured.given().redirects().follow(false).get("/entitlements").then().statusCode(303).header("Location",
                Matchers.endsWith("/entitlements"));

        RestAssured.given().redirects().follow(false).get("/levels").then().statusCode(303).header("Location",
                Matchers.endsWith("/levels"));

        RestAssured.given().redirects().follow(false).get("/companies/create").then().statusCode(303).header("Location",
                Matchers.endsWith("/companies/new"));

        Long companyId = createCompany("Cycle Co");
        deleteCompany(companyId);
    }

    @Test
    @TestSecurity(user = "admin2@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "admin2@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "admin") })
    void adminCanManageSupportCatalogAndCompanies() {
        ensureUser("admin2", "admin2@mnemosyne-systems.ai", User.TYPE_ADMIN);

        RestAssured.given().redirects().follow(false).get("/entitlements").then().statusCode(303).header("Location",
                Matchers.endsWith("/entitlements"));

        String entitlementName = "Test Entitlement";
        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC).formParam("name", entitlementName)
                .formParam("description", "Test description").formParam("versionNames", "1.0.0")
                .formParam("versionDates", "2026-01-01").post("/entitlements").then().statusCode(303);
        Entitlement entitlement = Entitlement.find("name", entitlementName).firstResult();
        Assertions.assertNotNull(entitlement);

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("name", "Updated Entitlement").formParam("description", "Updated description")
                .formParam("versionNames", "1.0.1").formParam("versionDates", "2026-02-01")
                .post("/entitlements/" + entitlement.id).then().statusCode(303);
        Entitlement updatedEntitlement = refreshedEntitlement(entitlement.id);
        Assertions.assertEquals("Updated Entitlement", updatedEntitlement.name);

        RestAssured.given().redirects().follow(false).get("/entitlements/" + entitlement.id).then().statusCode(303)
                .header("Location", Matchers.endsWith("/entitlements/" + entitlement.id));
        RestAssured.given().redirects().follow(false).get("/entitlements/" + entitlement.id + "/edit").then()
                .statusCode(303).header("Location", Matchers.endsWith("/entitlements/" + entitlement.id + "/edit"));

        RestAssured.given().redirects().follow(false).post("/entitlements/" + entitlement.id + "/delete").then()
                .statusCode(303);
        Assertions.assertNull(refreshedEntitlement(entitlement.id));

        String levelName = "Test Level";
        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC).formParam("name", levelName)
                .formParam("description", "Level description").formParam("level", 30).formParam("color", "Red")
                .post("/levels").then().statusCode(303);
        Level level = Level.find("name", levelName).firstResult();
        Assertions.assertNotNull(level);

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC).formParam("name", "Updated Level")
                .formParam("description", "Updated level").formParam("level", 45).formParam("color", "Yellow")
                .post("/levels/" + level.id).then().statusCode(303);
        Level updatedLevel = refreshedLevel(level.id);
        Assertions.assertEquals("Updated Level", updatedLevel.name);

        RestAssured.given().redirects().follow(false).get("/levels").then().statusCode(303).header("Location",
                Matchers.endsWith("/levels"));
        RestAssured.given().redirects().follow(false).get("/levels/" + level.id + "/edit").then().statusCode(303)
                .header("Location", Matchers.endsWith("/levels/" + level.id + "/edit"));

        RestAssured.given().redirects().follow(false).post("/levels/" + level.id + "/delete").then().statusCode(303);
        Assertions.assertNull(refreshedLevel(level.id));

        ensureUser("coverage-superuser", "coverage-superuser@mnemosyne-systems.ai", User.TYPE_SUPERUSER);
        User coverageSuperuser = User.find("email", "coverage-superuser@mnemosyne-systems.ai").firstResult();
        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC).formParam("name", "Coverage Co")
                .formParam("userIds", coverageSuperuser.id).post("/companies").then().statusCode(303);
        Company company = Company.find("name", "Coverage Co").firstResult();
        Assertions.assertNotNull(company);

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("name", "Coverage Co Updated").formParam("country", "United States of America")
                .post("/companies/" + company.id).then().statusCode(303);
        Company updatedCompany = refreshedCompany(company.id);
        Assertions.assertEquals("Coverage Co Updated", updatedCompany.name);

        RestAssured.given().redirects().follow(false).post("/companies/" + company.id + "/delete").then()
                .statusCode(303);
        Assertions.assertNull(refreshedCompany(company.id));
    }

    @Test
    @TestSecurity(user = "admin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "admin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "admin") })
    void reactAppSessionReturnsAdminCategoryLink() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN);

        RestAssured.given().get("/api/app/session").then().statusCode(200).body("role", Matchers.equalTo("admin"))
                .body("navigation.href", Matchers.hasItem("/categories"))
                .body("installationLogoBase64", Matchers.startsWith("data:image/svg+xml;base64,"))
                .body("installationBackgroundBase64",
                        Matchers.anyOf(Matchers.nullValue(), Matchers.startsWith("data:image/")))
                .body("installationHeaderFooterColor", Matchers.matchesPattern("^#[0-9a-f]{6}$"))
                .body("installationHeadersColor", Matchers.matchesPattern("^#[0-9a-f]{6}$"))
                .body("installationButtonsColor", Matchers.matchesPattern("^#[0-9a-f]{6}$"))
                .body("installationUse24HourClock", Matchers.equalTo(false))
                .body("inactivityTimeoutSeconds", Matchers.equalTo(AuthHelper.INACTIVITY_TIMEOUT_SECONDS))
                .body("inactivityWarningSeconds", Matchers.equalTo(AuthHelper.WARNING_LEAD_SECONDS));
    }

    @Test
    @TestSecurity(user = "admin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "admin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "admin") })
    void reactAppSessionReturnsAdminOwnerLink() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN);

        RestAssured.given().get("/api/app/session").then().statusCode(200).body("role", Matchers.equalTo("admin"))
                .body("navigation.href", Matchers.hasItem("/owner"))
                .body("installationLogoBase64", Matchers.startsWith("data:image/svg+xml;base64,"))
                .body("installationBackgroundBase64",
                        Matchers.anyOf(Matchers.nullValue(), Matchers.startsWith("data:image/")))
                .body("installationHeaderFooterColor", Matchers.matchesPattern("^#[0-9a-f]{6}$"))
                .body("installationHeadersColor", Matchers.matchesPattern("^#[0-9a-f]{6}$"))
                .body("installationButtonsColor", Matchers.matchesPattern("^#[0-9a-f]{6}$"))
                .body("installationUse24HourClock", Matchers.equalTo(false))
                .body("inactivityTimeoutSeconds", Matchers.equalTo(AuthHelper.INACTIVITY_TIMEOUT_SECONDS))
                .body("inactivityWarningSeconds", Matchers.equalTo(AuthHelper.WARNING_LEAD_SECONDS));
    }

    @Test
    @TestSecurity(user = "admin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "admin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "admin") })
    void reactAppSessionReturnsAdminCompanyLink() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN);

        RestAssured.given().get("/api/app/session").then().statusCode(200).body("role", Matchers.equalTo("admin"))
                .body("navigation.href", Matchers.hasItem("/companies"))
                .body("installationLogoBase64", Matchers.startsWith("data:image/svg+xml;base64,"))
                .body("installationBackgroundBase64",
                        Matchers.anyOf(Matchers.nullValue(), Matchers.startsWith("data:image/")))
                .body("installationHeaderFooterColor", Matchers.matchesPattern("^#[0-9a-f]{6}$"))
                .body("installationHeadersColor", Matchers.matchesPattern("^#[0-9a-f]{6}$"))
                .body("installationButtonsColor", Matchers.matchesPattern("^#[0-9a-f]{6}$"))
                .body("installationUse24HourClock", Matchers.equalTo(false))
                .body("inactivityTimeoutSeconds", Matchers.equalTo(AuthHelper.INACTIVITY_TIMEOUT_SECONDS))
                .body("inactivityWarningSeconds", Matchers.equalTo(AuthHelper.WARNING_LEAD_SECONDS));
    }

    // @Test
    // void reactLoginRedirectsAdminToAdminDashboard() {
    // ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN);
    //
    // RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC).formParam("username", "admin")
    // .formParam("password", "admin").post("/login").then().statusCode(303)
    // .header("Location", Matchers.equalTo("/"));
    // }
    @Test
    @TestSecurity(user = "support1@mnemosyne-systems.ai", roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = "support1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "admin") })
    void reactCategoriesApiRequiresAdminPrivileges() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);

        RestAssured.given().get("/api/categories").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "admin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "admin") })
    void reactCategoriesApiReturnsAdminCategoryListAndDetail() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN);

        Category category = ensureCategory("Escalation", "First line\n\nExtra detail", true);
        Attachment attachment = ensureCategoryAttachment(category, "category-note.txt", "Category attachment");

        RestAssured.given().get("/api/categories").then().statusCode(200).body("canCreate", Matchers.equalTo(true))
                .body("createPath", Matchers.equalTo("/categories/new"))
                .body("items.name", Matchers.hasItem("Escalation"))
                .body("items.find { it.name == 'Escalation' }.descriptionPreview", Matchers.equalTo("First line"))
                .body("items.find { it.name == 'Escalation' }.isDefault", Matchers.equalTo(true));

        RestAssured.given().get("/api/categories/" + category.id).then().statusCode(200)
                .body("name", Matchers.equalTo("Escalation"))
                .body("description", Matchers.containsString("Extra detail")).body("isDefault", Matchers.equalTo(true))
                .body("editPath", Matchers.equalTo("/categories/" + category.id + "/edit"))
                .body("attachments.name", Matchers.hasItem("category-note.txt"))
                .body("attachments.downloadPath", Matchers.hasItem("/attachments/" + attachment.id + "/data"));
    }

    @Test
    @TestSecurity(user = "admin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "admin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "admin") })
    void reactArticlesApiReturnsAdminArticleListAndDetail() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN);

        Article article = ensureArticle("Admin Runbook", "ops,admin", "## Admin article\n\nSteps");
        Attachment attachment = ensureArticleAttachment(article, "admin-runbook.txt", "Admin attachment");

        RestAssured.given().get("/api/articles").then().statusCode(200).body("canCreate", Matchers.equalTo(true))
                .body("createPath", Matchers.equalTo("/articles/new"))
                .body("items.title", Matchers.hasItem("Admin Runbook"));

        RestAssured.given().get("/api/articles/" + article.id).then().statusCode(200)
                .body("title", Matchers.equalTo("Admin Runbook")).body("tags", Matchers.equalTo("ops,admin"))
                .body("body", Matchers.containsString("Admin article")).body("canEdit", Matchers.equalTo(true))
                .body("canDelete", Matchers.equalTo(true))
                .body("editPath", Matchers.equalTo("/articles/" + article.id + "/edit"))
                .body("attachments.name", Matchers.hasItem("admin-runbook.txt"))
                .body("attachments.downloadPath", Matchers.hasItem("/attachments/" + attachment.id + "/data"));
    }

    @Test
    @TestSecurity(user = "support1@mnemosyne-systems.ai", roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = "support1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "admin") })
    void reactCompaniesApiRequiresAdminPrivileges() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);

        RestAssured.given().get("/api/companies").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "admin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "admin") })
    void reactCompaniesApiReturnsAdminCompanyListAndDetail() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("assigned-user", "assigned-user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("assigned-superuser", "assigned-superuser@mnemosyne-systems.ai", User.TYPE_SUPERUSER);

        Long companyId = ensureCompany("React Company");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Long assignedCompanyId = ensureCompany("Assigned Company");
        ensureCompanyUsers(assignedCompanyId, "assigned-user@mnemosyne-systems.ai",
                "assigned-superuser@mnemosyne-systems.ai");
        Company company = Company.findById(companyId);
        Entitlement entitlement = ensureEntitlement("React Entitlement", "React detail");
        Level level = ensureLevel("React Level", "React level detail", 60, "Blue");
        ensureCompanyEntitlement(company, entitlement, level);

        RestAssured.given().get("/api/companies").then().statusCode(200)
                .body("createPath", Matchers.equalTo("/companies/new"))
                .body("items.name", Matchers.hasItem("React Company"))
                .body("items.find { it.name == 'React Company' }.detailPath",
                        Matchers.equalTo("/companies/" + companyId));

        RestAssured.given().get("/api/companies/" + companyId).then().statusCode(200)
                .body("name", Matchers.equalTo("React Company"))
                .body("selectedUsers.email", Matchers.hasItem("user@mnemosyne-systems.ai"))
                .body("selectedTams.email", Matchers.hasItem("tam1@mnemosyne-systems.ai"))
                .body("userOptions.email", Matchers.not(Matchers.hasItem("assigned-user@mnemosyne-systems.ai")))
                .body("superuserOptions.email",
                        Matchers.not(Matchers.hasItem("assigned-superuser@mnemosyne-systems.ai")))
                .body("entitlementAssignments.entitlementName", Matchers.hasItem("React Entitlement"))
                .body("countries.size()", Matchers.greaterThan(0)).body("timezones.size()", Matchers.greaterThan(0));
    }

    @Test
    @TestSecurity(user = "admin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "admin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "admin") })
    void adminCanUpdateCompanyWithEntitlementAssignments() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN);

        Long companyId = createCompany("Entitlement Update Co");
        Company company = refreshedCompany(companyId);
        Assertions.assertNotNull(company);

        Entitlement entitlement = ensureEntitlement("Company Update Entitlement", "Update detail");
        Level level = ensureLevel("Company Update Level", "Update level", 50, "Orange");

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC).formParam("name", company.name)
                .formParam("entitlementIds", entitlement.id).formParam("levelIds", level.id)
                .formParam("entitlementDates", "2026-03-27")
                .formParam("entitlementDurations", CompanyEntitlement.DURATION_YEARLY).post("/companies/" + company.id)
                .then().statusCode(303);

        CompanyEntitlement savedEntitlement = CompanyEntitlement
                .find("company = ?1 and entitlement = ?2 and supportLevel = ?3", company, entitlement, level)
                .firstResult();
        Assertions.assertNotNull(savedEntitlement);
        Assertions.assertEquals("2026-03-27", savedEntitlement.date.toString());
        Assertions.assertEquals(CompanyEntitlement.DURATION_YEARLY, savedEntitlement.duration);

        Company updatedCompany = refreshedCompany(company.id);
        Assertions.assertTrue(companyHasUser(updatedCompany.id, "entitlement-update-co-superuser@testing.com"));

        RestAssured.given().get("/api/companies/" + company.id).then().statusCode(200).body("selectedSuperusers.email",
                Matchers.hasItem("entitlement-update-co-superuser@testing.com"));
    }

    @Test
    @TestSecurity(user = "support1@mnemosyne-systems.ai", roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = "support1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "admin") })
    void reactOwnerApiRequiresAdminPrivileges() {
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);

        RestAssured.given().get("/api/owner").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "admin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "admin") })
    void reactOwnerApiReturnsAndUpdatesOwnerDetails() {
        ensureUser("admin", "admin@mnemosyne-systems.ai", User.TYPE_ADMIN);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);

        Company ownerCompany = ownerCompany();
        Country country = Country.find("code", "US").firstResult();
        Timezone timezone = Timezone.find("country = ?1 order by name", country).firstResult();
        User supportUser = User.find("email", "support1@mnemosyne-systems.ai").firstResult();
        User tamUser = User.find("email", "tam1@mnemosyne-systems.ai").firstResult();
        String installationLogo = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCI+PHJlY3Qgd2lkdGg9IjI0IiBoZWlnaHQ9IjI0IiByeD0iNCIgZmlsbD0iIzEyMzQ1NiIvPjwvc3ZnPg==";
        String installationBackground = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jx6kAAAAASUVORK5CYII=";

        RestAssured.given().get("/api/owner").then().statusCode(200)
                .body("id", Matchers.equalTo(ownerCompany.id.intValue()))
                .body("name", Matchers.equalTo(ownerCompany.name)).body("countries.size()", Matchers.greaterThan(0))
                .body("timezones.size()", Matchers.greaterThan(0))
                .body("headerFooterColor", Matchers.equalTo("#b00020"))
                .body("headersColor", Matchers.equalTo("#b00020")).body("buttonsColor", Matchers.equalTo("#b00020"))
                .body("use24HourClock", Matchers.equalTo(false))
                .body("logoBase64", Matchers.startsWith("data:image/svg+xml;base64,"))
                .body("backgroundBase64", Matchers.nullValue())
                .body("supportOptions.email", Matchers.hasItem("support1@mnemosyne-systems.ai"))
                .body("tamOptions.email", Matchers.hasItem("tam1@mnemosyne-systems.ai"));

        RestAssured.given().contentType(ContentType.JSON)
                .body(Map.ofEntries(Map.entry("name", "mnemosyne systems"), Map.entry("address1", "Owner Street 1"),
                        Map.entry("address2", "Owner Street 2"), Map.entry("city", "Owner City"),
                        Map.entry("state", "Owner State"), Map.entry("zip", "12345"),
                        Map.entry("phoneNumber", "+45 12345678"), Map.entry("countryId", country.id),
                        Map.entry("timezoneId", timezone.id), Map.entry("supportIds", List.of(supportUser.id)),
                        Map.entry("tamIds", List.of(tamUser.id)), Map.entry("headerFooterColor", "#123456"),
                        Map.entry("headersColor", "#234567"), Map.entry("buttonsColor", "#345678"),
                        Map.entry("use24HourClock", true), Map.entry("logoBase64", installationLogo),
                        Map.entry("backgroundBase64", installationBackground)))
                .post("/api/owner").then().statusCode(200).body("city", Matchers.equalTo("Owner City"))
                .body("headerFooterColor", Matchers.equalTo("#123456"))
                .body("headersColor", Matchers.equalTo("#234567")).body("buttonsColor", Matchers.equalTo("#345678"))
                .body("use24HourClock", Matchers.equalTo(true)).body("logoBase64", Matchers.equalTo(installationLogo))
                .body("backgroundBase64", Matchers.equalTo(installationBackground))
                .body("supportUsers.email", Matchers.hasItem("support1@mnemosyne-systems.ai"))
                .body("tamUsers.email", Matchers.hasItem("tam1@mnemosyne-systems.ai"));

        Company updated = refreshedCompany(ownerCompany.id);
        Installation installation = Installation.find("singletonKey", "installation").firstResult();
        Assertions.assertEquals("Owner City", updated.city);
        Assertions.assertEquals("+45 12345678", updated.phoneNumber);
        Assertions.assertEquals(country.id, updated.country.id);
        Assertions.assertEquals(timezone.id, updated.timezone.id);
        Assertions.assertNotNull(installation);
        Assertions.assertEquals("#123456", installation.headerFooterColor);
        Assertions.assertEquals("#234567", installation.headersColor);
        Assertions.assertEquals("#345678", installation.buttonsColor);
        Assertions.assertEquals(Boolean.TRUE, installation.use24HourClock);
        Assertions.assertEquals(installationLogo, installation.logoBase64);
        Assertions.assertEquals(installationBackground, installation.backgroundBase64);
        Assertions.assertTrue(companyHasUser(ownerCompany.id, "support1@mnemosyne-systems.ai"));
        Assertions.assertTrue(companyHasUser(ownerCompany.id, "tam1@mnemosyne-systems.ai"));
        RestAssured.given().get("/api/app/session").then().statusCode(200)
                .body("installationLogoBase64", Matchers.equalTo(installationLogo))
                .body("installationBackgroundBase64", Matchers.equalTo(installationBackground))
                .body("installationHeaderFooterColor", Matchers.equalTo("#123456"))
                .body("installationHeadersColor", Matchers.equalTo("#234567"))
                .body("installationButtonsColor", Matchers.equalTo("#345678"))
                .body("installationUse24HourClock", Matchers.equalTo(true));
    }

    @Test
    @TestSecurity(user = "admin-json@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "admin-json@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "admin-json") })
    void reactAdminMutationsReturnJsonRedirects() {
        ensureUser("admin-json", "admin-json@mnemosyne-systems.ai", User.TYPE_ADMIN);

        RestAssured.given().header("X-Billetsys-Client", "react").contentType(ContentType.URLENC)
                .formParam("name", "react-admin-user").formParam("email", "react-admin-user@mnemosyne-systems.ai")
                .formParam("type", User.TYPE_USER).formParam("password", "secret").post("/users").then().statusCode(200)
                .body("redirectTo", Matchers.equalTo("/users"));
        User createdUser = User.find("email", "react-admin-user@mnemosyne-systems.ai").firstResult();
        Assertions.assertNotNull(createdUser);

        RestAssured.given().header("X-Billetsys-Client", "react").contentType(ContentType.URLENC)
                .formParam("name", "react-admin-user")
                .formParam("email", "react-admin-user-duplicate@mnemosyne-systems.ai").formParam("type", User.TYPE_USER)
                .formParam("password", "secret").post("/users").then().statusCode(400);

        RestAssured.given().header("X-Billetsys-Client", "react").contentType(ContentType.URLENC)
                .formParam("name", "renamed-admin-user")
                .formParam("email", "react-admin-user-updated@mnemosyne-systems.ai")
                .formParam("type", User.TYPE_SUPPORT).post("/user/" + createdUser.id).then().statusCode(200)
                .body("redirectTo", Matchers.equalTo("/users"));
        User refreshedCreatedUser = refreshedUser(createdUser.id);
        Assertions.assertEquals("react-admin-user", refreshedCreatedUser.name);
        Assertions.assertEquals("react-admin-user-updated@mnemosyne-systems.ai", refreshedCreatedUser.email);
        Assertions.assertEquals(User.TYPE_SUPPORT, refreshedCreatedUser.type);

        RestAssured.given().header("X-Billetsys-Client", "react").multiPart("name", "React Redirect Category")
                .multiPart("description", "Redirect body").multiPart("isDefault", "false").post("/categories").then()
                .statusCode(200).body("redirectTo", Matchers.equalTo("/categories"));
        Category category = Category.find("name", "React Redirect Category").firstResult();
        Assertions.assertNotNull(category);

        RestAssured.given().header("X-Billetsys-Client", "react").post("/categories/" + category.id + "/delete").then()
                .statusCode(200).body("redirectTo", Matchers.equalTo("/categories"));
    }

    @Test
    @TestSecurity(user = "admin-delete@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "admin-delete@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "admin-delete") })
    void adminCanDeleteReferencedUserAssignedToCompany() {
        ensureUser("admin-delete", "admin-delete@mnemosyne-systems.ai", User.TYPE_ADMIN);
        ensureUser("delete-target", "delete-target@mnemosyne-systems.ai", User.TYPE_USER);
        Long companyId = ensureCompany("Delete User Co");
        Long targetUserId = prepareReferencedUserForDelete(companyId, "delete-target@mnemosyne-systems.ai", true);

        RestAssured.given().redirects().follow(false).post("/user/" + targetUserId + "/delete").then().statusCode(303)
                .header("Location", Matchers.endsWith("/users"));

        Assertions.assertNull(refreshedUser(targetUserId));
        Company company = refreshedCompany(companyId);
        Assertions.assertNotNull(company);
        Assertions.assertFalse(companyHasUser(companyId, "delete-target@mnemosyne-systems.ai"));
    }

    @Test
    @TestSecurity(user = "admin-delete-2@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "admin-delete-2@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "admin-delete-2") })
    void adminCanDeleteReferencedNonSuperuserWithoutServerError() {
        ensureUser("admin-delete-2", "admin-delete-2@mnemosyne-systems.ai", User.TYPE_ADMIN);
        ensureUser("delete-target-2", "delete-target-2@mnemosyne-systems.ai", User.TYPE_USER);
        Long companyId = ensureCompany("Delete User Co 2");
        Long targetUserId = prepareReferencedUserForDelete(companyId, "delete-target-2@mnemosyne-systems.ai", false);

        RestAssured.given().redirects().follow(false).post("/user/" + targetUserId + "/delete").then().statusCode(303)
                .header("Location", Matchers.endsWith("/users"));

        Assertions.assertNull(refreshedUser(targetUserId));
        Assertions.assertFalse(companyHasUser(companyId, "delete-target-2@mnemosyne-systems.ai"));
        Assertions.assertTrue(deleteReferencesCleared("delete-target-2@mnemosyne-systems.ai"));
    }

    @Transactional
    Long prepareReferencedUserForDelete(Long companyId, String email, boolean assignToCompany) {
        Company company = Company.findById(companyId);
        User user = User.find("email", email).firstResult();
        Assertions.assertNotNull(company);
        Assertions.assertNotNull(user);
        if (assignToCompany
                && company.users.stream().noneMatch(existing -> existing.id != null && existing.id.equals(user.id))) {
            company.users.add(user);
        }

        Ticket ticket = new Ticket();
        ticket.name = Ticket.nextName(company);
        ticket.title = "Delete target";
        ticket.status = "Open";
        ticket.company = company;
        ticket.requester = user;
        ticket.supportUsers.add(user);
        ticket.tamUsers.add(user);
        ticket.persist();

        Message message = new Message();
        message.ticket = ticket;
        message.body = "Delete target message";
        message.date = java.time.LocalDateTime.now();
        message.author = user;
        message.persist();

        return user.id;
    }

    @Transactional
    boolean deleteReferencesCleared(String email) {
        long requesterCount = Ticket.count("requester.email", email);
        long messageAuthorCount = Message.count("author.email", email);
        long supportCount = Ticket.count("select distinct t from Ticket t join t.supportUsers u where u.email = ?1",
                email);
        long tamCount = Ticket.count("select distinct t from Ticket t join t.tamUsers u where u.email = ?1", email);
        return requesterCount == 0 && messageAuthorCount == 0 && supportCount == 0 && tamCount == 0;
    }

}
