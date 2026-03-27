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
import io.smallrye.common.annotation.Blocking;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/owner")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Blocking
public class OwnerApiResource {

    @GET
    @Transactional
    public OwnerResponse owner(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireAdmin(auth);
        return toResponse(OwnerResource.findOwnerCompany());
    }

    @POST
    @Transactional
    public OwnerResponse update(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, OwnerUpdateRequest request) {
        requireAdmin(auth);
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new WebApplicationException("Name is required", Response.Status.BAD_REQUEST);
        }

        Company company = OwnerResource.findOwnerCompany();
        company.name = request.name();
        company.address1 = request.address1();
        company.address2 = request.address2();
        company.city = request.city();
        company.state = request.state();
        company.zip = request.zip();
        company.country = request.countryId() != null ? Country.findById(request.countryId()) : null;
        company.timezone = request.timezoneId() != null ? Timezone.findById(request.timezoneId()) : null;
        company.phoneNumber = request.phoneNumber();
        company.users.clear();
        company.users.addAll(OwnerResource.resolveSelectedUsers(request.supportIds(), request.tamIds()));
        return toResponse(company);
    }

    private User requireAdmin(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isAdmin(user)) {
            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        return user;
    }

    private OwnerResponse toResponse(Company company) {
        List<UserSummary> supportUsers = company.users.stream()
                .filter(user -> User.TYPE_SUPPORT.equalsIgnoreCase(user.type))
                .sorted((left, right) -> left.name.compareToIgnoreCase(right.name)).map(this::toUserSummary).toList();
        List<UserSummary> tamUsers = company.users.stream().filter(user -> User.TYPE_TAM.equalsIgnoreCase(user.type))
                .sorted((left, right) -> left.name.compareToIgnoreCase(right.name)).map(this::toUserSummary).toList();
        List<UserSummary> supportOptions = User.<User> list("type = ?1 order by name", User.TYPE_SUPPORT).stream()
                .map(this::toUserSummary).toList();
        List<UserSummary> tamOptions = User.<User> list("type = ?1 order by name", User.TYPE_TAM).stream()
                .map(this::toUserSummary).toList();
        List<CountryOption> countries = Country.<Country> list("order by name").stream()
                .map(country -> new CountryOption(country.id, country.name, country.code)).toList();
        List<TimezoneOption> timezones = Timezone.<Timezone> list("order by name").stream()
                .map(timezone -> new TimezoneOption(timezone.id, timezone.name,
                        timezone.country == null ? null : timezone.country.id))
                .toList();

        return new OwnerResponse(company.id, company.name, company.address1, company.address2, company.city,
                company.state, company.zip, company.phoneNumber, company.country == null ? null : company.country.id,
                company.country == null ? null : company.country.name,
                company.timezone == null ? null : company.timezone.id,
                company.timezone == null ? null : company.timezone.name, supportUsers, tamUsers, supportOptions,
                tamOptions, countries, timezones);
    }

    private UserSummary toUserSummary(User user) {
        return new UserSummary(user.id, user.name, user.getDisplayName(), user.email, "/user/" + user.id);
    }

    public record OwnerResponse(Long id, String name, String address1, String address2, String city, String state,
            String zip, String phoneNumber, Long countryId, String countryName, Long timezoneId, String timezoneName,
            List<UserSummary> supportUsers, List<UserSummary> tamUsers, List<UserSummary> supportOptions,
            List<UserSummary> tamOptions, List<CountryOption> countries, List<TimezoneOption> timezones) {
    }

    public record UserSummary(Long id, String username, String displayName, String email, String profilePath) {
    }

    public record CountryOption(Long id, String name, String code) {
    }

    public record TimezoneOption(Long id, String name, Long countryId) {
    }

    public record OwnerUpdateRequest(String name, String address1, String address2, String city, String state,
            String zip, String phoneNumber, Long countryId, Long timezoneId, List<Long> supportIds, List<Long> tamIds) {
    }
}
