/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.UserAvailability;
import ai.mnemosyne_systems.service.UserAvailabilityService;
import ai.mnemosyne_systems.util.AuthHelper;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class UserAvailabilityApiResourceTest {

    private static final String BASE_PATH = "/api/availability";

    @Inject
    UserAvailabilityService service;

    private final List<Long> createdAvailabilityIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdCompanyIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        QuarkusTransaction.requiringNew().run(() -> {
            for (Long id : createdAvailabilityIds) {
                UserAvailability availability = UserAvailability.findById(id);
                if (availability != null) {
                    availability.delete();
                }
            }
            for (Long id : createdUserIds) {
                User user = User.findById(id);
                if (user != null) {
                    Company.<Company> listAll().forEach(c -> c.users.removeIf(u -> u.id.equals(id)));
                    user.delete();
                }
            }
            for (Long id : createdCompanyIds) {
                Company company = Company.findById(id);
                if (company != null) {
                    company.delete();
                }
            }
        });
        createdAvailabilityIds.clear();
        createdUserIds.clear();
        createdCompanyIds.clear();
    }

    private Company createCompany() {
        Company[] holder = new Company[1];
        QuarkusTransaction.requiringNew().run(() -> {
            Company company = new Company();
            company.name = "Company-" + UUID.randomUUID();
            company.persist();
            holder[0] = company;
        });
        createdCompanyIds.add(holder[0].id);
        return holder[0];
    }

    private User createUser(String type, Company company) {
        User[] holder = new User[1];
        QuarkusTransaction.requiringNew().run(() -> {
            User user = new User();
            String unique = UUID.randomUUID().toString();
            user.name = "user-" + unique;
            user.email = unique + "@example.com";
            user.type = type;
            user.passwordHash = User.DISABLED_PASSWORD_HASH;
            user.persist();

            if (company != null) {
                Company managedCompany = Company.findById(company.id);
                managedCompany.users.add(user);
            }

            holder[0] = user;
        });
        createdUserIds.add(holder[0].id);
        return holder[0];
    }

    private String sessionCookie(User user) {
        return AuthHelper.createSessionCookieValue(user);
    }

    private UserAvailability createPersonalAvailabilityDirect(User user, Company company, LocalDate start,
            LocalDate end, String reason) {

        UserAvailability availability = service.createPersonal(user, company, start, end, reason);
        createdAvailabilityIds.add(availability.id);
        return availability;
    }

    private UserAvailability createCompanyAvailabilityDirect(User user, Company company, LocalDate start, LocalDate end,
            String reason) {

        UserAvailability availability = service.createCompany(user, company, start, end, reason);
        createdAvailabilityIds.add(availability.id);
        return availability;
    }

    private Map<String, Object> requestBody(LocalDate start, LocalDate end, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("startDate", start == null ? null : start.toString());
        body.put("endDate", end == null ? null : end.toString());
        body.put("reason", reason);
        return body;
    }

    private void assertRejected(Response response) {
        response.then().statusCode(greaterThanOrEqualTo(400));
    }

    private void trackIfCreated(Response response) {
        if (response.getStatusCode() == 201) {
            Long id = response.jsonPath().getLong("id");
            if (id != null) {
                createdAvailabilityIds.add(id);
            }
        }
    }

    @Test
    void getMyAvailability_withoutAuthCookie_returnsUnauthorized() {
        given().when().get(BASE_PATH + "/me").then().statusCode(401);
    }

    @Test
    void createPersonal_withoutAuthCookie_returnsUnauthorized() {
        given().contentType("application/json").body(requestBody(LocalDate.now(), LocalDate.now().plusDays(1), "PTO"))
                .when().post(BASE_PATH).then().statusCode(401);
    }

    @Test
    void update_byDifferentUser_isRejectedAndLeavesRecordUnchanged() {
        Company company = createCompany();
        User owner = createUser(User.TYPE_USER, company);
        User intruder = createUser(User.TYPE_USER, company);

        LocalDate originalStart = LocalDate.now().plusDays(10);
        LocalDate originalEnd = LocalDate.now().plusDays(15);
        UserAvailability availability = createPersonalAvailabilityDirect(owner, company, originalStart, originalEnd,
                "Vacation");

        Response response = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(intruder))
                .contentType("application/json")
                .body(requestBody(originalStart.plusDays(1), originalEnd.plusDays(1), "Hijacked")).when()
                .put(BASE_PATH + "/" + availability.id);

        assertRejected(response);

        UserAvailability reloaded = UserAvailability.findById(availability.id);
        assertNotNull(reloaded);
        assertEquals(originalStart, reloaded.startDate);
        assertEquals(originalEnd, reloaded.endDate);
    }

    @Test
    void delete_byDifferentUser_isRejectedAndRecordStillExists() {
        Company company = createCompany();
        User owner = createUser(User.TYPE_USER, company);
        User intruder = createUser(User.TYPE_USER, company);

        UserAvailability availability = createPersonalAvailabilityDirect(owner, company, LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15), "Vacation");

        Response response = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(intruder)).when()
                .delete(BASE_PATH + "/" + availability.id);

        assertRejected(response);
        assertNotNull(UserAvailability.findById(availability.id));
    }

    @Test
    void createCompanyWide_byRegularUser_isRejected() {
        Company company = createCompany();
        User regularUser = createUser(User.TYPE_USER, company);

        Response response = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(regularUser))
                .contentType("application/json")
                .body(requestBody(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "Company retreat")).when()
                .post(BASE_PATH + "/company");

        assertRejected(response);
        trackIfCreated(response);

        List<UserAvailability> companyAvailability = service.getForCompany(company);
        assertTrue(companyAvailability.isEmpty(), "No company availability should have been created");
    }

    @Test
    void createCompanyWide_bySupport_isAllowed() {
        Company company = createCompany();
        User supportUser = createUser(User.TYPE_SUPPORT, company);

        Response response = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(supportUser))
                .contentType("application/json")
                .body(requestBody(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "Company retreat")).when()
                .post(BASE_PATH + "/company");

        response.then().statusCode(201);
        trackIfCreated(response);

        List<UserAvailability> companyAvailability = service.getForCompany(company);
        assertEquals(1, companyAvailability.size());
    }

    @Test
    void createPersonal_withValidData_returns201AndPersists() {
        Company company = createCompany();
        User user = createUser(User.TYPE_USER, company);

        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = LocalDate.now().plusDays(8);

        Response response = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(user)).contentType("application/json")
                .body(requestBody(start, end, "Vacation")).when().post(BASE_PATH);

        response.then().statusCode(201);
        trackIfCreated(response);

        assertEquals(start.toString(), response.jsonPath().getString("startDate"));
        assertEquals(end.toString(), response.jsonPath().getString("endDate"));

        List<UserAvailability> stored = service.getForUser(user);
        assertEquals(1, stored.size());
        assertEquals("Vacation", stored.get(0).reason);
    }

    @Test
    void createPersonal_whenUserHasNoCompany_returns400() {
        User userWithoutCompany = createUser(User.TYPE_USER, null);

        given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(userWithoutCompany)).contentType("application/json")
                .body(requestBody(LocalDate.now(), LocalDate.now().plusDays(1), "PTO")).when().post(BASE_PATH).then()
                .statusCode(400);
    }

    @Test
    void update_byOwner_changesDates() {
        Company company = createCompany();
        User user = createUser(User.TYPE_USER, company);

        UserAvailability availability = createPersonalAvailabilityDirect(user, company, LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15), "Vacation");

        LocalDate newStart = LocalDate.now().plusDays(12);
        LocalDate newEnd = LocalDate.now().plusDays(18);

        given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(user)).contentType("application/json")
                .body(requestBody(newStart, newEnd, "Extended vacation")).when().put(BASE_PATH + "/" + availability.id)
                .then().statusCode(200);

        UserAvailability reloaded = UserAvailability.findById(availability.id);
        assertEquals(newStart, reloaded.startDate);
        assertEquals(newEnd, reloaded.endDate);
        assertEquals("Extended vacation", reloaded.reason);
    }

    @Test
    void delete_byOwner_returns204AndRemovesRecord() {
        Company company = createCompany();
        User user = createUser(User.TYPE_USER, company);

        UserAvailability availability = createPersonalAvailabilityDirect(user, company, LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15), "Vacation");

        given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(user)).when().delete(BASE_PATH + "/" + availability.id)
                .then().statusCode(204);

        assertEquals(null, UserAvailability.findById(availability.id));
        createdAvailabilityIds.remove(availability.id);
    }

    @Test
    void createPersonal_overlappingExistingRange_isRejected() {
        Company company = createCompany();
        User user = createUser(User.TYPE_USER, company);

        LocalDate existingStart = LocalDate.now().plusDays(10);
        LocalDate existingEnd = LocalDate.now().plusDays(15);
        createPersonalAvailabilityDirect(user, company, existingStart, existingEnd, "Vacation");

        Response response = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(user)).contentType("application/json")
                .body(requestBody(existingStart.plusDays(3), existingEnd.plusDays(3), "Second trip")).when()
                .post(BASE_PATH);

        assertRejected(response);
        trackIfCreated(response);

        List<UserAvailability> stored = service.getForUser(user);
        assertEquals(1, stored.size(), "Only the original availability should exist");
    }

    @Test
    void createPersonal_nonOverlappingRanges_bothSucceed() {
        Company company = createCompany();
        User user = createUser(User.TYPE_USER, company);

        LocalDate existingStart = LocalDate.now().plusDays(10);
        LocalDate existingEnd = LocalDate.now().plusDays(15);
        createPersonalAvailabilityDirect(user, company, existingStart, existingEnd, "Vacation");

        Response before = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(user)).contentType("application/json")
                .body(requestBody(existingStart.minusDays(9), existingStart.minusDays(1), "Earlier trip")).when()
                .post(BASE_PATH);
        before.then().statusCode(201);
        trackIfCreated(before);

        Response after = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(user)).contentType("application/json")
                .body(requestBody(existingEnd.plusDays(1), existingEnd.plusDays(5), "Later trip")).when()
                .post(BASE_PATH);
        after.then().statusCode(201);
        trackIfCreated(after);

        List<UserAvailability> stored = service.getForUser(user);
        assertEquals(3, stored.size());
    }

    @Test
    void updatePersonal_toOverlapAnotherRange_isRejected() {
        Company company = createCompany();
        User user = createUser(User.TYPE_USER, company);

        UserAvailability first = createPersonalAvailabilityDirect(user, company, LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(5), "First trip");
        UserAvailability second = createPersonalAvailabilityDirect(user, company, LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15), "Second trip");

        Response response = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(user)).contentType("application/json")
                .body(requestBody(LocalDate.now().plusDays(1), LocalDate.now().plusDays(12), "Stretched")).when()
                .put(BASE_PATH + "/" + first.id);

        assertRejected(response);

        UserAvailability reloaded = UserAvailability.findById(first.id);
        assertEquals(LocalDate.now().plusDays(5), reloaded.endDate, "Original range must remain unchanged");
    }

    @Test
    void createPersonal_endDateBeforeStartDate_isRejected() {
        Company company = createCompany();
        User user = createUser(User.TYPE_USER, company);

        Response response = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(user)).contentType("application/json")
                .body(requestBody(LocalDate.now().plusDays(10), LocalDate.now().plusDays(5), "Invalid")).when()
                .post(BASE_PATH);

        assertRejected(response);
        trackIfCreated(response);

        assertTrue(service.getForUser(user).isEmpty());
    }

    @Test
    void createPersonal_missingStartDate_isRejected() {
        Company company = createCompany();
        User user = createUser(User.TYPE_USER, company);

        Response response = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(user)).contentType("application/json")
                .body(requestBody(null, LocalDate.now().plusDays(5), "Missing start")).when().post(BASE_PATH);

        assertRejected(response);
        trackIfCreated(response);
    }

    @Test
    void createPersonal_missingEndDate_isRejected() {
        Company company = createCompany();
        User user = createUser(User.TYPE_USER, company);

        Response response = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(user)).contentType("application/json")
                .body(requestBody(LocalDate.now().plusDays(5), null, "Missing end")).when().post(BASE_PATH);

        assertRejected(response);
        trackIfCreated(response);
    }

    @Test
    void createPersonal_sameStartAndEndDate_isAllowed() {
        Company company = createCompany();
        User user = createUser(User.TYPE_USER, company);
        LocalDate day = LocalDate.now().plusDays(3);

        Response response = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(user)).contentType("application/json")
                .body(requestBody(day, day, "Single day off")).when().post(BASE_PATH);

        response.then().statusCode(201);
        trackIfCreated(response);
    }

    @Test
    void user_canGetOwnAvailability_returnsOwnRecordsOnly() {
        Company company = createCompany();
        User owner = createUser(User.TYPE_USER, company);
        User other = createUser(User.TYPE_USER, company);

        UserAvailability own = createPersonalAvailabilityDirect(owner, company, LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(12), "Own");
        createPersonalAvailabilityDirect(other, company, LocalDate.now().plusDays(20), LocalDate.now().plusDays(22),
                "Other");

        Response response = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(owner)).when().get(BASE_PATH + "/me");

        response.then().statusCode(200);
        assertEquals(List.of(own.id), response.jsonPath().getList("id", Long.class));
    }

    @Test
    void user_companyWideGet_isForbidden() {
        Company company = createCompany();
        User user = createUser(User.TYPE_USER, company);

        given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(user)).when().get(BASE_PATH + "/company").then()
                .statusCode(401);
    }

    @Test
    void tam_canManageOwnAvailability() {
        Company company = createCompany();
        User tam = createUser(User.TYPE_TAM, company);

        Response create = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(tam)).contentType("application/json")
                .body(requestBody(LocalDate.now().plusDays(10), LocalDate.now().plusDays(15), "TAM PTO")).when()
                .post(BASE_PATH);
        create.then().statusCode(201);
        trackIfCreated(create);
        Long id = create.jsonPath().getLong("id");

        given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(tam)).when().get(BASE_PATH + "/me").then().statusCode(200)
                .body("id", hasItem(id.intValue()));

        given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(tam)).contentType("application/json")
                .body(requestBody(LocalDate.now().plusDays(12), LocalDate.now().plusDays(16), "TAM PTO extended"))
                .when().put(BASE_PATH + "/" + id).then().statusCode(200);

        given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(tam)).when().delete(BASE_PATH + "/" + id).then()
                .statusCode(204);
        createdAvailabilityIds.remove(id);
        assertEquals(null, UserAvailability.findById(id));
    }

    @Test
    void tam_updateAnotherUsersAvailability_isForbidden() {
        Company company = createCompany();
        User owner = createUser(User.TYPE_TAM, company);
        User intruder = createUser(User.TYPE_TAM, company);

        UserAvailability availability = createPersonalAvailabilityDirect(owner, company, LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15), "Owner PTO");

        given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(intruder)).contentType("application/json")
                .body(requestBody(LocalDate.now().plusDays(11), LocalDate.now().plusDays(16), "Hijacked")).when()
                .put(BASE_PATH + "/" + availability.id).then().statusCode(401);

        UserAvailability reloaded = UserAvailability.findById(availability.id);
        assertNotNull(reloaded);
        assertEquals(LocalDate.now().plusDays(10), reloaded.startDate);
    }

    @Test
    void tam_deleteAnotherUsersAvailability_isForbidden() {
        Company company = createCompany();
        User owner = createUser(User.TYPE_TAM, company);
        User intruder = createUser(User.TYPE_TAM, company);

        UserAvailability availability = createPersonalAvailabilityDirect(owner, company, LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15), "Owner PTO");

        given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(intruder)).when().delete(BASE_PATH + "/" + availability.id)
                .then().statusCode(401);

        assertNotNull(UserAvailability.findById(availability.id));
    }

    @Test
    void tam_companyWideGet_isForbidden() {
        Company company = createCompany();
        User tam = createUser(User.TYPE_TAM, company);

        given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(tam)).when().get(BASE_PATH + "/company").then()
                .statusCode(401);
    }

    @Test
    void tam_companyWidePost_isForbidden() {
        Company company = createCompany();
        User tam = createUser(User.TYPE_TAM, company);

        Response response = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(tam)).contentType("application/json")
                .body(requestBody(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "Company retreat")).when()
                .post(BASE_PATH + "/company");

        response.then().statusCode(401);
        trackIfCreated(response);
        assertTrue(service.getForCompany(company).isEmpty());
    }

    @Test
    void support_canManageOwnAvailability() {
        Company company = createCompany();
        User support = createUser(User.TYPE_SUPPORT, company);

        Response create = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(support)).contentType("application/json")
                .body(requestBody(LocalDate.now().plusDays(10), LocalDate.now().plusDays(15), "Support PTO")).when()
                .post(BASE_PATH);
        create.then().statusCode(201);
        trackIfCreated(create);
        Long id = create.jsonPath().getLong("id");

        given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(support)).when().get(BASE_PATH + "/me").then()
                .statusCode(200).body("id", hasItem(id.intValue()));

        given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(support)).when().delete(BASE_PATH + "/" + id).then()
                .statusCode(204);
        createdAvailabilityIds.remove(id);
    }

    @Test
    void support_cannotUpdateAnotherUsersPersonalAvailability() {
        Company company = createCompany();
        User owner = createUser(User.TYPE_USER, company);
        User support = createUser(User.TYPE_SUPPORT, company);

        UserAvailability availability = createPersonalAvailabilityDirect(owner, company, LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15), "Owner PTO");

        given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(support)).contentType("application/json")
                .body(requestBody(LocalDate.now().plusDays(11), LocalDate.now().plusDays(16), "Hijacked")).when()
                .put(BASE_PATH + "/" + availability.id).then().statusCode(401);

        UserAvailability reloaded = UserAvailability.findById(availability.id);
        assertEquals(LocalDate.now().plusDays(10), reloaded.startDate);
    }

    @Test
    void support_companyWideGet_returnsOnlyOwnCompany() {
        Company companyA = createCompany();
        Company companyB = createCompany();
        User supportA = createUser(User.TYPE_SUPPORT, companyA);
        User supportB = createUser(User.TYPE_SUPPORT, companyB);

        UserAvailability ownCompany = createCompanyAvailabilityDirect(supportA, companyA, LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15), "Company A retreat");
        createCompanyAvailabilityDirect(supportB, companyB, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15),
                "Company B retreat");

        Response response = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(supportA)).when()
                .get(BASE_PATH + "/company");

        response.then().statusCode(200);
        assertEquals(List.of(ownCompany.id), response.jsonPath().getList("id", Long.class));
    }

    @Test
    void support_cannotManageAnotherCompanysCompanyAvailability() {
        Company companyA = createCompany();
        Company companyB = createCompany();
        User supportA = createUser(User.TYPE_SUPPORT, companyA);
        User supportB = createUser(User.TYPE_SUPPORT, companyB);

        UserAvailability companyAvailabilityB = createCompanyAvailabilityDirect(supportB, companyB,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(15), "Company B retreat");

        given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(supportA)).contentType("application/json")
                .body(requestBody(LocalDate.now().plusDays(11), LocalDate.now().plusDays(16), "Hijacked")).when()
                .put(BASE_PATH + "/" + companyAvailabilityB.id).then().statusCode(401);

        given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(supportA)).when()
                .delete(BASE_PATH + "/" + companyAvailabilityB.id).then().statusCode(401);

        assertNotNull(UserAvailability.findById(companyAvailabilityB.id));
    }

    @Test
    void superuser_canManageOwnAvailability() {
        Company company = createCompany();
        User superuser = createUser(User.TYPE_SUPERUSER, company);

        Response create = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(superuser))
                .contentType("application/json")
                .body(requestBody(LocalDate.now().plusDays(10), LocalDate.now().plusDays(15), "Superuser PTO")).when()
                .post(BASE_PATH);
        create.then().statusCode(201);
        trackIfCreated(create);
        Long id = create.jsonPath().getLong("id");

        given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(superuser)).when().get(BASE_PATH + "/me").then()
                .statusCode(200).body("id", hasItem(id.intValue()));

        given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(superuser)).when().delete(BASE_PATH + "/" + id).then()
                .statusCode(204);
        createdAvailabilityIds.remove(id);
    }

    @Test
    void superuser_companyWideCreateAndGetForOwnCompany_isAllowed() {
        Company companyA = createCompany();
        Company companyB = createCompany();
        User superuserA = createUser(User.TYPE_SUPERUSER, companyA);
        User superuserB = createUser(User.TYPE_SUPERUSER, companyB);

        Response create = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(superuserA))
                .contentType("application/json")
                .body(requestBody(LocalDate.now().plusDays(10), LocalDate.now().plusDays(15), "Company A shutdown"))
                .when().post(BASE_PATH + "/company");
        create.then().statusCode(201);
        trackIfCreated(create);
        Long ownCompanyId = create.jsonPath().getLong("id");

        createCompanyAvailabilityDirect(superuserB, companyB, LocalDate.now().plusDays(20),
                LocalDate.now().plusDays(25), "Company B shutdown");

        Response response = given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(superuserA)).when()
                .get(BASE_PATH + "/company");

        response.then().statusCode(200);
        assertEquals(List.of(ownCompanyId), response.jsonPath().getList("id", Long.class));
    }

    @Test
    void superuser_cannotManageAnotherCompanysCompanyAvailability() {
        Company companyA = createCompany();
        Company companyB = createCompany();
        User superuserA = createUser(User.TYPE_SUPERUSER, companyA);
        User superuserB = createUser(User.TYPE_SUPERUSER, companyB);

        UserAvailability companyAvailabilityB = createCompanyAvailabilityDirect(superuserB, companyB,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(15), "Company B shutdown");

        given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(superuserA)).contentType("application/json")
                .body(requestBody(LocalDate.now().plusDays(11), LocalDate.now().plusDays(16), "Hijacked")).when()
                .put(BASE_PATH + "/" + companyAvailabilityB.id).then().statusCode(401);

        given().cookie(AuthHelper.AUTH_COOKIE, sessionCookie(superuserA)).when()
                .delete(BASE_PATH + "/" + companyAvailabilityB.id).then().statusCode(401);

        assertNotNull(UserAvailability.findById(companyAvailabilityB.id));
    }

    @Test
    void unauthenticated_companyGet_returnsUnauthorized() {
        given().when().get(BASE_PATH + "/company").then().statusCode(401);
    }

    @Test
    void unauthenticated_companyPost_returnsUnauthorized() {
        given().contentType("application/json").body(requestBody(LocalDate.now(), LocalDate.now().plusDays(1), "PTO"))
                .when().post(BASE_PATH + "/company").then().statusCode(401);
    }

    @Test
    void unauthenticated_update_returnsUnauthorized() {
        given().contentType("application/json").body(requestBody(LocalDate.now(), LocalDate.now().plusDays(1), "PTO"))
                .when().put(BASE_PATH + "/1").then().statusCode(401);
    }

    @Test
    void unauthenticated_delete_returnsUnauthorized() {
        given().when().delete(BASE_PATH + "/1").then().statusCode(401);
    }
}
