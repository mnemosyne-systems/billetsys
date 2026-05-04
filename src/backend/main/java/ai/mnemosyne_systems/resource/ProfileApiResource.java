package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Installation;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/profile")
@Produces(MediaType.APPLICATION_JSON)
public class ProfileApiResource {

    @GET
    @Transactional
    public ProfileResponse profile(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireUser(auth);
        return toResponse(user);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public ProfileResponse update(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, ProfileUpdateRequest request) {
        User user = requireUser(auth);
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new WebApplicationException("Username is required", Response.Status.BAD_REQUEST);
        }
        if (user.name == null || user.name.isBlank()) {
            user.name = request.name().trim();
        }
        user.email = trimOrNull(request.email());
        user.social = trimOrNull(request.social());
        user.fullName = trimOrNull(request.fullName());
        user.phoneNumber = trimOrNull(request.phoneNumber());
        user.phoneExtension = trimOrNull(request.phoneExtension());
        user.country = request.countryId() != null ? Country.findById(request.countryId()) : null;
        user.timezone = request.timezoneId() != null ? Timezone.findById(request.timezoneId()) : null;
        user.emailFormat = normalizeEmailFormat(request.emailFormat());
        user.logoBase64 = trimOrNull(request.logoBase64());

        if (AuthHelper.isAdmin(user)) {
            ensureAdminAssignedToOwnerCompany(user);
        } else if (AuthHelper.isSupport(user)) {
            reassignSupportUserCompany(user, request.companyId());
        }
        return toResponse(user);
    }

    @POST
    @Path("/password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public PasswordResponse updatePassword(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            PasswordUpdateRequest request) {
        User user = requireUser(auth);
        if (request == null || request.oldPassword() == null || request.oldPassword().isBlank()) {
            throw new WebApplicationException("Old password is required", Response.Status.BAD_REQUEST);
        }
        if (!BcryptUtil.matches(request.oldPassword(), user.passwordHash)) {
            throw new WebApplicationException("Old password is incorrect", Response.Status.BAD_REQUEST);
        }
        if (request.newPassword() == null || request.newPassword().isBlank()) {
            throw new WebApplicationException("New password is required", Response.Status.BAD_REQUEST);
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new WebApplicationException("Passwords do not match", Response.Status.BAD_REQUEST);
        }
        user.passwordHash = BcryptUtil.bcryptHash(request.newPassword());
        return new PasswordResponse(true);
    }

    private ProfileResponse toResponse(User user) {
        List<CountryOption> countries = Country.<Country> list("order by name").stream()
                .map(country -> new CountryOption(country.id, country.name)).toList();
        List<TimezoneOption> timezones = Timezone.<Timezone> list("order by name").stream()
                .map(timezone -> new TimezoneOption(timezone.id, timezone.name,
                        timezone.country == null ? null : timezone.country.id))
                .toList();
        List<Company> userCompanies = Company
                .find("select c from Company c join c.users u where u = ?1 order by c.name", user).list();
        Company currentCompany = userCompanies.isEmpty() ? null : userCompanies.get(0);
        List<CompanyOption> companies = Company.<Company> list("order by name").stream()
                .map(company -> new CompanyOption(company.id, company.name)).toList();
        return new ProfileResponse(user.type, user.name, user.getDisplayName(), user.email, user.fullName, user.social,
                user.phoneNumber, user.phoneExtension, user.country == null ? null : user.country.id,
                user.country == null ? null : user.country.name, user.timezone == null ? null : user.timezone.id,
                user.timezone == null ? null : user.timezone.name, user.logoBase64, user.emailFormat,
                currentCompany == null ? null : currentCompany.id, currentCompany == null ? null : currentCompany.name,
                companyBase(user), AuthHelper.isSupport(user), countries, timezones, companies);
    }

    private void reassignSupportUserCompany(User user, Long companyId) {
        List<Company> currentCompanies = Company.find("select c from Company c join c.users u where u = ?1", user)
                .list();
        for (Company company : currentCompanies) {
            company.users.removeIf(existing -> existing.id != null && existing.id.equals(user.id));
        }
        if (companyId == null) {
            return;
        }
        Company newCompany = Company.findById(companyId);
        if (newCompany == null) {
            throw new WebApplicationException("Company not found", Response.Status.BAD_REQUEST);
        }
        boolean exists = newCompany.users.stream()
                .anyMatch(existing -> existing.id != null && existing.id.equals(user.id));
        if (!exists) {
            newCompany.users.add(user);
        }
    }

    private void ensureAdminAssignedToOwnerCompany(User user) {
        Installation installation = Installation.find("singletonKey", "installation").firstResult();
        if (installation == null || installation.company == null) {
            return;
        }
        Company ownerCompany = installation.company;
        List<Company> currentCompanies = Company.find("select c from Company c join c.users u where u = ?1", user)
                .list();
        for (Company company : currentCompanies) {
            company.users.removeIf(existing -> existing.id != null && existing.id.equals(user.id));
        }
        boolean exists = ownerCompany.users.stream()
                .anyMatch(existing -> existing.id != null && existing.id.equals(user.id));
        if (!exists) {
            ownerCompany.users.add(user);
        }
    }

    private String companyBase(User user) {
        return AuthHelper.isSuperuser(user) ? "/superuser/companies" : "/user/companies";
    }

    private String trimOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeEmailFormat(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim().toLowerCase();
        if ("text".equals(trimmed) || "html".equals(trimmed)) {
            return trimmed;
        }
        return null;
    }

    private User requireUser(String auth) {
        User user = AuthHelper.findUser(auth);
        if (user == null) {
            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        return user;
    }

    public record ProfileResponse(String role, String username, String displayName, String email, String fullName,
            String social, String phoneNumber, String phoneExtension, Long countryId, String countryName,
            Long timezoneId, String timezoneName, String logoBase64, String emailFormat, Long currentCompanyId,
            String currentCompanyName, String companyBase, boolean canSelectCompany, List<CountryOption> countries,
            List<TimezoneOption> timezones, List<CompanyOption> companies) {
    }

    public record ProfileUpdateRequest(String name, String email, String fullName, String social, String phoneNumber,
            String phoneExtension, Long countryId, Long timezoneId, Long companyId, String logoBase64,
            String emailFormat) {
    }

    public record PasswordUpdateRequest(String oldPassword, String newPassword, String confirmPassword) {
    }

    public record PasswordResponse(boolean updated) {
    }

    public record CountryOption(Long id, String name) {
    }

    public record TimezoneOption(Long id, String name, Long countryId) {
    }

    public record CompanyOption(Long id, String name) {
    }
}
