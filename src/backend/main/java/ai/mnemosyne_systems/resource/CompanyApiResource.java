package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Entitlement;
import ai.mnemosyne_systems.model.Level;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.CookieParam;
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
public class CompanyApiResource {

    @GET
    @Transactional
    public CompanyListResponse list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireAdmin(auth);
        List<Company> companies = Company.list("lower(name) <> lower(?1) order by name", "mnemosyne systems");
        return new CompanyListResponse("/companies/new", companies.stream().map(this::toSummary).toList());
    }

    @GET
    @Path("/bootstrap")
    @Transactional
    public CompanyBootstrapResponse bootstrap(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireAdmin(auth);
        return new CompanyBootstrapResponse(optionCountries(), optionTimezones(), optionUsers(User.TYPE_USER),
                optionUsers(User.TYPE_TAM), optionEntitlements(), optionLevels(), defaultCountryId(),
                defaultTimezoneId(), LocalDate.now().toString(),
                List.of(new DurationOption(CompanyEntitlement.DURATION_MONTHLY, "Monthly"),
                        new DurationOption(CompanyEntitlement.DURATION_YEARLY, "Yearly")));
    }

    @GET
    @Path("/{id}")
    @Transactional
    public CompanyDetailResponse detail(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireAdmin(auth);
        Company company = Company.find(
                "select distinct c from Company c left join fetch c.users left join fetch c.primaryContact where c.id = ?1",
                id).firstResult();
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
        LinkedHashSet<Long> primaryContactOptionIds = new LinkedHashSet<>();
        List<UserOption> primaryContactOptions = new ArrayList<>();
        for (User user : companyUsers) {
            if (user != null && primaryContactOptionIds.add(user.id)) {
                primaryContactOptions.add(toUserOption(user));
            }
        }
        if (company.primaryContact != null && company.primaryContact.id != null
                && primaryContactOptionIds.add(company.primaryContact.id)) {
            primaryContactOptions.add(toUserOption(company.primaryContact));
        }
        CompanyBootstrapResponse bootstrap = bootstrap(auth);
        return new CompanyDetailResponse(company.id, company.name, company.address1, company.address2, company.city,
                company.state, company.zip, company.phoneNumber, company.country == null ? null : company.country.id,
                company.country == null ? null : company.country.name,
                company.timezone == null ? null : company.timezone.id,
                company.timezone == null ? null : company.timezone.name, selectedUserIds.stream().toList(),
                selectedTamIds.stream().toList(), selectedSuperuserOptions, selectedUserOptions, selectedTamOptions,
                companyEntitlements.stream().map(this::toEntitlementAssignment).toList(),
                company.primaryContact == null ? null : company.primaryContact.id,
                company.primaryContact == null ? null : company.primaryContact.name,
                company.primaryContact == null ? null : company.primaryContact.fullName,
                company.primaryContact == null ? null : company.primaryContact.email,
                company.primaryContact == null ? null : company.primaryContact.social,
                company.primaryContact == null ? null : company.primaryContact.phoneNumber,
                company.primaryContact == null ? null : company.primaryContact.phoneExtension,
                company.primaryContact == null || company.primaryContact.country == null ? null
                        : company.primaryContact.country.id,
                company.primaryContact == null || company.primaryContact.country == null ? null
                        : company.primaryContact.country.name,
                company.primaryContact == null || company.primaryContact.timezone == null ? null
                        : company.primaryContact.timezone.id,
                company.primaryContact == null || company.primaryContact.timezone == null ? null
                        : company.primaryContact.timezone.name,
                company.primaryContact == null ? null : company.primaryContact.logoBase64,
                company.primaryContact != null, primaryContactOptions, bootstrap.countries(), bootstrap.timezones(),
                bootstrap.userOptions(), bootstrap.tamOptions(), bootstrap.entitlements(), bootstrap.levels(),
                bootstrap.defaultCountryId(), bootstrap.defaultTimezoneId(), bootstrap.todayDate(),
                bootstrap.durations());
    }

    private CompanySummary toSummary(Company company) {
        long superuserCount = company.users == null ? 0
                : company.users.stream().filter(user -> User.TYPE_SUPERUSER.equalsIgnoreCase(user.type)).count();
        long userCount = company.users == null ? 0
                : company.users.stream().filter(user -> User.TYPE_USER.equalsIgnoreCase(user.type)).count();
        long tamCount = company.users == null ? 0
                : company.users.stream().filter(user -> User.TYPE_TAM.equalsIgnoreCase(user.type)).count();
        return new CompanySummary(company.id, company.name, company.country == null ? null : company.country.name,
                company.timezone == null ? null : company.timezone.name, company.phoneNumber,
                company.primaryContact == null ? null : company.primaryContact.getDisplayName(), superuserCount,
                userCount, tamCount, "/companies/" + company.id, "/companies/" + company.id + "/edit");
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

    private List<UserOption> optionUsers(String type) {
        return User.<User> list("type = ?1 order by name", type).stream().map(this::toUserOption).toList();
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

    private User requireAdmin(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isAdmin(user)) {
            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        return user;
    }

    public record CompanyListResponse(String createPath, List<CompanySummary> items) {
    }

    public record CompanySummary(Long id, String name, String countryName, String timezoneName, String phoneNumber,
            String primaryContactName, long superuserCount, long userCount, long tamCount, String detailPath,
            String editPath) {
    }

    public record CompanyBootstrapResponse(List<CountryOption> countries, List<TimezoneOption> timezones,
            List<UserOption> userOptions, List<UserOption> tamOptions, List<EntitlementOption> entitlements,
            List<LevelOption> levels, Long defaultCountryId, Long defaultTimezoneId, String todayDate,
            List<DurationOption> durations) {
    }

    public record CompanyDetailResponse(Long id, String name, String address1, String address2, String city,
            String state, String zip, String phoneNumber, Long countryId, String countryName, Long timezoneId,
            String timezoneName, List<Long> selectedUserIds, List<Long> selectedTamIds,
            List<UserOption> selectedSuperusers, List<UserOption> selectedUsers, List<UserOption> selectedTams,
            List<EntitlementAssignment> entitlementAssignments, Long primaryContactId, String primaryContactName,
            String primaryContactFullName, String primaryContactEmail, String primaryContactSocial,
            String primaryContactPhoneNumber, String primaryContactPhoneExtension, Long primaryContactCountryId,
            String primaryContactCountryName, Long primaryContactTimezoneId, String primaryContactTimezoneName,
            String primaryContactLogoBase64, boolean existingPrimaryContact, List<UserOption> primaryContactOptions,
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
