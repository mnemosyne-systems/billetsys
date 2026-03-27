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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/superuser")
@Produces(MediaType.APPLICATION_JSON)
public class SuperuserDirectoryApiResource {

    @GET
    @Path("/users")
    @Transactional
    public UserDirectoryApiModels.DirectoryListResponse list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("companyId") Long companyId) {
        User currentUser = requireSuperuser(auth);
        List<Company> companies = Company
                .find("select distinct c from Company c join c.users u where u = ?1 order by c.name", currentUser)
                .list();
        Company selectedCompany = selectCompany(companies, companyId);
        List<User> users = selectedCompany == null ? List.of()
                : Company.<User> find("select u from Company c join c.users u where c = ?1 order by u.name",
                        selectedCompany).list();
        String createPath = selectedCompany != null ? "/superuser/users/new?companyId=" + selectedCompany.id
                : "/superuser/users/new";
        return new UserDirectoryApiModels.DirectoryListResponse("Users", "",
                selectedCompany == null ? null : selectedCompany.id, false, companies.size() <= 1, createPath,
                companies.stream().map(UserDirectoryApiModels::companyOption).toList(), users.stream()
                        .map(user -> UserDirectoryApiModels.userReference(user, detailPath(user), null)).toList());
    }

    @GET
    @Path("/users/bootstrap")
    @Transactional
    public UserDirectoryApiModels.UserFormResponse bootstrap(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("companyId") Long companyId, @QueryParam("countryId") Long countryId) {
        User currentUser = requireSuperuser(auth);
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
        return new UserDirectoryApiModels.UserFormResponse("New user", "/superuser/users",
                "/superuser/users?companyId=" + selectedCompany.id, selectedCompany.id, companies.size() <= 1, true,
                companies.stream().map(UserDirectoryApiModels::companyOption).toList(),
                countries.stream().map(UserDirectoryApiModels::countryOption).toList(),
                timezones.stream().map(UserDirectoryApiModels::timezoneOption).toList(),
                List.of(new UserDirectoryApiModels.TypeOption(User.TYPE_USER, "User")),
                UserDirectoryApiModels.userFormData(newUser, selectedCompany.id));
    }

    @GET
    @Path("/support-users/{id}")
    @Transactional
    public UserDirectoryApiModels.UserDetailResponse supportUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        User currentUser = requireSuperuser(auth);
        User user = userByType(id, User.TYPE_SUPPORT);
        return detailResponse(currentUser, user);
    }

    @GET
    @Path("/superuser-users/{id}")
    @Transactional
    public UserDirectoryApiModels.UserDetailResponse superuser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        User currentUser = requireSuperuser(auth);
        User user = userByType(id, User.TYPE_SUPERUSER);
        return detailResponse(currentUser, user);
    }

    @GET
    @Path("/user-profiles/{id}")
    @Transactional
    public UserDirectoryApiModels.UserDetailResponse profile(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        User currentUser = requireSuperuser(auth);
        User user = User.findById(id);
        if (user == null) {
            throw new NotFoundException();
        }
        return detailResponse(currentUser, user);
    }

    @GET
    @Path("/companies/{id}")
    @Transactional
    public UserDirectoryApiModels.CompanyDetailResponse company(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        User currentUser = requireSuperuser(auth);
        Company company = allowedCompany(currentUser, id);
        return new UserDirectoryApiModels.CompanyDetailResponse(company.id, company.name, company.address1,
                company.address2, company.city, company.state, company.zip, company.phoneNumber,
                company.country == null ? null : company.country.name,
                company.timezone == null ? null : company.timezone.name,
                usersForCompany(company, User.TYPE_USER, "/superuser/user-profiles/"),
                usersForCompany(company, User.TYPE_SUPERUSER, "/superuser/superuser-users/"), List.of(), List.of(),
                "/superuser/users?companyId=" + company.id);
    }

    private UserDirectoryApiModels.UserDetailResponse detailResponse(User currentUser, User user) {
        Company company = Company.<Company> find("select c from Company c join c.users u where u = ?1", user)
                .firstResult();
        boolean allowed = company != null && Company.count(
                "select count(c) from Company c join c.users current join c.users viewed where current = ?1 and viewed = ?2",
                currentUser, user) > 0;
        if (!allowed && (currentUser.id == null || !currentUser.id.equals(user.id))) {
            throw new NotFoundException();
        }
        return new UserDirectoryApiModels.UserDetailResponse(user.id, user.name, user.getDisplayName(), user.fullName,
                user.email, user.social, user.phoneNumber, user.phoneExtension, user.type,
                UserDirectoryApiModels.typeLabel(user.type), user.country == null ? null : user.country.name,
                user.timezone == null ? null : user.timezone.name, company == null ? null : company.id,
                company == null ? null : company.name, null, null,
                company == null ? "/superuser/users" : "/superuser/users?companyId=" + company.id);
    }

    private List<UserDirectoryApiModels.UserReference> usersForCompany(Company company, String type, String basePath) {
        return User.<User> find(
                "select distinct u from Company c join c.users u where c = ?1 and lower(u.type) = ?2 order by u.name",
                company, type).list().stream()
                .map(user -> UserDirectoryApiModels.userReference(user, basePath + user.id, null)).toList();
    }

    private String detailPath(User user) {
        if (user == null || user.id == null || user.type == null) {
            return null;
        }
        return switch (user.type.toLowerCase()) {
            case User.TYPE_SUPPORT -> "/superuser/support-users/" + user.id;
            case User.TYPE_SUPERUSER -> "/superuser/superuser-users/" + user.id;
            default -> "/superuser/user-profiles/" + user.id;
        };
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

    private User userByType(Long id, String type) {
        User user = User.findById(id);
        if (user == null || !type.equalsIgnoreCase(user.type)) {
            throw new NotFoundException();
        }
        return user;
    }

    private Company allowedCompany(User currentUser, Long companyId) {
        Company company = Company
                .<Company> find("select distinct c from Company c left join fetch c.users where c.id = ?1", companyId)
                .firstResult();
        if (company == null) {
            throw new NotFoundException();
        }
        boolean allowed = company.users.stream().anyMatch(existing -> existing != null && existing.id != null
                && currentUser.id != null && existing.id.equals(currentUser.id));
        if (!allowed) {
            throw new NotFoundException();
        }
        return company;
    }

    private User requireSuperuser(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isSuperuser(user)) {
            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        return user;
    }
}
