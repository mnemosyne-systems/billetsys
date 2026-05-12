/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.infra.BrandingProvider;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Installation;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;

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
@RolesAllowed("admin")
public class OwnerApiResource {

    @GET
    @Transactional
    public OwnerResponse owner() {
        return toResponse(OwnerResource.findOwnerCompany());
    }

    @POST
    @Transactional
    public OwnerResponse update(OwnerUpdateRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new WebApplicationException("Name is required", Response.Status.BAD_REQUEST);
        }

        Company company = OwnerResource.findOwnerCompany();
        String normalizedHeaderFooterColor = OwnerResource.normalizeOwnerColor(request.headerFooterColor());
        String normalizedHeadersColor = OwnerResource.normalizeOwnerColor(request.headersColor());
        String normalizedButtonsColor = OwnerResource.normalizeOwnerColor(request.buttonsColor());
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
        Installation installation = OwnerResource.findOrCreateInstallation(company);
        installation.name = request.name();
        installation.headerFooterColor = normalizedHeaderFooterColor;
        installation.headersColor = normalizedHeadersColor;
        installation.buttonsColor = normalizedButtonsColor;
        installation.use24HourClock = Boolean.TRUE.equals(request.use24HourClock());
        installation.ticketAutoCloseDays = OwnerResource.clampAutoCloseDays(request.ticketAutoCloseDays());
        installation.adminRoleIcon = trimToNull(request.adminRoleIcon());
        installation.supportRoleIcon = trimToNull(request.supportRoleIcon());
        installation.superuserRoleIcon = trimToNull(request.superuserRoleIcon());
        installation.tamRoleIcon = trimToNull(request.tamRoleIcon());
        installation.userRoleIcon = trimToNull(request.userRoleIcon());
        installation.externalRoleIcon = trimToNull(request.externalRoleIcon());
        installation.adminRoleColor = BrandingProvider.normalizeRoleColor(request.adminRoleColor());
        installation.supportRoleColor = BrandingProvider.normalizeRoleColor(request.supportRoleColor());
        installation.superuserRoleColor = BrandingProvider.normalizeRoleColor(request.superuserRoleColor());
        installation.tamRoleColor = BrandingProvider.normalizeRoleColor(request.tamRoleColor());
        installation.userRoleColor = BrandingProvider.normalizeRoleColor(request.userRoleColor());
        installation.externalRoleColor = BrandingProvider.normalizeRoleColor(request.externalRoleColor());
        installation.logoBase64 = trimToNull(request.logoBase64());
        installation.backgroundBase64 = trimToNull(request.backgroundBase64());
        return toResponse(company);
    }

    private OwnerResponse toResponse(Company company) {
        Installation installation = OwnerResource.findOrCreateInstallation(company);
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
                company.country == null ? null : company.country.name, installation.logoBase64,
                installation.backgroundBase64,
                BrandingProvider.normalizeInstallationColor(installation.headerFooterColor),
                BrandingProvider.normalizeInstallationColor(installation.headersColor),
                BrandingProvider.normalizeInstallationColor(installation.buttonsColor),
                Boolean.TRUE.equals(installation.use24HourClock), company.timezone == null ? null : company.timezone.id,
                company.timezone == null ? null : company.timezone.name,
                installation.ticketAutoCloseDays != null ? installation.ticketAutoCloseDays : 7,
                installation.adminRoleIcon, installation.supportRoleIcon, installation.superuserRoleIcon,
                installation.tamRoleIcon, installation.userRoleIcon, installation.externalRoleIcon,
                BrandingProvider.normalizeRoleColor(installation.adminRoleColor),
                BrandingProvider.normalizeRoleColor(installation.supportRoleColor),
                BrandingProvider.normalizeRoleColor(installation.superuserRoleColor),
                BrandingProvider.normalizeRoleColor(installation.tamRoleColor),
                BrandingProvider.normalizeRoleColor(installation.userRoleColor),
                BrandingProvider.normalizeRoleColor(installation.externalRoleColor), supportUsers, tamUsers,
                supportOptions, tamOptions, countries, timezones);
    }

    private UserSummary toUserSummary(User user) {
        return new UserSummary(user.id, user.name, user.getDisplayName(), user.email, "/user/" + user.id);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record OwnerResponse(Long id, String name, String address1, String address2, String city, String state,
            String zip, String phoneNumber, Long countryId, String countryName, String logoBase64,
            String backgroundBase64, String headerFooterColor, String headersColor, String buttonsColor,
            boolean use24HourClock, Long timezoneId, String timezoneName, int ticketAutoCloseDays, String adminRoleIcon,
            String supportRoleIcon, String superuserRoleIcon, String tamRoleIcon, String userRoleIcon,
            String externalRoleIcon, String adminRoleColor, String supportRoleColor, String superuserRoleColor,
            String tamRoleColor, String userRoleColor, String externalRoleColor, List<UserSummary> supportUsers,
            List<UserSummary> tamUsers, List<UserSummary> supportOptions, List<UserSummary> tamOptions,
            List<CountryOption> countries, List<TimezoneOption> timezones) {
    }

    public record UserSummary(Long id, String username, String displayName, String email, String profilePath) {
    }

    public record CountryOption(Long id, String name, String code) {
    }

    public record TimezoneOption(Long id, String name, Long countryId) {
    }

    public record OwnerUpdateRequest(String name, String address1, String address2, String city, String state,
            String zip, String phoneNumber, Long countryId, Long timezoneId, List<Long> supportIds, List<Long> tamIds,
            String headerFooterColor, String headersColor, String buttonsColor, Boolean use24HourClock,
            Integer ticketAutoCloseDays, String adminRoleIcon, String supportRoleIcon, String superuserRoleIcon,
            String tamRoleIcon, String userRoleIcon, String externalRoleIcon, String adminRoleColor,
            String supportRoleColor, String superuserRoleColor, String tamRoleColor, String userRoleColor,
            String externalRoleColor, String logoBase64, String backgroundBase64) {
    }
}
