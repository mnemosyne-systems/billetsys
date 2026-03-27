package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/tam/users")
@Produces(MediaType.APPLICATION_JSON)
public class TamUserApiResource {

    @GET
    @Transactional
    public UserDirectoryApiModels.DirectoryListResponse list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("companyId") Long companyId) {
        User currentUser = requireTam(auth);
        List<Company> companies = Company
                .find("select distinct c from Company c join c.users u where u = ?1 order by c.name", currentUser)
                .list();
        Company selectedCompany = selectCompany(companies, companyId);
        List<User> users = selectedCompany == null ? List.of()
                : Company.<User> find("select u from Company c join c.users u where c = ?1 order by u.name",
                        selectedCompany).list();
        String createPath = selectedCompany != null ? "/tam/users/new?companyId=" + selectedCompany.id
                : "/tam/users/new";
        return new UserDirectoryApiModels.DirectoryListResponse("Users", "",
                selectedCompany == null ? null : selectedCompany.id, false, companies.size() <= 1, createPath,
                companies.stream().map(UserDirectoryApiModels::companyOption).toList(),
                users.stream()
                        .map(user -> UserDirectoryApiModels.userReference(user, "/user/user-profiles/" + user.id, null))
                        .toList());
    }

    @GET
    @Path("/bootstrap")
    @Transactional
    public UserDirectoryApiModels.UserFormResponse bootstrap(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("companyId") Long companyId, @QueryParam("countryId") Long countryId) {
        User currentUser = requireTam(auth);
        List<Company> companies = Company
                .find("select distinct c from Company c join c.users u where u = ?1 order by c.name", currentUser)
                .list();
        Company selectedCompany = selectCompany(companies, companyId);
        if (selectedCompany == null) {
            throw new NotFoundException();
        }
        User newUser = new User();
        newUser.type = User.TYPE_USER;
        Country selectedCountry = selectCountry(countryId);
        newUser.country = selectedCountry;
        newUser.timezone = selectedCountry == null ? null
                : Timezone.find("country = ?1 and name = ?2", selectedCountry, "America/New_York").firstResult();
        List<Country> countries = Country.list("order by name");
        List<Timezone> timezones = selectedCountry == null ? List.of()
                : Timezone.list("country = ?1 order by name", selectedCountry);
        return new UserDirectoryApiModels.UserFormResponse("New user", "/tam/users",
                "/tam/users?companyId=" + selectedCompany.id, selectedCompany.id, companies.size() <= 1, true,
                companies.stream().map(UserDirectoryApiModels::companyOption).toList(),
                countries.stream().map(UserDirectoryApiModels::countryOption).toList(),
                timezones.stream().map(UserDirectoryApiModels::timezoneOption).toList(),
                List.of(new UserDirectoryApiModels.TypeOption(User.TYPE_USER, "User")),
                UserDirectoryApiModels.userFormData(newUser, selectedCompany.id));
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

    private Country selectCountry(Long countryId) {
        if (countryId != null) {
            Country country = Country.findById(countryId);
            if (country != null) {
                return country;
            }
        }
        return Country.find("code", "US").firstResult();
    }

    private User requireTam(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isUser(user) || !User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        return user;
    }
}
