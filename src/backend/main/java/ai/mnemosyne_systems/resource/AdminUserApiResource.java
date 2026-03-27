package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/admin/users")
@Produces(MediaType.APPLICATION_JSON)
public class AdminUserApiResource {

    @GET
    @Transactional
    public UserDirectoryApiModels.DirectoryListResponse list(@CookieParam("authUserIdV3") String auth,
            @QueryParam("companyId") Long companyId) {
        OwnerResource.requireAdmin(auth);
        List<Company> companies = Company.list("order by name");
        Company selectedCompany = selectCompany(companies, companyId);
        List<User> users = selectedCompany == null ? List.of()
                : Company.<User> find("select u from Company c join c.users u where c = ?1 order by u.name",
                        selectedCompany).list();
        String createPath = selectedCompany != null ? "/users/new?companyId=" + selectedCompany.id : "/users/new";
        return new UserDirectoryApiModels.DirectoryListResponse("Users", "",
                selectedCompany == null ? null : selectedCompany.id, true, false, createPath,
                companies.stream().map(UserDirectoryApiModels::companyOption).toList(),
                users.stream().map(user -> UserDirectoryApiModels.userReference(user, "/users/" + user.id,
                        "/users/" + user.id + "/edit")).toList());
    }

    @GET
    @Path("/bootstrap")
    @Transactional
    public UserDirectoryApiModels.UserFormResponse bootstrap(@CookieParam("authUserIdV3") String auth,
            @QueryParam("userId") Long userId, @QueryParam("companyId") Long companyId,
            @QueryParam("countryId") Long countryId) {
        OwnerResource.requireAdmin(auth);
        List<Company> companies = Company.list("order by name");
        User user = userId == null ? new User() : User.findById(userId);
        if (userId != null && user == null) {
            throw new NotFoundException();
        }
        if (userId == null) {
            user.type = User.TYPE_USER;
            user.name = "";
            user.email = "";
        }
        Company userCompany = userId == null ? null
                : Company.<Company> find("select c from Company c join c.users u where u = ?1", user).firstResult();
        Company selectedCompany = companyId != null ? Company.findById(companyId) : userCompany;
        if (selectedCompany == null && !companies.isEmpty()) {
            selectedCompany = companies.get(0);
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
        String submitPath = userId == null ? "/users" : "/user/" + userId;
        String cancelPath = selectedCompany != null ? "/users?companyId=" + selectedCompany.id : "/users";
        return new UserDirectoryApiModels.UserFormResponse(userId == null ? "New user" : "Edit user", submitPath,
                cancelPath, selectedCompany == null ? null : selectedCompany.id, false, userId == null,
                companies.stream().map(UserDirectoryApiModels::companyOption).toList(),
                countries.stream().map(UserDirectoryApiModels::countryOption).toList(),
                timezones.stream().map(UserDirectoryApiModels::timezoneOption).toList(),
                List.of(new UserDirectoryApiModels.TypeOption(User.TYPE_ADMIN, "Admin"),
                        new UserDirectoryApiModels.TypeOption(User.TYPE_SUPPORT, "Support"),
                        new UserDirectoryApiModels.TypeOption(User.TYPE_USER, "User"),
                        new UserDirectoryApiModels.TypeOption(User.TYPE_TAM, "TAM"),
                        new UserDirectoryApiModels.TypeOption(User.TYPE_SUPERUSER, "Superuser")),
                UserDirectoryApiModels.userFormData(user, selectedCompany == null ? null : selectedCompany.id));
    }

    @GET
    @Path("/{id}")
    @Transactional
    public UserDirectoryApiModels.UserDetailResponse detail(@CookieParam("authUserIdV3") String auth,
            @PathParam("id") Long id) {
        OwnerResource.requireAdmin(auth);
        User user = User.findById(id);
        if (user == null) {
            throw new NotFoundException();
        }
        Company company = Company.<Company> find("select c from Company c join c.users u where u = ?1", user)
                .firstResult();
        String backPath = company != null ? "/users?companyId=" + company.id : "/users";
        return new UserDirectoryApiModels.UserDetailResponse(user.id, user.name, user.getDisplayName(), user.fullName,
                user.email, user.social, user.phoneNumber, user.phoneExtension, user.type,
                UserDirectoryApiModels.typeLabel(user.type), user.country == null ? null : user.country.name,
                user.timezone == null ? null : user.timezone.name, company == null ? null : company.id,
                company == null ? null : company.name, "/users/" + user.id + "/edit", "/user/" + user.id + "/delete",
                backPath);
    }

    private Company selectCompany(List<Company> companies, Long companyId) {
        if (companies == null || companies.isEmpty()) {
            return null;
        }
        if (companyId == null) {
            return companies.get(0);
        }
        return companies.stream().filter(company -> company.id != null && company.id.equals(companyId)).findFirst()
                .orElse(companies.get(0));
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
