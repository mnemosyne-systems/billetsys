/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Entitlement;
import ai.mnemosyne_systems.model.Level;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.Version;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Company isolation for the cached ticket-bootstrap atoms.
 * <p>
 * All assertions go through HTTP (fresh persistence context per request), so any staleness observed can only come from
 * the bootstrap caches — never from Hibernate L1 session reuse in the test JVM. Methods carry both admin and support
 * roles so hooked admin writes and support reads share one flow; every method restores what it mutates, keeping methods
 * independent of execution order. The versions/categories/company globals are keyed without company scope by design
 * (shared catalog rows), so their tests prove stale/fresh behavior rather than per-company isolation.
 */
@QuarkusTest
class TicketBootstrapCacheTest extends AccessTestSupport {

    @Test
    @TestSecurity(user = "bootstrap-probe@mnemosyne-systems.ai", roles = { "admin", "support" })
    @JwtSecurity(claims = { @Claim(key = "email", value = "bootstrap-probe@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "bootstrap-probe") })
    void entitlementOptionsAreIsolatedAndInvalidated() {
        ensureUser("bootstrap-probe", "bootstrap-probe@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        Long companyA = ensureCompany("Bootstrap Ent Company A");
        Long companyB = ensureCompany("Bootstrap Ent Company B");
        ensureTicket(companyA);
        ensureTicket(companyB);
        addCompanyEntitlement(companyA, "Business");

        // The global companies atom is shared across the whole suite: other
        // test classes populate it before our bypass seeds exist. Evict it
        // with a hooked delete that leaves no residue, so the populate below
        // resolves our company instead of falling back.
        Long preambleCompany = addCompany("Bootstrap Ent Preamble Company");
        deleteCompany(preambleCompany);

        // Populate both entitlement atoms (workbench unordered + role ordered).
        assertWorkbenchEntitlements(companyA, "Business");
        assertSupportEntitlements(companyA, "Business");
        assertWorkbenchLacksEntitlement(companyB, "Business");

        // Bypass write (no events): a second entitlement must stay invisible
        // (cache-hit proof on both atoms).
        addCompanyEntitlement(companyA, "Enterprise");
        assertWorkbenchLacksEntitlement(companyA, "Enterprise");
        assertSupportLacksEntitlement(companyA, "Enterprise");
        assertWorkbenchLacksEntitlement(companyB, "Enterprise");

        // Hooked write (level rename records nothing, but the direct hook in
        // LevelResource.updateLevel evicts every entitlement label cache).
        LevelSnapshot normal = readLevel("Normal");
        postLevel(normal.id(), "Normal-Renamed", normal);
        try {
            assertWorkbenchEntitlements(companyA, "Normal-Renamed");
            assertSupportLevels(companyA, "Normal-Renamed");
        } finally {
            postLevel(normal.id(), "Normal", normal);
        }
        assertWorkbenchEntitlements(companyA, "Business");
        assertSupportLevels(companyA, "Normal");
    }

    @Test
    @TestSecurity(user = "bootstrap-probe@mnemosyne-systems.ai", roles = { "admin", "support" })
    @JwtSecurity(claims = { @Claim(key = "email", value = "bootstrap-probe@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "bootstrap-probe") })
    void versionsCategoriesAndCompaniesRefresh() {
        ensureUser("bootstrap-probe", "bootstrap-probe@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        Long companyA = ensureCompany("Bootstrap Misc Company A");
        Long companyB = ensureCompany("Bootstrap Misc Company B");
        ensureTicket(companyA);
        ensureTicket(companyB);
        // The Enterprise versions atom is never read by the other method, so
        // it cannot be poisoned across methods. The singleton categories and
        // companies atoms can be, so evict them first with hooked deletes
        // that leave no residue.
        Long preambleCategory = addCategory("Bootstrap Preamble Category");
        deleteCategory(preambleCategory);
        Long preambleCompany = addCompany("Bootstrap Preamble Company");
        deleteCompany(preambleCompany);
        // The versions atom is keyed by entitlement, hence shared suite-wide
        // as well. Rewrite it unchanged (fires the direct hook) so the
        // populate below cannot hit another class's entry.
        dropVersion("Enterprise", "NO-SUCH-VERSION-XYZ");
        Long enterpriseEntryId = addCompanyEntitlement(companyA, "Enterprise");

        // Bypass seeds created before populating, so the snapshots include them.
        addVersion("Enterprise", "9.9.8-probe", LocalDate.now().minusDays(1));
        Long categoryId = addCategory("Bootstrap Probe Category");
        Long extraCompanyId = addCompany("Bootstrap Probe Company");
        List<String> versions = supportVersions(companyA, enterpriseEntryId);
        Assertions.assertTrue(versions.contains("9.9.8-probe"));
        assertSupportCategories(companyA, "Bootstrap Probe Category");
        assertWorkbenchCompanies(companyA, "Bootstrap Probe Company");

        // Further bypass writes stay invisible (stale proof).
        addVersion("Enterprise", "9.9.9-probe", LocalDate.now());
        Long categoryId2 = addCategory("Bootstrap Probe Category 2");
        Long extraCompanyId2 = addCompany("Bootstrap Probe Company 2");
        Assertions.assertFalse(supportVersions(companyA, enterpriseEntryId).contains("9.9.9-probe"));
        assertSupportLacksCategory(companyA, "Bootstrap Probe Category 2");
        assertWorkbenchLacksCompany(companyA, "Bootstrap Probe Company 2");

        // Hooked writes refresh (fresh proof) and clean up without residue.
        // Each drop is asserted before the next, so freshness is observed
        // while the other probe row is still present.
        dropVersion("Enterprise", "9.9.8-probe");
        List<String> refreshed = supportVersions(companyA, enterpriseEntryId);
        Assertions.assertFalse(refreshed.contains("9.9.8-probe"));
        Assertions.assertTrue(refreshed.contains("9.9.9-probe"));
        dropVersion("Enterprise", "9.9.9-probe");
        List<String> cleanedVersions = supportVersions(companyA, enterpriseEntryId);
        Assertions.assertFalse(cleanedVersions.contains("9.9.8-probe"));
        Assertions.assertFalse(cleanedVersions.contains("9.9.9-probe"));
        deleteCategory(categoryId);
        assertSupportLacksCategory(companyA, "Bootstrap Probe Category");
        assertSupportCategories(companyA, "Bootstrap Probe Category 2");
        deleteCategory(categoryId2);
        assertSupportLacksCategory(companyA, "Bootstrap Probe Category 2");
        deleteCompany(extraCompanyId);
        assertWorkbenchLacksCompany(companyA, "Bootstrap Probe Company");
        assertWorkbenchCompanies(companyA, "Bootstrap Probe Company 2");
        deleteCompany(extraCompanyId2);
        assertWorkbenchLacksCompany(companyA, "Bootstrap Probe Company 2");

        // B never sees A-scoped option data.
        assertWorkbenchLacksEntitlement(companyB, "Business");
    }

    void assertWorkbenchEntitlements(Long companyId, String labelPart) {
        RestAssured.given().queryParam("companyId", companyId).get("/api/ticket-workbench/bootstrap").then()
                .statusCode(200).body("entitlements.name", Matchers.hasItem(Matchers.containsString(labelPart)));
    }

    void assertWorkbenchLacksEntitlement(Long companyId, String labelPart) {
        RestAssured.given().queryParam("companyId", companyId).get("/api/ticket-workbench/bootstrap").then()
                .statusCode(200)
                .body("entitlements.name", Matchers.not(Matchers.hasItem(Matchers.containsString(labelPart))));
    }

    void assertSupportEntitlements(Long companyId, String name) {
        RestAssured.given().queryParam("companyId", companyId).get("/api/support/tickets/bootstrap").then()
                .statusCode(200).body("companyEntitlements.name", Matchers.hasItem(name));
    }

    void assertSupportLevels(Long companyId, String levelName) {
        RestAssured.given().queryParam("companyId", companyId).get("/api/support/tickets/bootstrap").then()
                .statusCode(200).body("companyEntitlements.levelName", Matchers.hasItem(levelName));
    }

    void assertSupportLacksEntitlement(Long companyId, String labelPart) {
        RestAssured.given().queryParam("companyId", companyId).get("/api/support/tickets/bootstrap").then()
                .statusCode(200)
                .body("companyEntitlements.name", Matchers.not(Matchers.hasItem(Matchers.containsString(labelPart))));
    }

    List<String> supportVersions(Long companyId, Long companyEntitlementId) {
        return RestAssured.given().queryParam("companyId", companyId)
                .queryParam("companyEntitlementId", companyEntitlementId).get("/api/support/tickets/bootstrap").then()
                .statusCode(200).extract().path("versions.name");
    }

    void assertSupportCategories(Long companyId, String name) {
        RestAssured.given().queryParam("companyId", companyId).get("/api/support/tickets/bootstrap").then()
                .statusCode(200).body("categories.name", Matchers.hasItem(name));
    }

    void assertSupportLacksCategory(Long companyId, String name) {
        RestAssured.given().queryParam("companyId", companyId).get("/api/support/tickets/bootstrap").then()
                .statusCode(200).body("categories.name", Matchers.not(Matchers.hasItem(name)));
    }

    void assertWorkbenchCompanies(Long companyId, String name) {
        RestAssured.given().queryParam("companyId", companyId).get("/api/ticket-workbench/bootstrap").then()
                .statusCode(200).body("companies.name", Matchers.hasItem(name));
    }

    void assertWorkbenchLacksCompany(Long companyId, String name) {
        RestAssured.given().queryParam("companyId", companyId).get("/api/ticket-workbench/bootstrap").then()
                .statusCode(200).body("companies.name", Matchers.not(Matchers.hasItem(name)));
    }

    Long addCompanyEntitlement(Long companyId, String entitlementName) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Company company = Company.findById(companyId);
            Entitlement entitlement = Entitlement.find("name", entitlementName).firstResult();
            Level level = Level.find("name", "Normal").firstResult();
            Assertions.assertNotNull(company);
            Assertions.assertNotNull(entitlement);
            Assertions.assertNotNull(level);
            CompanyEntitlement entry = new CompanyEntitlement();
            entry.company = company;
            entry.entitlement = entitlement;
            entry.supportLevel = level;
            entry.persist();
            return entry.id;
        });
    }

    void addVersion(String entitlementName, String versionName, LocalDate date) {
        QuarkusTransaction.requiringNew().run(() -> {
            Entitlement entitlement = Entitlement.find("name", entitlementName).firstResult();
            Assertions.assertNotNull(entitlement);
            Version version = new Version();
            version.entitlement = entitlement;
            version.name = versionName;
            version.date = date;
            version.persist();
        });
    }

    Long addCategory(String name) {
        return QuarkusTransaction.requiringNew().call(() -> {
            ai.mnemosyne_systems.model.Category category = new ai.mnemosyne_systems.model.Category();
            category.name = name;
            category.isDefault = false;
            category.persist();
            return category.id;
        });
    }

    void deleteCategory(Long categoryId) {
        RestAssured.given().header("X-Billetsys-Client", "react").post("/categories/" + categoryId + "/delete").then()
                .statusCode(200);
    }

    Long addCompany(String name) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Company company = new Company();
            company.name = name;
            company.persist();
            return company.id;
        });
    }

    void deleteCompany(Long companyId) {
        RestAssured.given().header("X-Billetsys-Client", "react").post("/companies/" + companyId + "/delete").then()
                .statusCode(200);
    }

    record LevelSnapshot(Long id, String name, String description, Integer level, String color, Integer fromDay,
            Integer fromTime, Integer toDay, Integer toTime, Long countryId, Long timezoneId) {
    }

    LevelSnapshot readLevel(String name) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Level level = Level.find("name", name).firstResult();
            Assertions.assertNotNull(level);
            return new LevelSnapshot(level.id, level.name, level.description, level.level, level.color, level.fromDay,
                    level.fromTime, level.toDay, level.toTime, level.country == null ? null : level.country.id,
                    level.timezone == null ? null : level.timezone.id);
        });
    }

    void postLevel(Long id, String name, LevelSnapshot snapshot) {
        RestAssured.given().header("X-Billetsys-Client", "react").contentType(ContentType.URLENC)
                .formParam("name", name).formParam("description", snapshot.description())
                .formParam("level", snapshot.level()).formParam("color", snapshot.color())
                .formParam("fromDay", snapshot.fromDay()).formParam("fromTime", snapshot.fromTime())
                .formParam("toDay", snapshot.toDay()).formParam("toTime", snapshot.toTime())
                .formParam("countryId", snapshot.countryId()).formParam("timezoneId", snapshot.timezoneId())
                .post("/levels/" + id).then().statusCode(200);
    }

    void dropVersion(String entitlementName, String versionName) {
        Map<String, Object> current = QuarkusTransaction.requiringNew().call(() -> {
            Entitlement entitlement = Entitlement.find("name", entitlementName).firstResult();
            Assertions.assertNotNull(entitlement);
            List<String> ids = new ArrayList<>();
            List<String> names = new ArrayList<>();
            List<String> dates = new ArrayList<>();
            List<Long> levelIds = new ArrayList<>();
            for (Level level : entitlement.supportLevels) {
                levelIds.add(level.id);
            }
            for (Version version : Version.<Version> list("entitlement = ?1 order by date asc, id asc", entitlement)) {
                if (version.name.equals(versionName)) {
                    continue;
                }
                ids.add(String.valueOf(version.id));
                names.add(version.name);
                dates.add(version.date.toString());
            }
            return Map.of("id", entitlement.id, "name", entitlement.name, "description", entitlement.description,
                    "levelIds", levelIds, "versionIds", ids, "versionNames", names, "versionDates", dates);
        });
        @SuppressWarnings("unchecked")
        var request = RestAssured.given().header("X-Billetsys-Client", "react").contentType(ContentType.URLENC)
                .formParam("name", current.get("name")).formParam("description", current.get("description"));
        for (Long levelId : (List<Long>) current.get("levelIds")) {
            request.formParam("levelIds", levelId);
        }
        for (String versionId : (List<String>) current.get("versionIds")) {
            request.formParam("versionIds", versionId);
        }
        for (String keptName : (List<String>) current.get("versionNames")) {
            request.formParam("versionNames", keptName);
        }
        for (String keptDate : (List<String>) current.get("versionDates")) {
            request.formParam("versionDates", keptDate);
        }
        request.post("/entitlements/" + current.get("id")).then().statusCode(200);
    }
}
