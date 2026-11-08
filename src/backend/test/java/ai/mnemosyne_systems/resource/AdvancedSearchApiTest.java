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
import ai.mnemosyne_systems.model.CustomField;
import ai.mnemosyne_systems.model.CustomFieldOption;
import ai.mnemosyne_systems.model.Entitlement;
import ai.mnemosyne_systems.model.Level;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.TicketCustomFieldValue;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.Version;
import ai.mnemosyne_systems.model.event.EventConstants;
import ai.mnemosyne_systems.service.TicketCustomFieldService;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AdvancedSearchApiTest extends AccessTestSupport {

    private static final String SUPPORT_EMAIL = "adv-support@mnemosyne-systems.ai";
    private static final String SUPPORT2_EMAIL = "adv-support2@mnemosyne-systems.ai";
    private static final String TAM_EMAIL = "adv-tam-member@mnemosyne-systems.ai";
    private static final String USER_EMAIL = "adv-user@mnemosyne-systems.ai";
    private static final String SUPERUSER_EMAIL = "adv-superuser@mnemosyne-systems.ai";
    private static final String TAM_ROLE_EMAIL = "adv-tam@mnemosyne-systems.ai";
    private static final String TAM2_ROLE_EMAIL = "adv-tam2@mnemosyne-systems.ai";
    private static final String USER2_EMAIL = "adv-user2@mnemosyne-systems.ai";
    private static final String ADMIN_EMAIL = "adv-admin@mnemosyne-systems.ai";

    private static final String SUPPORT_PATH = "/api/support/tickets/search";
    private static final String SUPERUSER_PATH = "/api/superuser/tickets/search";
    private static final String USER_PATH = "/api/user/tickets/search";

    @Inject
    TicketCustomFieldService ticketCustomFieldService;

    @Test
    @TestSecurity(user = SUPPORT_EMAIL, roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPPORT_EMAIL), @Claim(key = "sub", value = "adv-support") })
    void supportEmptySearchReturnsScopedTickets() {
        ensureUser("adv-support", SUPPORT_EMAIL, User.TYPE_SUPPORT);
        Long companyId = seedCompany("ADV Empty Support Co");
        EntitlementData entry = seedEntitlement(companyId, "ADV Empty Support Entitlement", "Normal");
        ensureUser("adv-support2", SUPPORT2_EMAIL, User.TYPE_SUPPORT);
        Ticket assigned = seedTicket(companyId, entry.id, "ADV Empty Support Assigned", "Assigned", USER_EMAIL,
                SUPPORT_EMAIL, null, null, entry.version100, null, null);
        Ticket otherSupport = seedTicket(companyId, entry.id, "ADV Empty Support Other", "Assigned", USER_EMAIL, null,
                null, null, entry.version100, null, null);
        assignSupport(otherSupport.id, SUPPORT2_EMAIL);

        Set<Long> expected = unionOfListViews("/api/support/tickets");
        Set<Long> actual = searchAllIds(SUPPORT_PATH, Map.of());

        Assertions.assertEquals(expected, actual);
        Assertions.assertTrue(actual.contains(assigned.id));
        Assertions.assertFalse(actual.contains(otherSupport.id));

        RestAssured.given().get(SUPPORT_PATH).then().statusCode(200).body("view", Matchers.equalTo("advanced"))
                .body("title", Matchers.equalTo("Search results"));
    }

    @Test
    @TestSecurity(user = SUPERUSER_EMAIL, roles = "superuser")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPERUSER_EMAIL),
            @Claim(key = "sub", value = "adv-superuser") })
    void superuserEmptySearchReturnsScopedTickets() {
        ensureUser("adv-superuser", SUPERUSER_EMAIL, User.TYPE_SUPERUSER);
        Long companyId = seedCompany("ADV Empty Superuser Co", SUPERUSER_EMAIL);
        EntitlementData entry = seedEntitlement(companyId, "ADV Empty Superuser Entitlement", "Normal");
        Ticket mine = seedTicket(companyId, entry.id, "ADV Empty Superuser Mine", "Assigned", USER_EMAIL, SUPPORT_EMAIL,
                null, null, entry.version100, null, null);
        Long otherCompanyId = seedCompany("ADV Empty Superuser Other Co");
        EntitlementData otherEntry = seedEntitlement(otherCompanyId, "ADV Empty Superuser Other Entitlement", "Normal");
        Ticket other = seedTicket(otherCompanyId, otherEntry.id, "ADV Empty Superuser Other", "Assigned", USER_EMAIL,
                SUPPORT_EMAIL, null, null, otherEntry.version100, null, null);

        Set<Long> expected = unionOfListViews("/api/superuser/tickets");
        Set<Long> actual = searchAllIds(SUPERUSER_PATH, Map.of());

        Assertions.assertEquals(expected, actual);
        Assertions.assertTrue(actual.contains(mine.id));
        Assertions.assertFalse(actual.contains(other.id));
    }

    @Test
    @TestSecurity(user = TAM_ROLE_EMAIL, roles = "tam")
    @JwtSecurity(claims = { @Claim(key = "email", value = TAM_ROLE_EMAIL), @Claim(key = "sub", value = "adv-tam") })
    void tamEmptySearchReturnsScopedTickets() {
        ensureUser("adv-tam", TAM_ROLE_EMAIL, User.TYPE_TAM);
        Long companyId = seedCompany("ADV Empty Tam Co", TAM_ROLE_EMAIL);
        EntitlementData entry = seedEntitlement(companyId, "ADV Empty Tam Entitlement", "Normal");
        Ticket mine = seedTicket(companyId, entry.id, "ADV Empty Tam Mine", "Assigned", USER_EMAIL, SUPPORT_EMAIL, null,
                null, entry.version100, null, null);
        Long otherCompanyId = seedCompany("ADV Empty Tam Other Co");
        EntitlementData otherEntry = seedEntitlement(otherCompanyId, "ADV Empty Tam Other Entitlement", "Normal");
        Ticket other = seedTicket(otherCompanyId, otherEntry.id, "ADV Empty Tam Other", "Assigned", USER_EMAIL,
                SUPPORT_EMAIL, null, null, otherEntry.version100, null, null);

        Set<Long> expected = unionOfListViews("/api/user/tickets");
        Set<Long> actual = searchAllIds(USER_PATH, Map.of());

        Assertions.assertEquals(expected, actual);
        Assertions.assertTrue(actual.contains(mine.id));
        Assertions.assertFalse(actual.contains(other.id));
    }

    @Test
    @TestSecurity(user = USER_EMAIL, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = USER_EMAIL), @Claim(key = "sub", value = "adv-user") })
    void userEmptySearchReturnsScopedTickets() {
        ensureUser("adv-user", USER_EMAIL, User.TYPE_USER);
        ensureUser("adv-user2", USER2_EMAIL, User.TYPE_USER);
        Long companyId = seedCompany("ADV Empty User Co", USER_EMAIL);
        EntitlementData entry = seedEntitlement(companyId, "ADV Empty User Entitlement", "Normal");
        Ticket mine = seedTicket(companyId, entry.id, "ADV Empty User Mine", "Assigned", USER_EMAIL, SUPPORT_EMAIL,
                null, null, entry.version100, null, null);
        Ticket other = seedTicket(companyId, entry.id, "ADV Empty User Other", "Assigned", USER2_EMAIL, SUPPORT_EMAIL,
                null, null, entry.version100, null, null);

        Set<Long> expected = unionOfListViews("/api/user/tickets");
        Set<Long> actual = searchAllIds(USER_PATH, Map.of());

        Assertions.assertEquals(expected, actual);
        Assertions.assertTrue(actual.contains(mine.id));
        Assertions.assertFalse(actual.contains(other.id));
    }

    @Test
    @TestSecurity(user = SUPPORT_EMAIL, roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPPORT_EMAIL), @Claim(key = "sub", value = "adv-support") })
    void searchBootstrapExposesVersionsBeyondTheTicketFormEntitlement() {
        ensureUser("adv-support", SUPPORT_EMAIL, User.TYPE_SUPPORT);

        Response plain = RestAssured.given().get("/api/support/tickets/bootstrap").then().statusCode(200).extract()
                .response();
        List<Integer> companyIds = plain.jsonPath().getList("companies.id", Integer.class);
        Assertions.assertFalse(companyIds.isEmpty());
        Long companyId = companyIds.get(0).longValue();
        List<Integer> formVersionIds = plain.jsonPath().getList("versions.id", Integer.class);

        // A second entitlement for the same company owns versions the ticket form's default entitlement does not.
        EntitlementData entry = seedEntitlement(companyId, "ZZZ Advanced Search Versions", "Normal");

        Response searchBootstrap = RestAssured.given().queryParam("allVersions", "true")
                .get("/api/support/tickets/bootstrap").then().statusCode(200).extract().response();
        List<Integer> searchVersionIds = searchBootstrap.jsonPath().getList("versions.id", Integer.class);

        Assertions.assertTrue(searchVersionIds.contains(entry.version100.intValue()));
        Assertions.assertTrue(searchVersionIds.containsAll(formVersionIds));
    }

    @Test
    @TestSecurity(user = SUPPORT_EMAIL, roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPPORT_EMAIL), @Claim(key = "sub", value = "adv-support") })
    void supportFiltersNeverReachOutOfScopeTickets() {
        ensureUser("adv-support", SUPPORT_EMAIL, User.TYPE_SUPPORT);
        ensureUser("adv-support2", SUPPORT2_EMAIL, User.TYPE_SUPPORT);
        Long inCompanyId = seedCompany("ADV Invariant Support In Co");
        EntitlementData inEntry = seedEntitlement(inCompanyId, "ADV Invariant Support In Entitlement", "Normal");
        Ticket inScope = seedTicket(inCompanyId, inEntry.id, "ADV Invariant Support In Scope", "Assigned", USER_EMAIL,
                SUPPORT_EMAIL, null, null, inEntry.version100, null, null);
        Long outCompanyId = seedCompany("ADV Invariant Support Out Co");
        EntitlementData outEntry = seedEntitlement(outCompanyId, "ADV Invariant Support Out Entitlement", "Normal");
        Ticket outOfScope = seedTicket(outCompanyId, outEntry.id, "ADV Invariant Support Out Of Scope", "Assigned",
                USER_EMAIL, null, null, null, outEntry.version100, null, null);
        assignSupport(outOfScope.id, SUPPORT2_EMAIL);
        Long fieldId = seedCustomField("ADV Invariant Support Field", CustomField.TYPE_TEXT, List.of());
        setCustomValue(outOfScope.id, fieldId, "secret-probe", null, null);

        Assertions.assertTrue(searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Invariant Support Out Of Scope")).isEmpty());
        Assertions.assertTrue(searchAllIds(SUPPORT_PATH, Map.of("companyId", outCompanyId)).isEmpty());
        Assertions.assertTrue(searchAllIds(SUPPORT_PATH, Map.of("companyEntitlementId", outEntry.id)).isEmpty());
        Assertions.assertTrue(searchAllIds(SUPPORT_PATH, Map.of("cf." + fieldId, "secret-probe")).isEmpty());

        Set<Long> combined = searchAllIds(SUPPORT_PATH,
                Map.of("q", "ADV Invariant Support", "companyId", outCompanyId));
        Assertions.assertTrue(combined.isEmpty());
        Set<Long> matchingInScope = searchAllIds(SUPPORT_PATH, Map.of("companyId", inCompanyId));
        Assertions.assertEquals(Set.of(inScope.id), matchingInScope);
    }

    @Test
    @TestSecurity(user = SUPERUSER_EMAIL, roles = "superuser")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPERUSER_EMAIL),
            @Claim(key = "sub", value = "adv-superuser") })
    void superuserFiltersNeverReachOutOfScopeTickets() {
        ensureUser("adv-superuser", SUPERUSER_EMAIL, User.TYPE_SUPERUSER);
        Long inCompanyId = seedCompany("ADV Invariant Superuser In Co", SUPERUSER_EMAIL);
        EntitlementData inEntry = seedEntitlement(inCompanyId, "ADV Invariant Superuser In Entitlement", "Normal");
        Ticket inScope = seedTicket(inCompanyId, inEntry.id, "ADV Invariant Superuser In Scope", "Assigned", USER_EMAIL,
                SUPPORT_EMAIL, null, null, inEntry.version100, null, null);
        Long outCompanyId = seedCompany("ADV Invariant Superuser Out Co");
        EntitlementData outEntry = seedEntitlement(outCompanyId, "ADV Invariant Superuser Out Entitlement", "Normal");
        Ticket outOfScope = seedTicket(outCompanyId, outEntry.id, "ADV Invariant Superuser Out Of Scope", "Assigned",
                USER_EMAIL, SUPPORT_EMAIL, null, null, outEntry.version100, null, null);

        Assertions.assertTrue(
                searchAllIds(SUPERUSER_PATH, Map.of("q", "ADV Invariant Superuser Out Of Scope")).isEmpty());
        Assertions.assertTrue(searchAllIds(SUPERUSER_PATH, Map.of("companyId", outCompanyId)).isEmpty());
        Assertions.assertTrue(searchAllIds(SUPERUSER_PATH, Map.of("companyEntitlementId", outEntry.id)).isEmpty());
        Assertions.assertEquals(Set.of(inScope.id), searchAllIds(SUPERUSER_PATH, Map.of("companyId", inCompanyId)));
        Assertions.assertFalse(searchAllIds(SUPERUSER_PATH, Map.of()).contains(outOfScope.id));
    }

    @Test
    @TestSecurity(user = TAM_ROLE_EMAIL, roles = "tam")
    @JwtSecurity(claims = { @Claim(key = "email", value = TAM_ROLE_EMAIL), @Claim(key = "sub", value = "adv-tam") })
    void tamFiltersNeverReachOutOfScopeTickets() {
        ensureUser("adv-tam", TAM_ROLE_EMAIL, User.TYPE_TAM);
        Long inCompanyId = seedCompany("ADV Invariant Tam In Co", TAM_ROLE_EMAIL);
        EntitlementData inEntry = seedEntitlement(inCompanyId, "ADV Invariant Tam In Entitlement", "Normal");
        Ticket inScope = seedTicket(inCompanyId, inEntry.id, "ADV Invariant Tam In Scope", "Assigned", USER_EMAIL,
                SUPPORT_EMAIL, null, null, inEntry.version100, null, null);
        Long outCompanyId = seedCompany("ADV Invariant Tam Out Co");
        EntitlementData outEntry = seedEntitlement(outCompanyId, "ADV Invariant Tam Out Entitlement", "Normal");
        Ticket outOfScope = seedTicket(outCompanyId, outEntry.id, "ADV Invariant Tam Out Of Scope", "Assigned",
                USER_EMAIL, SUPPORT_EMAIL, null, null, outEntry.version100, null, null);

        Assertions.assertTrue(searchAllIds(USER_PATH, Map.of("q", "ADV Invariant Tam Out Of Scope")).isEmpty());
        Assertions.assertTrue(searchAllIds(USER_PATH, Map.of("companyId", outCompanyId)).isEmpty());
        Assertions.assertTrue(searchAllIds(USER_PATH, Map.of("companyEntitlementId", outEntry.id)).isEmpty());
        Assertions.assertEquals(Set.of(inScope.id), searchAllIds(USER_PATH, Map.of("companyId", inCompanyId)));
        Assertions.assertFalse(searchAllIds(USER_PATH, Map.of()).contains(outOfScope.id));
    }

    @Test
    @TestSecurity(user = USER_EMAIL, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = USER_EMAIL), @Claim(key = "sub", value = "adv-user") })
    void userFiltersNeverReachOutOfScopeTickets() {
        ensureUser("adv-user", USER_EMAIL, User.TYPE_USER);
        ensureUser("adv-user2", USER2_EMAIL, User.TYPE_USER);
        Long companyId = seedCompany("ADV Invariant User Co", USER_EMAIL, USER2_EMAIL);
        EntitlementData entry = seedEntitlement(companyId, "ADV Invariant User Entitlement", "Normal");
        Ticket inScope = seedTicket(companyId, entry.id, "ADV Invariant User In Scope", "Assigned", USER_EMAIL,
                SUPPORT_EMAIL, null, null, entry.version100, null, null);
        Ticket outOfScope = seedTicket(companyId, entry.id, "ADV Invariant User Out Of Scope", "Assigned", USER2_EMAIL,
                SUPPORT_EMAIL, null, null, entry.version100, null, null);
        Long fieldId = seedCustomField("ADV Invariant User Field", CustomField.TYPE_TEXT, List.of());
        setCustomValue(outOfScope.id, fieldId, "user-secret-probe", null, null);

        Assertions.assertTrue(searchAllIds(USER_PATH, Map.of("q", "ADV Invariant User Out Of Scope")).isEmpty());
        Assertions.assertTrue(searchAllIds(USER_PATH, Map.of("cf." + fieldId, "user-secret-probe")).isEmpty());
        Assertions.assertEquals(Set.of(inScope.id),
                searchAllIds(USER_PATH, Map.of("q", "ADV Invariant User In Scope")));
    }

    @Test
    @TestSecurity(user = SUPPORT_EMAIL, roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPPORT_EMAIL), @Claim(key = "sub", value = "adv-support") })
    void supportStatusFilterUsesDisplayedStatus() {
        ensureUser("adv-support", SUPPORT_EMAIL, User.TYPE_SUPPORT);
        Long companyId = seedCompany("ADV Status Support Co");
        EntitlementData entry = seedEntitlement(companyId, "ADV Status Support Entitlement", "Normal");
        Ticket assignedOpen = seedTicket(companyId, entry.id, "ADV Status Support Assigned Open", "Open", USER_EMAIL,
                SUPPORT_EMAIL, null, null, entry.version100, null, null);
        Ticket unassignedOpen = seedTicket(companyId, entry.id, "ADV Status Support Unassigned Open", "Open",
                USER_EMAIL, null, null, null, entry.version100, null, null);
        Ticket assignedWaiting = seedTicket(companyId, entry.id, "ADV Status Support Assigned Waiting",
                "Waiting for External Feedback", USER_EMAIL, SUPPORT_EMAIL, null, null, entry.version100, null, null);

        Map<String, Set<Long>> listStatuses = listStatusIds("/api/support/tickets");
        Assertions.assertTrue(listStatuses.getOrDefault("Assigned", Set.of()).contains(assignedOpen.id));
        Assertions.assertFalse(listStatuses.getOrDefault("Open", Set.of()).contains(assignedOpen.id));
        Assertions.assertTrue(listStatuses.getOrDefault("Open", Set.of()).contains(unassignedOpen.id));
        Assertions.assertTrue(
                listStatuses.getOrDefault("Waiting for External Feedback", Set.of()).contains(assignedWaiting.id));
        assertStatusFiltersMatchList(SUPPORT_PATH, "/api/support/tickets");
    }

    @Test
    @TestSecurity(user = SUPERUSER_EMAIL, roles = "superuser")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPERUSER_EMAIL),
            @Claim(key = "sub", value = "adv-superuser") })
    void superuserStatusFilterUsesDisplayedStatus() {
        ensureUser("adv-superuser", SUPERUSER_EMAIL, User.TYPE_SUPERUSER);
        Long companyId = seedCompany("ADV Status Superuser Co", SUPERUSER_EMAIL);
        EntitlementData entry = seedEntitlement(companyId, "ADV Status Superuser Entitlement", "Normal");
        Ticket assignedOpen = seedTicket(companyId, entry.id, "ADV Status Superuser Assigned Open", "Open", USER_EMAIL,
                SUPPORT_EMAIL, null, null, entry.version100, null, null);
        Ticket unassignedOpen = seedTicket(companyId, entry.id, "ADV Status Superuser Unassigned Open", "Open",
                USER_EMAIL, null, null, null, entry.version100, null, null);
        Ticket assignedWaiting = seedTicket(companyId, entry.id, "ADV Status Superuser Assigned Waiting",
                "Waiting for External Feedback", USER_EMAIL, SUPPORT_EMAIL, null, null, entry.version100, null, null);

        Map<String, Set<Long>> listStatuses = listStatusIds("/api/superuser/tickets");
        Assertions.assertTrue(listStatuses.getOrDefault("Assigned", Set.of()).contains(assignedOpen.id));
        Assertions.assertFalse(listStatuses.getOrDefault("Open", Set.of()).contains(assignedOpen.id));
        Assertions.assertTrue(listStatuses.getOrDefault("Open", Set.of()).contains(unassignedOpen.id));
        Assertions.assertTrue(listStatuses.getOrDefault("In Progress", Set.of()).contains(assignedWaiting.id));
        Assertions.assertFalse(
                listStatuses.getOrDefault("Waiting for External Feedback", Set.of()).contains(assignedWaiting.id));
        assertStatusFiltersMatchList(SUPERUSER_PATH, "/api/superuser/tickets");
    }

    @Test
    @TestSecurity(user = TAM_ROLE_EMAIL, roles = "tam")
    @JwtSecurity(claims = { @Claim(key = "email", value = TAM_ROLE_EMAIL), @Claim(key = "sub", value = "adv-tam") })
    void tamStatusFilterUsesDisplayedStatus() {
        ensureUser("adv-tam", TAM_ROLE_EMAIL, User.TYPE_TAM);
        Long companyId = seedCompany("ADV Status Tam Co", TAM_ROLE_EMAIL);
        EntitlementData entry = seedEntitlement(companyId, "ADV Status Tam Entitlement", "Normal");
        Ticket assignedOpen = seedTicket(companyId, entry.id, "ADV Status Tam Assigned Open", "Open", USER_EMAIL,
                SUPPORT_EMAIL, null, null, entry.version100, null, null);
        Ticket unassignedOpen = seedTicket(companyId, entry.id, "ADV Status Tam Unassigned Open", "Open", USER_EMAIL,
                null, null, null, entry.version100, null, null);
        Ticket assignedWaiting = seedTicket(companyId, entry.id, "ADV Status Tam Assigned Waiting",
                "Waiting for External Feedback", USER_EMAIL, SUPPORT_EMAIL, null, null, entry.version100, null, null);

        Map<String, Set<Long>> listStatuses = listStatusIds("/api/user/tickets");
        Assertions.assertTrue(listStatuses.getOrDefault("Assigned", Set.of()).contains(assignedOpen.id));
        Assertions.assertFalse(listStatuses.getOrDefault("Open", Set.of()).contains(assignedOpen.id));
        Assertions.assertTrue(listStatuses.getOrDefault("Open", Set.of()).contains(unassignedOpen.id));
        Assertions.assertTrue(listStatuses.getOrDefault("In Progress", Set.of()).contains(assignedWaiting.id));
        Assertions.assertFalse(
                listStatuses.getOrDefault("Waiting for External Feedback", Set.of()).contains(assignedWaiting.id));
        assertStatusFiltersMatchList(USER_PATH, "/api/user/tickets");
    }

    @Test
    @TestSecurity(user = USER_EMAIL, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = USER_EMAIL), @Claim(key = "sub", value = "adv-user") })
    void userStatusFilterUsesDisplayedStatus() {
        ensureUser("adv-user", USER_EMAIL, User.TYPE_USER);
        Long companyId = seedCompany("ADV Status User Co", USER_EMAIL);
        EntitlementData entry = seedEntitlement(companyId, "ADV Status User Entitlement", "Normal");
        Ticket assignedOpen = seedTicket(companyId, entry.id, "ADV Status User Assigned Open", "Open", USER_EMAIL,
                SUPPORT_EMAIL, null, null, entry.version100, null, null);
        Ticket unassignedOpen = seedTicket(companyId, entry.id, "ADV Status User Unassigned Open", "Open", USER_EMAIL,
                null, null, null, entry.version100, null, null);
        Ticket assignedWaiting = seedTicket(companyId, entry.id, "ADV Status User Assigned Waiting",
                "Waiting for External Feedback", USER_EMAIL, SUPPORT_EMAIL, null, null, entry.version100, null, null);

        Map<String, Set<Long>> listStatuses = listStatusIds("/api/user/tickets");
        Assertions.assertTrue(listStatuses.getOrDefault("Assigned", Set.of()).contains(assignedOpen.id));
        Assertions.assertFalse(listStatuses.getOrDefault("Open", Set.of()).contains(assignedOpen.id));
        Assertions.assertTrue(listStatuses.getOrDefault("Open", Set.of()).contains(unassignedOpen.id));
        Assertions.assertTrue(listStatuses.getOrDefault("In Progress", Set.of()).contains(assignedWaiting.id));
        Assertions.assertFalse(
                listStatuses.getOrDefault("Waiting for External Feedback", Set.of()).contains(assignedWaiting.id));
        assertStatusFiltersMatchList(USER_PATH, "/api/user/tickets");
    }

    @Test
    @TestSecurity(user = USER_EMAIL, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = USER_EMAIL), @Claim(key = "sub", value = "adv-user") })
    void userIgnoredFiltersDoNotChangeResults() {
        ensureUser("adv-user", USER_EMAIL, User.TYPE_USER);
        ensureUser("adv-user2", USER2_EMAIL, User.TYPE_USER);
        ensureUser("adv-support2", SUPPORT2_EMAIL, User.TYPE_SUPPORT);
        ensureUser("adv-tam-member", TAM_EMAIL, User.TYPE_TAM);
        Long companyId = seedCompany("ADV Ignored User Co", USER_EMAIL);
        EntitlementData entry = seedEntitlement(companyId, "ADV Ignored User Entitlement", "Normal");
        Ticket mine = seedTicket(companyId, entry.id, "ADV Ignored User Ticket", "Assigned", USER_EMAIL, SUPPORT_EMAIL,
                TAM_EMAIL, null, entry.version100, null, null);
        Long otherCompanyId = seedCompany("ADV Ignored User Other Co");
        EntitlementData otherEntry = seedEntitlement(otherCompanyId, "ADV Ignored User Other Entitlement", "Normal");
        Ticket other = seedTicket(otherCompanyId, otherEntry.id, "ADV Ignored User Other Ticket", "Assigned",
                USER2_EMAIL, null, null, null, otherEntry.version100, null, null);

        Set<Long> baseline = searchAllIds(USER_PATH, Map.of());
        Assertions.assertTrue(baseline.contains(mine.id));

        Map<String, Object> ignored = new LinkedHashMap<>();
        ignored.put("companyId", otherCompanyId);
        ignored.put("companyEntitlementId", otherEntry.id);
        ignored.put("entitlementId", otherEntry.entitlementId);
        ignored.put("levelId", otherEntry.levelId);
        ignored.put("supportUserId", userId(SUPPORT2_EMAIL));
        ignored.put("tamUserId", userId(TAM_EMAIL));
        ignored.put("requesterId", userId(USER2_EMAIL));
        ignored.put("supportUser", "support2");
        ignored.put("tamUser", "tam1");
        ignored.put("entitlementName", "ADV Ignored User Other Entitlement");
        ignored.put("levelName", otherEntry.levelName);
        ignored.put("requester", "adv-user2");
        for (Map.Entry<String, Object> parameter : ignored.entrySet()) {
            Assertions.assertEquals(baseline, searchAllIds(USER_PATH, Map.of(parameter.getKey(), parameter.getValue())),
                    "Ignored user filter must not change results: " + parameter.getKey());
        }
        Assertions.assertFalse(searchAllIds(USER_PATH, Map.of()).contains(other.id));
    }

    @Test
    @TestSecurity(user = TAM_ROLE_EMAIL, roles = "tam")
    @JwtSecurity(claims = { @Claim(key = "email", value = TAM_ROLE_EMAIL), @Claim(key = "sub", value = "adv-tam") })
    void tamIgnoredFiltersDoNotChangeResults() {
        ensureUser("adv-tam", TAM_ROLE_EMAIL, User.TYPE_TAM);
        ensureUser("adv-tam2", TAM2_ROLE_EMAIL, User.TYPE_TAM);
        ensureUser("adv-tam-member", TAM_EMAIL, User.TYPE_TAM);
        ensureUser("adv-user2", USER2_EMAIL, User.TYPE_USER);
        Long companyId = seedCompany("ADV Ignored Tam Co", TAM_ROLE_EMAIL);
        EntitlementData entry = seedEntitlement(companyId, "ADV Ignored Tam Entitlement", "Normal");
        Ticket mine = seedTicket(companyId, entry.id, "ADV Ignored Tam Ticket", "Assigned", USER_EMAIL, SUPPORT_EMAIL,
                TAM_EMAIL, null, entry.version100, null, null);

        Set<Long> baseline = searchAllIds(USER_PATH, Map.of());
        Assertions.assertTrue(baseline.contains(mine.id));

        Assertions.assertEquals(baseline, searchAllIds(USER_PATH, Map.of("tamUserId", userId(TAM2_ROLE_EMAIL))));
        Assertions.assertEquals(baseline, searchAllIds(USER_PATH, Map.of("tamUser", "adv-tam2")));
        Assertions.assertEquals(baseline, searchAllIds(USER_PATH, Map.of("requesterId", userId(USER2_EMAIL))));
        Assertions.assertEquals(baseline, searchAllIds(USER_PATH, Map.of("requester", "adv-user2")));
    }

    @Test
    @TestSecurity(user = SUPPORT_EMAIL, roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPPORT_EMAIL), @Claim(key = "sub", value = "adv-support") })
    void supportRequesterFilterIsIgnored() {
        ensureUser("adv-support", SUPPORT_EMAIL, User.TYPE_SUPPORT);
        ensureUser("adv-user2", USER2_EMAIL, User.TYPE_USER);
        Long companyId = seedCompany("ADV Ignored Support Co");
        EntitlementData entry = seedEntitlement(companyId, "ADV Ignored Support Entitlement", "Normal");
        Ticket mine = seedTicket(companyId, entry.id, "ADV Ignored Support Ticket", "Assigned", USER_EMAIL,
                SUPPORT_EMAIL, null, null, entry.version100, null, null);

        Set<Long> baseline = searchAllIds(SUPPORT_PATH, Map.of());
        Assertions.assertTrue(baseline.contains(mine.id));
        Assertions.assertEquals(baseline, searchAllIds(SUPPORT_PATH, Map.of("requesterId", userId(USER2_EMAIL))));
        Assertions.assertEquals(baseline, searchAllIds(SUPPORT_PATH, Map.of("requester", "adv-user2")));
    }

    @Test
    @TestSecurity(user = SUPERUSER_EMAIL, roles = "superuser")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPERUSER_EMAIL),
            @Claim(key = "sub", value = "adv-superuser") })
    void superuserRequesterFilterIsIgnored() {
        ensureUser("adv-superuser", SUPERUSER_EMAIL, User.TYPE_SUPERUSER);
        ensureUser("adv-user2", USER2_EMAIL, User.TYPE_USER);
        Long companyId = seedCompany("ADV Ignored Superuser Co", SUPERUSER_EMAIL);
        EntitlementData entry = seedEntitlement(companyId, "ADV Ignored Superuser Entitlement", "Normal");
        Ticket mine = seedTicket(companyId, entry.id, "ADV Ignored Superuser Ticket", "Assigned", USER_EMAIL,
                SUPPORT_EMAIL, null, null, entry.version100, null, null);

        Set<Long> baseline = searchAllIds(SUPERUSER_PATH, Map.of());
        Assertions.assertTrue(baseline.contains(mine.id));
        Assertions.assertEquals(baseline, searchAllIds(SUPERUSER_PATH, Map.of("requesterId", userId(USER2_EMAIL))));
        Assertions.assertEquals(baseline, searchAllIds(SUPERUSER_PATH, Map.of("requester", "adv-user2")));
    }

    @Test
    @TestSecurity(user = SUPPORT_EMAIL, roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPPORT_EMAIL), @Claim(key = "sub", value = "adv-support") })
    void supportIndividualAndCombinedFilters() {
        ensureUser("adv-support", SUPPORT_EMAIL, User.TYPE_SUPPORT);
        ensureUser("adv-tam", TAM_ROLE_EMAIL, User.TYPE_TAM);
        ensureUser("adv-tam2", TAM2_ROLE_EMAIL, User.TYPE_TAM);
        ensureUser("adv-tam-member", TAM_EMAIL, User.TYPE_TAM);
        FilterData data = seedFilterData();

        Set<Long> prefix = searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Filter"));
        Assertions.assertEquals(Set.of(data.alpha, data.beta, data.gamma, data.delta), prefix);

        Assertions.assertEquals(Set.of(data.alpha, data.beta, data.delta),
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Filter", "status", "assigned")));
        Assertions.assertEquals(Set.of(data.alpha, data.beta, data.delta),
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Filter", "status", "open,assigned")));
        Assertions.assertEquals(Set.of(data.gamma),
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Filter", "status", "Closed")));
        Assertions.assertEquals(Set.of(data.alpha, data.gamma),
                searchAllIds(SUPPORT_PATH, Map.of("categoryId", data.categoryAlpha)));
        Assertions.assertEquals(Set.of(data.alpha, data.beta),
                searchAllIds(SUPPORT_PATH, Map.of("affectsVersionId", data.version100)));
        Assertions.assertEquals(Set.of(data.alpha),
                searchAllIds(SUPPORT_PATH, Map.of("resolvedVersionId", data.version101)));
        Assertions.assertEquals(Set.of(data.alpha, data.beta, data.gamma),
                searchAllIds(SUPPORT_PATH, Map.of("companyId", data.companyA)));
        Assertions.assertEquals(Set.of(data.alpha, data.beta),
                searchAllIds(SUPPORT_PATH, Map.of("companyEntitlementId", data.entitlementAlpha)));
        Assertions.assertEquals(Set.of(data.alpha, data.beta),
                searchAllIds(SUPPORT_PATH, Map.of("entitlementId", data.entitlementAId)));
        Assertions.assertEquals(Set.of(data.alpha, data.beta),
                searchAllIds(SUPPORT_PATH, Map.of("levelId", data.levelAId)));
        Assertions.assertEquals(Set.of(data.alpha, data.beta), searchAllIds(SUPPORT_PATH,
                Map.of("companyId", data.companyA, "entitlementName", "ADV Filter Entitlement A")));
        Assertions.assertEquals(Set.of(data.alpha, data.beta),
                searchAllIds(SUPPORT_PATH, Map.of("companyId", data.companyA, "levelName", "ADV Filter Level A")));

        Assertions.assertEquals(Set.of(data.alpha, data.beta, data.gamma, data.delta),
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Filter", "supportUserId", userId(SUPPORT_EMAIL))));
        Assertions.assertTrue(
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Filter", "supportUser", "adv-support2")).isEmpty());
        Assertions.assertEquals(Set.of(data.alpha, data.beta, data.gamma, data.delta),
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Filter", "supportUser", "adv-support")));
        Assertions.assertEquals(Set.of(data.alpha),
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Filter", "tamUserId", userId(TAM_EMAIL))));
        Assertions.assertEquals(Set.of(data.beta),
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Filter", "tamUser", "adv-tam2")));

        Assertions.assertEquals(Set.of(data.alpha), searchAllIds(SUPPORT_PATH, Map.of("q", "Alpha", "status",
                "Assigned", "companyId", data.companyA, "categoryId", data.categoryAlpha, "levelId", data.levelAId)));
    }

    @Test
    @TestSecurity(user = SUPPORT_EMAIL, roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPPORT_EMAIL), @Claim(key = "sub", value = "adv-support") })
    void supportDateFiltersUseInclusiveRanges() {
        ensureUser("adv-support", SUPPORT_EMAIL, User.TYPE_SUPPORT);
        Long companyId = seedCompany("ADV Date Co");
        EntitlementData entry = seedEntitlement(companyId, "ADV Date Entitlement", "Normal");
        Ticket old = seedTicket(companyId, entry.id, "ADV Date Old", "Assigned", USER_EMAIL, SUPPORT_EMAIL, null, null,
                entry.version100, null, LocalDateTime.of(2026, 3, 1, 9, 0));
        seedMessage(old.id, "ADV Date Old public", SUPPORT_EMAIL, LocalDateTime.of(2026, 2, 1, 10, 0), true);
        insertOpenedEvent(old.id, companyId, LocalDateTime.of(2026, 1, 5, 12, 0));
        Ticket recent = seedTicket(companyId, entry.id, "ADV Date Recent", "Assigned", USER_EMAIL, SUPPORT_EMAIL, null,
                null, entry.version100, null, LocalDateTime.of(2026, 3, 10, 9, 0));
        seedMessage(recent.id, "ADV Date Recent public", SUPPORT_EMAIL, LocalDateTime.of(2026, 2, 10, 23, 59, 59),
                true);
        insertOpenedEvent(recent.id, companyId, LocalDateTime.of(2026, 1, 20, 0, 0));
        Ticket withoutDates = seedTicket(companyId, entry.id, "ADV Date Without Dates", "Assigned", USER_EMAIL,
                SUPPORT_EMAIL, null, null, entry.version100, null, null);

        Assertions.assertEquals(Set.of(old.id), searchAllIds(SUPPORT_PATH,
                Map.of("q", "ADV Date", "activityFrom", "2026-02-01", "activityTo", "2026-02-01")));
        Assertions.assertEquals(Set.of(recent.id),
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Date", "activityFrom", "2026-02-10")));
        Assertions.assertEquals(Set.of(old.id), searchAllIds(SUPPORT_PATH,
                Map.of("q", "ADV Date", "createdFrom", "2026-01-05", "createdTo", "2026-01-05")));
        Assertions.assertEquals(Set.of(recent.id),
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Date", "createdFrom", "2026-01-20")));
        Assertions.assertEquals(Set.of(old.id), searchAllIds(SUPPORT_PATH,
                Map.of("q", "ADV Date", "resolvedFrom", "2026-03-01", "resolvedTo", "2026-03-01")));
        Assertions.assertEquals(Set.of(recent.id),
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Date", "resolvedFrom", "2026-03-09")));
        Assertions.assertTrue(
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Date", "activityFrom", "2030-01-01")).isEmpty());
        Assertions.assertFalse(searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Date", "activityFrom", "2026-02-01"))
                .contains(withoutDates.id));
    }

    @Test
    @TestSecurity(user = SUPPORT_EMAIL, roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPPORT_EMAIL), @Claim(key = "sub", value = "adv-support") })
    void supportCustomFieldFilters() {
        ensureUser("adv-support", SUPPORT_EMAIL, User.TYPE_SUPPORT);
        Long companyId = seedCompany("ADV Custom Field Co");
        EntitlementData entry = seedEntitlement(companyId, "ADV Custom Field Entitlement", "Normal");
        Ticket first = seedTicket(companyId, entry.id, "ADV Custom Field First", "Assigned", USER_EMAIL, SUPPORT_EMAIL,
                null, null, entry.version100, null, null);
        Ticket second = seedTicket(companyId, entry.id, "ADV Custom Field Second", "Assigned", USER_EMAIL,
                SUPPORT_EMAIL, null, null, entry.version100, null, null);
        Long textField = seedCustomField("ADV CF Text", CustomField.TYPE_TEXT, List.of());
        Long numberField = seedCustomField("ADV CF Number", CustomField.TYPE_NUMBER, List.of());
        Long dateField = seedCustomField("ADV CF Date", CustomField.TYPE_DATE, List.of());
        Long selectField = seedCustomField("ADV CF Select", CustomField.TYPE_SELECT, List.of("One", "Two"));
        setCustomValue(first.id, textField, "Hello World", null, null);
        setCustomValue(first.id, numberField, null, new BigDecimal("10"), null);
        setCustomValue(first.id, dateField, null, null, LocalDate.of(2026, 4, 1));
        setCustomValue(first.id, selectField, "One", null, null);
        setCustomValue(second.id, textField, "Something Else", null, null);
        setCustomValue(second.id, numberField, null, new BigDecimal("20"), null);
        setCustomValue(second.id, dateField, null, null, LocalDate.of(2026, 4, 15));
        setCustomValue(second.id, selectField, "Two", null, null);

        Assertions.assertEquals(Set.of(first.id),
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Custom Field", "cf." + textField, "hello")));
        Assertions.assertEquals(Set.of(first.id),
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Custom Field", "cf." + textField, "Hello Wor")));
        Assertions.assertTrue(
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Custom Field", "cf." + textField, "%")).isEmpty());
        Assertions.assertTrue(
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Custom Field", "cf." + textField, "_")).isEmpty());
        Assertions.assertEquals(Set.of(first.id),
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Custom Field", "cf." + selectField, "One")));
        Assertions.assertEquals(Set.of(first.id),
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Custom Field", "cf." + numberField, "10")));
        Assertions.assertEquals(Set.of(second.id),
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Custom Field", "cf." + numberField + ".from", "11")));
        Assertions.assertEquals(Set.of(first.id), searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Custom Field",
                "cf." + numberField + ".from", "5", "cf." + numberField + ".to", "15")));
        Assertions.assertEquals(Set.of(first.id),
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Custom Field", "cf." + dateField, "2026-04-01")));
        Assertions.assertEquals(Set.of(second.id),
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV Custom Field", "cf." + dateField + ".from", "2026-04-02")));
        Assertions.assertEquals(Set.of(first.id), searchAllIds(SUPPORT_PATH,
                Map.of("q", "ADV Custom Field", "cf." + textField, "hello", "cf." + selectField, "One")));
    }

    @Test
    @TestSecurity(user = SUPPORT_EMAIL, roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPPORT_EMAIL), @Claim(key = "sub", value = "adv-support") })
    void malformedSearchValuesReturnBadRequest() {
        ensureUser("adv-support", SUPPORT_EMAIL, User.TYPE_SUPPORT);
        Long inactiveField = seedCustomField("ADV CF Inactive", CustomField.TYPE_TEXT, List.of());
        deactivateDefinitionDirect(inactiveField);
        Long numberField = seedCustomField("ADV CF Invalid Number", CustomField.TYPE_NUMBER, List.of());
        Long dateField = seedCustomField("ADV CF Invalid Date", CustomField.TYPE_DATE, List.of());
        Long selectField = seedCustomField("ADV CF Invalid Select", CustomField.TYPE_SELECT, List.of("One", "Two"));
        Long textField = seedCustomField("ADV CF Invalid Text", CustomField.TYPE_TEXT, List.of());

        badRequest(Map.of("activityFrom", "not-a-date"));
        badRequest(Map.of("activityFrom", "2026-02-02", "activityTo", "2026-02-01"));
        badRequest(Map.of("createdFrom", "2026-02-02", "createdTo", "2026-02-01"));
        badRequest(Map.of("resolvedFrom", "2026-02-02", "resolvedTo", "2026-02-01"));
        badRequest(Map.of("categoryId", "abc"));
        badRequest(Map.of("companyId", "abc"));
        badRequest(Map.of("requesterId", "abc"));
        badRequest(Map.of("supportUserId", "abc"));
        badRequest(Map.of("tamUserId", "abc"));
        badRequest(Map.of("cf.abc", "value"));
        badRequest(Map.of("cf.999999999", "value"));
        badRequest(Map.of("cf." + inactiveField, "value"));
        badRequest(Map.of("cf." + numberField, "not-a-number"));
        badRequest(Map.of("cf." + numberField + ".from", "20", "cf." + numberField + ".to", "10"));
        badRequest(Map.of("cf." + dateField, "2026-13-40"));
        badRequest(Map.of("cf." + dateField + ".from", "2026-05-02", "cf." + dateField + ".to", "2026-05-01"));
        badRequest(Map.of("cf." + selectField, "Three"));
        badRequest(Map.of("cf." + textField + ".from", "a"));
        badRequest(Map.of("cf." + textField, "a", "cf." + textField + ".to", "b"));
    }

    @Test
    @TestSecurity(user = SUPPORT_EMAIL, roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPPORT_EMAIL), @Claim(key = "sub", value = "adv-support") })
    void supportPaginationAndSorting() {
        ensureUser("adv-support", SUPPORT_EMAIL, User.TYPE_SUPPORT);
        Long companyId = seedCompany("ADV Page Co");
        EntitlementData entry = seedEntitlement(companyId, "ADV Page Entitlement", "Normal");
        Ticket a = seedTicket(companyId, entry.id, "ADV Page A", "Assigned", USER_EMAIL, SUPPORT_EMAIL, null, null,
                entry.version100, null, null);
        Ticket b = seedTicket(companyId, entry.id, "ADV Page B", "Assigned", USER_EMAIL, SUPPORT_EMAIL, null, null,
                entry.version100, null, null);
        Ticket c = seedTicket(companyId, entry.id, "ADV Page C", "Assigned", USER_EMAIL, SUPPORT_EMAIL, null, null,
                entry.version100, null, null);

        Response firstPage = RestAssured.given().queryParam("q", "ADV Page").queryParam("sort", "title")
                .queryParam("dir", "asc").queryParam("page", 1).queryParam("pageSize", 2).get(SUPPORT_PATH).then()
                .statusCode(200).body("totalItems", Matchers.equalTo(3)).body("totalPages", Matchers.equalTo(2))
                .extract().response();
        Assertions.assertEquals(List.of(a.id.intValue(), b.id.intValue()), firstPage.jsonPath().getList("items.id"));

        Response secondPage = RestAssured.given().queryParam("q", "ADV Page").queryParam("sort", "title")
                .queryParam("dir", "asc").queryParam("page", 2).queryParam("pageSize", 2).get(SUPPORT_PATH).then()
                .statusCode(200).extract().response();
        Assertions.assertEquals(List.of(c.id.intValue()), secondPage.jsonPath().getList("items.id"));

        Response descending = RestAssured.given().queryParam("q", "ADV Page").queryParam("sort", "title")
                .queryParam("dir", "desc").queryParam("page", 1).queryParam("pageSize", 2).get(SUPPORT_PATH).then()
                .statusCode(200).extract().response();
        Assertions.assertEquals(List.of(c.id.intValue(), b.id.intValue()), descending.jsonPath().getList("items.id"));
    }

    @Test
    @TestSecurity(user = USER_EMAIL, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = USER_EMAIL), @Claim(key = "sub", value = "adv-user") })
    void userCannotMatchInternalMessageBodies() {
        ensureUser("adv-user", USER_EMAIL, User.TYPE_USER);
        Long companyId = seedCompany("ADV Message Co", USER_EMAIL);
        EntitlementData entry = seedEntitlement(companyId, "ADV Message Entitlement", "Normal");
        Ticket ticket = seedTicket(companyId, entry.id, "ADV Message Ticket", "Assigned", USER_EMAIL, SUPPORT_EMAIL,
                null, null, entry.version100, null, null);
        seedMessage(ticket.id, "ADV PUBLIC BODY", SUPPORT_EMAIL, LocalDateTime.now(), true);
        seedMessage(ticket.id, "ADV INTERNAL BODY", SUPPORT_EMAIL, LocalDateTime.now().plusSeconds(1), false);

        Assertions.assertEquals(Set.of(ticket.id), searchAllIds(USER_PATH, Map.of("q", "ADV PUBLIC BODY")));
        Assertions.assertTrue(searchAllIds(USER_PATH, Map.of("q", "ADV INTERNAL BODY")).isEmpty());
    }

    @Test
    @TestSecurity(user = SUPPORT_EMAIL, roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPPORT_EMAIL), @Claim(key = "sub", value = "adv-support") })
    void supportCanMatchInternalMessageBodies() {
        ensureUser("adv-support", SUPPORT_EMAIL, User.TYPE_SUPPORT);
        Long companyId = seedCompany("ADV Message Support Co");
        EntitlementData entry = seedEntitlement(companyId, "ADV Message Support Entitlement", "Normal");
        Ticket ticket = seedTicket(companyId, entry.id, "ADV Message Support Ticket", "Assigned", USER_EMAIL,
                SUPPORT_EMAIL, null, null, entry.version100, null, null);
        seedMessage(ticket.id, "ADV SUPPORT INTERNAL BODY", SUPPORT_EMAIL, LocalDateTime.now(), false);

        Assertions.assertEquals(Set.of(ticket.id),
                searchAllIds(SUPPORT_PATH, Map.of("q", "ADV SUPPORT INTERNAL BODY")));
    }

    @Test
    @TestSecurity(user = ADMIN_EMAIL, roles = "admin")
    @JwtSecurity(claims = { @Claim(key = "email", value = ADMIN_EMAIL), @Claim(key = "sub", value = "adv-admin") })
    void adminCannotUseTicketSearch() {
        ensureUser("adv-admin", ADMIN_EMAIL, User.TYPE_ADMIN);
        RestAssured.given().get(SUPPORT_PATH).then().statusCode(403);
        RestAssured.given().get(SUPERUSER_PATH).then().statusCode(403);
        RestAssured.given().get(USER_PATH).then().statusCode(403);
    }

    @Test
    @TestSecurity(user = SUPPORT_EMAIL, roles = "support")
    @JwtSecurity(claims = { @Claim(key = "email", value = SUPPORT_EMAIL), @Claim(key = "sub", value = "adv-support") })
    void simpleListAndSearchRemainUnchanged() {
        ensureUser("adv-support", SUPPORT_EMAIL, User.TYPE_SUPPORT);
        Long companyId = seedCompany("ADV Regression Co");
        EntitlementData entry = seedEntitlement(companyId, "ADV Regression Entitlement", "Normal");
        Ticket ticket = seedTicket(companyId, entry.id, "ADV Regression Ticket", "Assigned", USER_EMAIL, SUPPORT_EMAIL,
                null, null, entry.version100, null, null);

        RestAssured.given().queryParam("q", ticket.name).get("/api/support/tickets").then().statusCode(200)
                .body("searchTerm", Matchers.equalTo(ticket.name))
                .body("items.id", Matchers.hasItem(ticket.id.intValue()));
        RestAssured.given().queryParam("q", ticket.name).get("/api/support/tickets/suggest").then().statusCode(200)
                .body("items.id", Matchers.hasItem(ticket.id.intValue()));
    }

    private void badRequest(Map<String, Object> parameters) {
        RestAssured.given().queryParams(parameters).get(SUPPORT_PATH).then().statusCode(400);
    }

    private Set<Long> searchAllIds(String path, Map<String, Object> parameters) {
        Set<Long> ids = new LinkedHashSet<>();
        int page = 1;
        int totalPages;
        do {
            Response response = RestAssured.given().queryParams(parameters).queryParam("page", page)
                    .queryParam("pageSize", 100).get(path).then().statusCode(200).extract().response();
            for (Integer id : response.jsonPath().getList("items.id", Integer.class)) {
                ids.add(id.longValue());
            }
            totalPages = response.jsonPath().getInt("totalPages");
            page++;
        } while (page <= totalPages);
        return ids;
    }

    private Set<Long> unionOfListViews(String path) {
        Set<Long> ids = new LinkedHashSet<>();
        for (String view : List.of("assigned", "open", "closed")) {
            int page = 1;
            int totalPages;
            do {
                Response response = RestAssured.given().queryParam("view", view).queryParam("page", page)
                        .queryParam("pageSize", 100).get(path).then().statusCode(200).extract().response();
                for (Integer id : response.jsonPath().getList("items.id", Integer.class)) {
                    ids.add(id.longValue());
                }
                totalPages = response.jsonPath().getInt("totalPages");
                page++;
            } while (page <= totalPages);
        }
        return ids;
    }

    private Map<String, Set<Long>> listStatusIds(String path) {
        Map<String, Set<Long>> statuses = new LinkedHashMap<>();
        for (String view : List.of("assigned", "open", "closed")) {
            int page = 1;
            int totalPages;
            do {
                Response response = RestAssured.given().queryParam("view", view).queryParam("page", page)
                        .queryParam("pageSize", 100).get(path).then().statusCode(200).extract().response();
                List<Map<String, Object>> items = response.jsonPath().getList("items");
                for (Map<String, Object> item : items) {
                    Object id = item.get("id");
                    Object status = item.get("status");
                    if (id instanceof Number number && status != null) {
                        statuses.computeIfAbsent(status.toString(), ignored -> new LinkedHashSet<>())
                                .add(number.longValue());
                    }
                }
                totalPages = response.jsonPath().getInt("totalPages");
                page++;
            } while (page <= totalPages);
        }
        return statuses;
    }

    private void assertStatusFiltersMatchList(String searchPath, String listPath) {
        for (Map.Entry<String, Set<Long>> entry : listStatusIds(listPath).entrySet()) {
            Assertions.assertEquals(entry.getValue(), searchAllIds(searchPath, Map.of("status", entry.getKey())),
                    "Status filter must match the list for " + entry.getKey());
        }
    }

    private Long userId(String email) {
        User user = User.find("email", email).firstResult();
        return user == null ? null : user.id;
    }

    @Transactional
    Long seedCompany(String name, String... memberEmails) {
        Company company = Company.find("name", name).firstResult();
        if (company == null) {
            company = new Company();
            company.name = name;
            company.persist();
        }
        for (String email : memberEmails) {
            User user = User.find("email", email).firstResult();
            if (user != null && company.users.stream().noneMatch(existing -> existing.id.equals(user.id))) {
                company.users.add(user);
            }
        }
        return company.id;
    }

    @Transactional
    EntitlementData seedEntitlement(Long companyId, String entitlementName, String levelName) {
        Company company = Company.findById(companyId);
        Entitlement entitlement = ensureEntitlement(entitlementName, "Advanced search test");
        Level level = ensureLevel(levelName, levelName, 1, "#123456");
        CompanyEntitlement entry = ensureCompanyEntitlement(company, entitlement, level);
        Version version100 = ensureVersion(entitlement, "1.0.0");
        Version version101 = ensureVersion(entitlement, "1.0.1");
        return new EntitlementData(entry.id, entitlement.id, level.id, version100.id, version101.id, level.name);
    }

    @Transactional
    Ticket seedTicket(Long companyId, Long companyEntitlementId, String title, String status, String requesterEmail,
            String supportEmail, String tamEmail, Long categoryId, Long affectsVersionId, Long resolvedVersionId,
            LocalDateTime resolvedAt) {
        Company company = Company.findById(companyId);
        Ticket ticket = new Ticket();
        ticket.name = Ticket.nextName(company);
        ticket.title = title;
        ticket.status = status;
        ticket.company = company;
        ticket.requester = requesterEmail == null ? null : User.find("email", requesterEmail).firstResult();
        ticket.companyEntitlement = companyEntitlementId == null ? null
                : CompanyEntitlement.findById(companyEntitlementId);
        ticket.category = categoryId == null ? null : Category.findById(categoryId);
        ticket.affectsVersion = affectsVersionId == null ? null : Version.findById(affectsVersionId);
        ticket.resolvedVersion = resolvedVersionId == null ? null : Version.findById(resolvedVersionId);
        ticket.resolvedAt = resolvedAt;
        if (supportEmail != null) {
            User support = User.find("email", supportEmail).firstResult();
            if (support != null) {
                ticket.supportUsers.add(support);
            }
        }
        if (tamEmail != null) {
            User tam = User.find("email", tamEmail).firstResult();
            if (tam != null) {
                ticket.tamUsers.add(tam);
            }
        }
        ticket.persist();
        return ticket;
    }

    @Transactional
    void assignSupport(Long ticketId, String supportEmail) {
        Ticket ticket = Ticket.findById(ticketId);
        User support = User.find("email", supportEmail).firstResult();
        if (support != null && ticket.supportUsers.stream().noneMatch(existing -> existing.id.equals(support.id))) {
            ticket.supportUsers.add(support);
        }
    }

    @Transactional
    void seedMessage(Long ticketId, String body, String authorEmail, LocalDateTime date, boolean isPublic) {
        Message message = new Message();
        message.ticket = Ticket.findById(ticketId);
        message.body = body;
        message.author = User.find("email", authorEmail).firstResult();
        message.date = date;
        message.isPublic = isPublic;
        message.persist();
    }

    @Transactional
    void insertOpenedEvent(Long ticketId, Long companyId, LocalDateTime createdAt) {
        Panache.getEntityManager()
                .createNativeQuery(
                        "insert into events (event_type, created_at, event_key, company_id) values (?1, ?2, ?3, ?4)")
                .setParameter(1, EventConstants.TICKET_OPENED).setParameter(2, createdAt).setParameter(3, ticketId)
                .setParameter(4, companyId).executeUpdate();
    }

    @Transactional
    Long seedCustomField(String name, String type, List<String> options) {
        CustomField field = new CustomField();
        field.name = name;
        field.nameKey = TicketCustomFieldService.normalizeNameKey(name);
        field.fieldType = type;
        field.required = false;
        field.active = true;
        field.sortOrder = 0;
        field.persist();
        int index = 0;
        for (String label : options) {
            CustomFieldOption option = new CustomFieldOption();
            option.customField = field;
            option.label = label;
            option.sortOrder = index++;
            option.persist();
            field.options.add(option);
        }
        ticketCustomFieldService.invalidateDefinitions();
        return field.id;
    }

    @Transactional
    void setCustomValue(Long ticketId, Long fieldId, String text, BigDecimal number, LocalDate date) {
        Ticket ticket = Ticket.findById(ticketId);
        CustomField field = CustomField.findById(fieldId);
        TicketCustomFieldValue value = new TicketCustomFieldValue();
        value.ticket = ticket;
        value.customField = field;
        value.textValue = text;
        value.numberValue = number;
        value.dateValue = date;
        value.persist();
        ticket.customFieldValues.add(value);
    }

    @Transactional
    void deactivateDefinitionDirect(Long fieldId) {
        CustomField field = CustomField.findById(fieldId);
        if (field != null) {
            field.active = false;
        }
        ticketCustomFieldService.invalidateDefinitions();
    }

    private FilterData seedFilterData() {
        Long companyA = seedCompany("ADV Filter Co A");
        Long companyB = seedCompany("ADV Filter Co B");
        EntitlementData entitlementA = seedEntitlement(companyA, "ADV Filter Entitlement A", "ADV Filter Level A");
        EntitlementData entitlementB = seedEntitlement(companyB, "ADV Filter Entitlement B", "ADV Filter Level B");
        Category categoryAlpha = ensureCategory("ADV Filter Category Alpha", "Advanced search", false);
        Category categoryBeta = ensureCategory("ADV Filter Category Beta", "Advanced search", false);

        Ticket alpha = seedTicket(companyA, entitlementA.id, "ADV Filter Alpha", "Assigned", USER_EMAIL, SUPPORT_EMAIL,
                TAM_EMAIL, categoryAlpha.id, entitlementA.version100, entitlementA.version101, null);
        Ticket beta = seedTicket(companyA, entitlementA.id, "ADV Filter Beta", "Open", USER_EMAIL, SUPPORT_EMAIL,
                TAM2_ROLE_EMAIL, categoryBeta.id, entitlementA.version100, null, null);
        Ticket gamma = seedTicket(companyA, entitlementB.id, "ADV Filter Gamma", "Closed", USER_EMAIL, SUPPORT_EMAIL,
                null, categoryAlpha.id, entitlementB.version101, null, LocalDateTime.now());
        Ticket delta = seedTicket(companyB, entitlementB.id, "ADV Filter Delta", "Assigned", USER_EMAIL, SUPPORT_EMAIL,
                null, null, entitlementB.version100, null, null);
        return new FilterData(alpha.id, beta.id, gamma.id, delta.id, companyA, entitlementA.id, entitlementB.id,
                entitlementA.entitlementId, entitlementA.levelId, entitlementB.levelId, entitlementA.version100,
                entitlementA.version101, categoryAlpha.id);
    }

    private static final class EntitlementData {
        private final Long id;
        private final Long entitlementId;
        private final Long levelId;
        private final Long version100;
        private final Long version101;
        private final String levelName;

        private EntitlementData(Long id, Long entitlementId, Long levelId, Long version100, Long version101,
                String levelName) {
            this.id = id;
            this.entitlementId = entitlementId;
            this.levelId = levelId;
            this.version100 = version100;
            this.version101 = version101;
            this.levelName = levelName;
        }
    }

    private static final class FilterData {
        private final Long alpha;
        private final Long beta;
        private final Long gamma;
        private final Long delta;
        private final Long companyA;
        private final Long entitlementAlpha;
        private final Long entitlementBeta;
        private final Long entitlementAId;
        private final Long levelAId;
        private final Long levelBId;
        private final Long version100;
        private final Long version101;
        private final Long categoryAlpha;

        private FilterData(Long alpha, Long beta, Long gamma, Long delta, Long companyA, Long entitlementAlpha,
                Long entitlementBeta, Long entitlementAId, Long levelAId, Long levelBId, Long version100,
                Long version101, Long categoryAlpha) {
            this.alpha = alpha;
            this.beta = beta;
            this.gamma = gamma;
            this.delta = delta;
            this.companyA = companyA;
            this.entitlementAlpha = entitlementAlpha;
            this.entitlementBeta = entitlementBeta;
            this.entitlementAId = entitlementAId;
            this.levelAId = levelAId;
            this.levelBId = levelBId;
            this.version100 = version100;
            this.version101 = version101;
            this.categoryAlpha = categoryAlpha;
        }
    }
}
