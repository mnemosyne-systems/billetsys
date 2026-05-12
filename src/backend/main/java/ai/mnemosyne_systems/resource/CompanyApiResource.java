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
import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Entitlement;
import ai.mnemosyne_systems.model.Level;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Path("/api/companies")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("admin")
public class CompanyApiResource {

    @GET
    @Transactional
    public CompanyListResponse list() {
        List<Company> companies = Company.list("lower(name) <> lower(?1) order by name", "mnemosyne systems");
        return new CompanyListResponse("/companies/new", companies.stream().map(this::toSummary).toList());
    }

    @GET
    @Path("/bootstrap")
    @Transactional
    public CompanyBootstrapResponse bootstrap() {
        return new CompanyBootstrapResponse(optionCountries(), optionTimezones(),
                optionUsers(User.TYPE_SUPERUSER, null), optionUsers(User.TYPE_USER, null),
                optionUsers(User.TYPE_TAM, null), optionEntitlements(), optionLevels(), defaultCountryId(),
                defaultTimezoneId(), LocalDate.now().toString(),
                List.of(new DurationOption(CompanyEntitlement.DURATION_MONTHLY, "Monthly"),
                        new DurationOption(CompanyEntitlement.DURATION_YEARLY, "Yearly")));
    }

    @GET
    @Path("/{id}")
    @Transactional
    public CompanyDetailResponse detail(@PathParam("id") Long id) {
        Company company = Company.find("select distinct c from Company c left join fetch c.users where c.id = ?1", id)
                .firstResult();
        if (company == null) {
            throw new NotFoundException();
        }
        List<CompanyEntitlement> companyEntitlements = CompanyEntitlement.find(
                "select distinct ce from CompanyEntitlement ce join fetch ce.entitlement join fetch ce.supportLevel where ce.company = ?1",
                company).list();
        List<User> companyUsers = Company.find("select u from Company c join c.users u where c.id = ?1", id).list();
        List<UserOption> selectedSuperuserOptions = companyUsers.stream()
                .filter(user -> User.TYPE_SUPERUSER.equalsIgnoreCase(user.type)).map(this::toUserOption).toList();
        List<UserOption> selectedUserOptions = companyUsers.stream()
                .filter(user -> User.TYPE_USER.equalsIgnoreCase(user.type)).map(this::toUserOption).toList();
        List<UserOption> selectedTamOptions = companyUsers.stream()
                .filter(user -> User.TYPE_TAM.equalsIgnoreCase(user.type)).map(this::toUserOption).toList();
        LinkedHashSet<Long> selectedUserIds = new LinkedHashSet<>(
                selectedUserOptions.stream().map(UserOption::id).toList());
        LinkedHashSet<Long> selectedTamIds = new LinkedHashSet<>(
                selectedTamOptions.stream().map(UserOption::id).toList());
        List<UserOption> superuserOptions = optionUsers(User.TYPE_SUPERUSER, company.id);
        List<UserOption> userOptions = optionUsers(User.TYPE_USER, company.id);
        CompanyBootstrapResponse bootstrap = bootstrap();
        return new CompanyDetailResponse(company.id, company.name, company.address1, company.address2, company.city,
                company.state, company.zip, company.phoneNumber, company.country == null ? null : company.country.id,
                company.country == null ? null : company.country.name,
                company.timezone == null ? null : company.timezone.id,
                company.timezone == null ? null : company.timezone.name, selectedUserIds.stream().toList(),
                selectedTamIds.stream().toList(), selectedSuperuserOptions, selectedUserOptions, selectedTamOptions,
                companyEntitlements.stream().map(this::toEntitlementAssignment).toList(), superuserOptions,
                bootstrap.countries(), bootstrap.timezones(), userOptions, bootstrap.tamOptions(),
                bootstrap.entitlements(), bootstrap.levels(), bootstrap.defaultCountryId(),
                bootstrap.defaultTimezoneId(), bootstrap.todayDate(), bootstrap.durations());
    }

    private CompanySummary toSummary(Company company) {
        long superuserCount = company.users == null ? 0
                : company.users.stream().filter(user -> User.TYPE_SUPERUSER.equalsIgnoreCase(user.type)).count();
        long userCount = company.users == null ? 0
                : company.users.stream().filter(user -> User.TYPE_USER.equalsIgnoreCase(user.type)).count();
        long tamCount = company.users == null ? 0
                : company.users.stream().filter(user -> User.TYPE_TAM.equalsIgnoreCase(user.type)).count();
        String superuserName = company.users == null ? null
                : company.users.stream().filter(user -> User.TYPE_SUPERUSER.equalsIgnoreCase(user.type)).findFirst()
                        .map(User::getDisplayName).orElse(null);
        return new CompanySummary(company.id, company.name, company.country == null ? null : company.country.name,
                company.timezone == null ? null : company.timezone.name, company.phoneNumber, superuserName,
                superuserCount, userCount, tamCount, "/companies/" + company.id, "/companies/" + company.id + "/edit");
    }

    private EntitlementAssignment toEntitlementAssignment(CompanyEntitlement entry) {
        return new EntitlementAssignment(entry.entitlement == null ? null : entry.entitlement.id,
                entry.entitlement == null ? null : entry.entitlement.name,
                entry.supportLevel == null ? null : entry.supportLevel.id,
                entry.supportLevel == null ? null : entry.supportLevel.name,
                entry.date == null ? null : entry.date.toString(), entry.duration, isEntitlementExpired(entry));
    }

    private List<CountryOption> optionCountries() {
        return Country.<Country> list("order by name").stream()
                .map(country -> new CountryOption(country.id, country.name)).toList();
    }

    private List<TimezoneOption> optionTimezones() {
        return Timezone.<Timezone> list("order by name").stream().map(timezone -> new TimezoneOption(timezone.id,
                timezone.name, timezone.country == null ? null : timezone.country.id)).toList();
    }

    private List<UserOption> optionUsers(String type, Long companyId) {
        List<User> assignedUsers;
        if (type.equalsIgnoreCase(User.TYPE_USER) || type.equalsIgnoreCase(User.TYPE_SUPERUSER)) {
            assignedUsers = companyId == null
                    ? User.<User> find("select distinct u from Company c join c.users u where lower(u.type) = ?1",
                            type.toLowerCase()).list()
                    : User.<User> find(
                            "select distinct u from Company c join c.users u where lower(u.type) = ?1 and c.id <> ?2",
                            type.toLowerCase(), companyId).list();
        } else {
            assignedUsers = List.of();
        }
        LinkedHashSet<Long> assignedIdSet = assignedUsers.stream().map(user -> user.id)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return User.<User> list("type = ?1 order by name", type).stream()
                .filter(user -> user.id != null && !assignedIdSet.contains(user.id)).map(this::toUserOption).toList();
    }

    private UserOption toUserOption(User user) {
        return new UserOption(user.id, user.name, user.getDisplayName(), user.email);
    }

    private List<EntitlementOption> optionEntitlements() {
        return Entitlement.<Entitlement> list("order by name").stream()
                .map(entitlement -> new EntitlementOption(entitlement.id, entitlement.name)).toList();
    }

    private List<LevelOption> optionLevels() {
        return Level.<Level> list("order by level, name").stream()
                .map(level -> new LevelOption(level.id, level.name, level.level)).toList();
    }

    private Long defaultCountryId() {
        Country country = Country.find("code", "US").firstResult();
        return country == null ? null : country.id;
    }

    private Long defaultTimezoneId() {
        Timezone timezone = Timezone.find("name", "America/New_York").firstResult();
        return timezone == null ? null : timezone.id;
    }

    private boolean isEntitlementExpired(CompanyEntitlement entitlement) {
        if (entitlement == null || entitlement.date == null || entitlement.duration == null) {
            return false;
        }
        LocalDate endDate = entitlement.date;
        if (entitlement.duration == CompanyEntitlement.DURATION_MONTHLY) {
            endDate = endDate.plusMonths(1);
        } else if (entitlement.duration == CompanyEntitlement.DURATION_YEARLY) {
            endDate = endDate.plusYears(1);
        } else {
            return false;
        }
        return LocalDate.now().isAfter(endDate);
    }

    public record CompanyListResponse(String createPath, List<CompanySummary> items) {
    }

    public record CompanySummary(Long id, String name, String countryName, String timezoneName, String phoneNumber,
            String superuserName, long superuserCount, long userCount, long tamCount, String detailPath,
            String editPath) {
    }

    public record CompanyBootstrapResponse(List<CountryOption> countries, List<TimezoneOption> timezones,
            List<UserOption> superuserOptions, List<UserOption> userOptions, List<UserOption> tamOptions,
            List<EntitlementOption> entitlements, List<LevelOption> levels, Long defaultCountryId,
            Long defaultTimezoneId, String todayDate, List<DurationOption> durations) {
    }

    public record CompanyDetailResponse(Long id, String name, String address1, String address2, String city,
            String state, String zip, String phoneNumber, Long countryId, String countryName, Long timezoneId,
            String timezoneName, List<Long> selectedUserIds, List<Long> selectedTamIds,
            List<UserOption> selectedSuperusers, List<UserOption> selectedUsers, List<UserOption> selectedTams,
            List<EntitlementAssignment> entitlementAssignments, List<UserOption> superuserOptions,
            List<CountryOption> countries, List<TimezoneOption> timezones, List<UserOption> userOptions,
            List<UserOption> tamOptions, List<EntitlementOption> entitlements, List<LevelOption> levels,
            Long defaultCountryId, Long defaultTimezoneId, String todayDate, List<DurationOption> durations) {
    }

    public record CountryOption(Long id, String name) {
    }

    public record TimezoneOption(Long id, String name, Long countryId) {
    }

    public record UserOption(Long id, String username, String displayName, String email) {
    }

    public record EntitlementOption(Long id, String name) {
    }

    public record LevelOption(Long id, String name, Integer level) {
    }

    public record DurationOption(Integer value, String label) {
    }

    public record EntitlementAssignment(Long entitlementId, String entitlementName, Long levelId, String levelName,
            String date, Integer duration, boolean expired) {
    }
}
