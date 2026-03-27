package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Company;
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
import java.util.List;

@Path("/api/user")
@Produces(MediaType.APPLICATION_JSON)
public class UserViewApiResource {

    @GET
    @Path("/support-users/{id}")
    @Transactional
    public UserDirectoryApiModels.UserDetailResponse supportUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        requireUser(auth);
        return profileDetail(id, User.TYPE_SUPPORT);
    }

    @GET
    @Path("/tam-users/{id}")
    @Transactional
    public UserDirectoryApiModels.UserDetailResponse tamUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        requireUser(auth);
        return profileDetail(id, User.TYPE_TAM);
    }

    @GET
    @Path("/superuser-users/{id}")
    @Transactional
    public UserDirectoryApiModels.UserDetailResponse superuser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        User currentUser = requireUser(auth);
        User user = User.findById(id);
        if (user == null || !User.TYPE_SUPERUSER.equalsIgnoreCase(user.type)) {
            throw new NotFoundException();
        }
        boolean allowed = Company.count(
                "select count(c) from Company c join c.users current join c.users viewed where current = ?1 and viewed = ?2",
                currentUser, user) > 0;
        if (!allowed) {
            throw new NotFoundException();
        }
        return detailResponse(user);
    }

    @GET
    @Path("/user-profiles/{id}")
    @Transactional
    public UserDirectoryApiModels.UserDetailResponse profile(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        requireUser(auth);
        User user = User.findById(id);
        if (user == null) {
            throw new NotFoundException();
        }
        return detailResponse(user);
    }

    @GET
    @Path("/companies/{id}")
    @Transactional
    public UserDirectoryApiModels.CompanyDetailResponse company(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        requireUser(auth);
        Company company = Company.findById(id);
        if (company == null) {
            throw new NotFoundException();
        }
        return new UserDirectoryApiModels.CompanyDetailResponse(company.id, company.name, company.address1,
                company.address2, company.city, company.state, company.zip, company.phoneNumber,
                company.country == null ? null : company.country.name,
                company.timezone == null ? null : company.timezone.name,
                usersForCompany(company, User.TYPE_USER, "/user/user-profiles/"),
                usersForCompany(company, User.TYPE_SUPERUSER, "/user/superuser-users/"),
                usersForCompany(company, User.TYPE_TAM, "/user/tam-users/"), List.of(), "/user/tickets");
    }

    private UserDirectoryApiModels.UserDetailResponse profileDetail(Long id, String expectedType) {
        User user = User.findById(id);
        if (user == null || !expectedType.equalsIgnoreCase(user.type)) {
            throw new NotFoundException();
        }
        return detailResponse(user);
    }

    private UserDirectoryApiModels.UserDetailResponse detailResponse(User user) {
        Company company = Company.<Company> find("select c from Company c join c.users u where u = ?1", user)
                .firstResult();
        return new UserDirectoryApiModels.UserDetailResponse(user.id, user.name, user.getDisplayName(), user.fullName,
                user.email, user.social, user.phoneNumber, user.phoneExtension, user.type,
                UserDirectoryApiModels.typeLabel(user.type), user.country == null ? null : user.country.name,
                user.timezone == null ? null : user.timezone.name, company == null ? null : company.id,
                company == null ? null : company.name, null, null, "/user/tickets");
    }

    private List<UserDirectoryApiModels.UserReference> usersForCompany(Company company, String type, String basePath) {
        return User.<User> find(
                "select distinct u from Company c join c.users u where c = ?1 and lower(u.type) = ?2 order by u.name",
                company, type).list().stream()
                .map(user -> UserDirectoryApiModels.userReference(user, basePath + user.id, null)).toList();
    }

    private User requireUser(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isUser(user)) {
            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        return user;
    }
}
