package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@io.smallrye.common.annotation.Blocking
public class ExternalUserResource {

    @POST
    @Path("{role}/externals")
    @Transactional
    public Response createExternalUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("role") String role,
            @FormParam("name") String name, @FormParam("fullName") String fullName, @FormParam("email") String email,
            @FormParam("social") String social, @FormParam("phoneNumber") String phoneNumber,
            @FormParam("phoneExtension") String phoneExtension, @FormParam("countryId") Long countryId,
            @FormParam("timezoneId") Long timezoneId, @FormParam("companyId") Long companyId) {
        User currentUser = requireRole(auth, role);
        Company company = resolveCompanyForRole(currentUser, role, companyId);
        if (company == null) {
            throw new NotFoundException();
        }

        if (email == null || email.isBlank() || fullName == null || fullName.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Email and Full Name are required").build();
        }

        long count = User.count("email", email);
        if (count > 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Email already exists").build();
        }

        if (name != null && !name.isBlank()) {
            if (User.usernameExists(name)) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Username already exists").build();
            }
        }

        User user = new User();
        user.name = (name != null && !name.isBlank()) ? name : "ext-" + UUID.randomUUID().toString().substring(0, 8); // Use
                                                                                                                      // provided
                                                                                                                      // username
                                                                                                                      // or
                                                                                                                      // auto-generate
        user.fullName = fullName;
        user.email = email;
        user.type = User.TYPE_EXTERNAL;
        user.passwordHash = User.DISABLED_PASSWORD_HASH; // Secure dummy hash, no login allowed
        user.phoneNumber = phoneNumber;
        user.phoneExtension = phoneExtension;
        user.social = social;

        if (countryId != null) {
            user.country = Country.findById(countryId);
        }
        if (timezoneId != null) {
            user.timezone = Timezone.findById(timezoneId);
        }

        user.persist();
        company.users.add(user);

        return Response.seeOther(URI.create("/" + role + "/externals")).build();
    }

    @POST
    @Path("{role}/externals/{id}")
    @Transactional
    public Response updateExternalUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("role") String role,
            @PathParam("id") Long id, @FormParam("name") String name, @FormParam("fullName") String fullName,
            @FormParam("email") String email, @FormParam("social") String social,
            @FormParam("phoneNumber") String phoneNumber, @FormParam("phoneExtension") String phoneExtension,
            @FormParam("countryId") Long countryId, @FormParam("timezoneId") Long timezoneId) {
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

        if (email == null || email.isBlank() || fullName == null || fullName.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Email and Full Name are required").build();
        }

        long count = User.count("email = ?1 and id != ?2", email, id);
        if (count > 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Email already exists").build();
        }

        user.fullName = fullName;
        user.email = email;
        user.phoneNumber = phoneNumber;
        user.phoneExtension = phoneExtension;
        user.social = social;

        if (countryId != null) {
            user.country = Country.findById(countryId);
        }
        if (timezoneId != null) {
            user.timezone = Timezone.findById(timezoneId);
        }

        user.persist();
        return Response.seeOther(URI.create("/" + role + "/externals")).build();
    }

    @POST
    @Path("{role}/externals/{id}/delete")
    @Transactional
    public Response deleteExternalUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("role") String role,
            @PathParam("id") Long id) {
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

        userCompany.users.remove(user);
        user.delete();

        return Response.seeOther(URI.create("/" + role + "/externals")).build();
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
}
