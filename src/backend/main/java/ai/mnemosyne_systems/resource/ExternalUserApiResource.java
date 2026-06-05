/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/{role}/externals")
@Produces(MediaType.APPLICATION_JSON)
public class ExternalUserApiResource {

    @GET
    @Transactional
    public UserDirectoryApiModels.DirectoryListResponse list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("role") String role, @QueryParam("companyId") Long companyId) {
        User currentUser = requireRole(auth, role);
        Company company = resolveCompanyForRole(currentUser, role, companyId);

        List<User> users = company == null ? List.of()
                : Company.<User> find(
                        "select u from Company c join c.users u where c = ?1 and u.type = ?2 order by u.fullName",
                        company, User.TYPE_EXTERNAL).list();

        String createPath = "/" + role + "/externals/new";
        return new UserDirectoryApiModels.DirectoryListResponse("External Contributors", "",
                company == null ? null : company.id, false, true, createPath,
                company == null ? List.of() : List.of(UserDirectoryApiModels.companyOption(company)),
                users.stream().map(
                        user -> UserDirectoryApiModels.userReference(user, "/" + role + "/externals/" + user.id, null))
                        .toList());
    }

    @GET
    @Path("/bootstrap")
    @Transactional
    public UserDirectoryApiModels.UserFormResponse bootstrap(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("role") String role, @QueryParam("userId") Long userId, @QueryParam("companyId") Long companyId,
            @QueryParam("countryId") Long countryId) {
        User currentUser = requireRole(auth, role);
        Company company = resolveCompanyForRole(currentUser, role, companyId);
        if (company == null && !("support".equals(role) && companyId == null)) {
            throw new NotFoundException();
        }

        List<Company> availableCompanies;
        if ("support".equals(role)) {
            availableCompanies = Company.list("order by name");
        } else {
            availableCompanies = Company.<Company> find(
                    "select distinct c from Company c join c.users u where u = ?1 order by c.name", currentUser).list();
        }

        User user = userId == null ? new User() : User.findById(userId);
        if (userId != null && (user == null || !User.TYPE_EXTERNAL.equals(user.type))) {
            throw new NotFoundException();
        }
        if (userId == null) {
            user.type = User.TYPE_EXTERNAL;
        }

        Country selectedCountry = selectCountry(user, countryId);
        if (user.country == null) {
            user.country = selectedCountry;
        }
        if (user.timezone == null && selectedCountry != null) {
            user.timezone = Timezone.find("country = ?1 and name = ?2", selectedCountry, "America/New_York")
                    .firstResult();
        }

        List<Country> countries = Country.list("order by name");
        List<Timezone> timezones = selectedCountry == null ? List.of()
                : Timezone.list("country = ?1 order by name", selectedCountry);

        String submitPath = userId == null ? "/" + role + "/externals" : "/" + role + "/externals/" + userId;
        String title = userId == null ? "External Contributor" : "Edit External Contributor";

        return new UserDirectoryApiModels.UserFormResponse(title, submitPath, "/" + role + "/externals",
                company == null ? null : company.id, true, false,
                "support".equals(role) ? UserDirectoryApiModels.prependUnassignedCompanyOption(availableCompanies)
                        : availableCompanies.stream().map(UserDirectoryApiModels::companyOption).toList(),
                countries.stream().map(UserDirectoryApiModels::countryOption).toList(),
                timezones.stream().map(UserDirectoryApiModels::timezoneOption).toList(),
                List.of(new UserDirectoryApiModels.TypeOption(User.TYPE_EXTERNAL, "External")),
                UserDirectoryApiModels.userFormData(user, company.id));
    }

    @GET
    @Path("/{id}")
    @Transactional
    public UserDirectoryApiModels.UserDetailResponse detail(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("role") String role, @PathParam("id") Long id) {
        User currentUser = requireRole(auth, role);

        User user = User.findById(id);
        if (user == null || !User.TYPE_EXTERNAL.equals(user.type)) {
            throw new NotFoundException();
        }

        Company userCompany = Company.<Company> find("select c from Company c join c.users u where u = ?1", user)
                .firstResult();
        Company roleCompany = resolveCompanyForRole(currentUser, role, userCompany == null ? null : userCompany.id);

        if (userCompany == null || roleCompany == null || !userCompany.id.equals(roleCompany.id)) {
            throw new NotFoundException();
        }

        return new UserDirectoryApiModels.UserDetailResponse(user.id, user.name, user.getDisplayName(), user.fullName,
                user.email, user.social, user.phoneNumber, user.phoneExtension, user.type, "External",
                user.country == null ? null : user.country.name, user.timezone == null ? null : user.timezone.name,
                user.logoBase64, userCompany.id, userCompany.name, null, "/" + role + "/externals/" + user.id + "/edit",
                "/" + role + "/externals/" + user.id + "/delete", "/" + role + "/externals");
    }

    private User requireRole(String auth, String role) {
        User user = AuthHelper.findUser(auth);
        if (user == null) {
            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        if ("support".equals(role) && AuthHelper.isSupport(user))
            return user;
        if ("tam".equals(role) && AuthHelper.isTam(user))
            return user;
        if ("superuser".equals(role) && AuthHelper.isSuperuser(user))
            return user;
        if ("user".equals(role) && AuthHelper.isUser(user))
            return user;

        throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
    }

    private Company resolveCompanyForRole(User user, String role, Long requestedCompanyId) {
        if ("support".equals(role)) {
            if (requestedCompanyId != null) {
                return Company.findById(requestedCompanyId);
            }
            return null; // Prompt for selection or default
        }
        if ("tam".equals(role) || "superuser".equals(role) || "user".equals(role)) {
            List<Company> companies = Company.<Company> find(
                    "select distinct c from Company c join c.users u where u = ?1 order by c.name", user).list();
            if (companies.isEmpty()) {
                return null;
            }
            if (requestedCompanyId != null) {
                return companies.stream().filter(c -> c.id != null && c.id.equals(requestedCompanyId)).findFirst()
                        .orElse(companies.get(0));
            }
            return companies.get(0);
        }
        return null;
    }

    private Country selectCountry(User user, Long countryId) {
        if (countryId != null) {
            Country country = Country.findById(countryId);
            if (country != null) {
                return country;
            }
        }
        if (user != null && user.country != null) {
            return user.country;
        }
        return Country.find("code", "US").firstResult();
    }
}
