/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.ReportData;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.service.ReportService;
import ai.mnemosyne_systems.service.TicketCreationService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Company isolation for the cached report snapshots.
 * <p>
 * All assertions go through HTTP (fresh persistence context per request), so any staleness observed can only come from
 * the report cache — never from Hibernate L1 session reuse in the test JVM.
 */
@QuarkusTest
class ReportCacheTest extends AccessTestSupport {

    private static final String COMPANY_A = "Report Cache Company A";
    private static final String COMPANY_B = "Report Cache Company B";

    @Inject
    TicketCreationService ticketCreationService;

    @Inject
    ReportService reportService;

    @Test
    @TestSecurity(user = "report-cache-admin@mnemosyne-systems.ai", roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = "report-cache-admin@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "report-cache-admin") })
    void companySnapshotsAreIsolatedAndInvalidated() {
        ensureUser("report-cache-admin", "report-cache-admin@mnemosyne-systems.ai", User.TYPE_ADMIN);
        Long companyA = ensureCompany(COMPANY_A);
        Long companyB = ensureCompany(COMPANY_B);
        Ticket ticketA = ensureTicket(companyA);
        ensureTicket(companyB);
        Long entitlementA = ticketA.companyEntitlement.id;
        Assertions.assertNotNull(entitlementA);

        int totalA = reportTotal(companyA);
        int totalB = reportTotal(companyB);

        // Mechanical cache proof: same key twice in-JVM must return the identical instance.
        ReportData first = reportService.computeReport(ReportService.companyScope(companyA), "all");

        // Direct write bypassing EventService: no invalidation fires, so the
        // cached snapshot for A must stay unchanged (cache-hit proof).
        ensureTicket(companyA);
        ReportData second = reportService.computeReport(ReportService.companyScope(companyA), "all");
        Assertions.assertSame(first, second);
        int staleTotalA = reportTotal(companyA);
        Assertions.assertEquals(totalA, staleTotalA);
        // B's cached snapshot is untouched and contains no A data (isolation proof).
        assertCompanyReport(companyB, totalB, COMPANY_B, COMPANY_A);

        // Hooked write through TicketCreationService (fires saveTicketEvent +
        // recordMessageCreated): the snapshots must refresh. Expect the bypass
        // ticket plus the new one on top of the previously cached total.
        createTicket(companyA, entitlementA);
        Assertions.assertEquals(staleTotalA + 2, reportTotal(companyA));
        // B recomputes after the evict-all but still sees only its own data.
        assertCompanyReport(companyB, totalB, COMPANY_B, COMPANY_A);
    }

    @Test
    @TestSecurity(user = "tam1@mnemosyne-systems.ai", roles = "tam")
    @JwtSecurity(claims = { @Claim(key = "email", value = "tam1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "tam1") })
    void tamScopeNeverLeaksOtherCompanies() {
        Long companyA = ensureCompany(COMPANY_A);
        Long companyB = ensureCompany(COMPANY_B);
        ensureTicket(companyA);
        ensureTicket(companyB);
        ensureCompanyUsers(companyA, "tam1@mnemosyne-systems.ai");

        // No companyId: the user-scoped snapshot must show A but never B.
        RestAssured.given().get("/api/reports").then().statusCode(200)
                .body("company.label", Matchers.hasItem(COMPANY_A))
                .body("company.label", Matchers.not(Matchers.hasItem(COMPANY_B)));
    }

    int reportTotal(Long companyId) {
        return RestAssured.given().queryParam("companyId", companyId).get("/api/reports").then().statusCode(200)
                .extract().path("totalTickets");
    }

    void assertCompanyReport(Long companyId, int expectedTotal, String expectedCompany, String absentCompany) {
        RestAssured.given().queryParam("companyId", companyId).get("/api/reports").then().statusCode(200)
                .body("totalTickets", Matchers.equalTo(expectedTotal))
                .body("company.label", Matchers.hasItem(expectedCompany))
                .body("company.label", Matchers.not(Matchers.hasItem(absentCompany)));
    }

    void createTicket(Long companyId, Long companyEntitlementId) {
        QuarkusTransaction.requiringNew().run(() -> {
            Company company = Company.findById(companyId);
            CompanyEntitlement entitlement = CompanyEntitlement.findById(companyEntitlementId);
            User requester = User.find("email", "report-cache-admin@mnemosyne-systems.ai").firstResult();
            Category category = Category.findDefault();
            ticketCreationService.createTicketWithInitialMessage(
                    new TicketCreationService.TicketCreationRequest("Cache probe ticket", "Open", company, entitlement,
                            category, requester, "Cache probe initial message", null, null, true));
        });
    }
}
