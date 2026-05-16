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
import ai.mnemosyne_systems.model.Installation;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.infra.BrandingProvider;
import ai.mnemosyne_systems.util.AuthHelper;
import io.smallrye.common.annotation.Blocking;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Path("/owner")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class OwnerResource {

    @GET
    public Response viewOwner(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireAdmin(auth);
        return Response.seeOther(URI.create("/owner")).build();
    }

    @GET
    @Path("/edit")
    public Response editOwner(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireAdmin(auth);
        return Response.seeOther(URI.create("/owner/edit")).build();
    }

    @POST
    @Path("/edit")
    @Transactional
    public Response updateOwner(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @FormParam("name") String name,
            @FormParam("address1") String address1, @FormParam("address2") String address2,
            @FormParam("city") String city, @FormParam("state") String state, @FormParam("zip") String zip,
            @FormParam("countryId") Long countryId, @FormParam("timezoneId") Long timezoneId,
            @FormParam("phoneNumber") String phoneNumber, @FormParam("supportIds") List<Long> supportIds,
            @FormParam("tamIds") List<Long> tamIds, @FormParam("headerFooterColor") String headerFooterColor,
            @FormParam("headersColor") String headersColor, @FormParam("buttonsColor") String buttonsColor,
            @FormParam("use24HourClock") Boolean use24HourClock,
            @FormParam("ticketAutoCloseDays") Integer ticketAutoCloseDays) {
        requireAdmin(auth);
        Company company = findOwnerCompany();
        if (name == null || name.isBlank()) {
            throw new jakarta.ws.rs.BadRequestException("Name is required");
        }
        String normalizedHeaderFooterColor = normalizeOwnerColor(headerFooterColor);
        String normalizedHeadersColor = normalizeOwnerColor(headersColor);
        String normalizedButtonsColor = normalizeOwnerColor(buttonsColor);
        company.name = name;
        company.address1 = address1;
        company.address2 = address2;
        company.city = city;
        company.state = state;
        company.zip = zip;
        company.country = countryId != null ? Country.findById(countryId) : null;
        company.timezone = timezoneId != null ? Timezone.findById(timezoneId) : null;
        company.phoneNumber = phoneNumber;
        company.users.clear();
        company.users.addAll(resolveSelectedUsers(supportIds, tamIds));
        Installation installation = findOrCreateInstallation(company);
        installation.name = name;
        installation.headerFooterColor = normalizedHeaderFooterColor;
        installation.headersColor = normalizedHeadersColor;
        installation.buttonsColor = normalizedButtonsColor;
        installation.use24HourClock = Boolean.TRUE.equals(use24HourClock);
        installation.ticketAutoCloseDays = clampAutoCloseDays(ticketAutoCloseDays);
        return Response.seeOther(URI.create("/owner")).build();
    }

    static Company findOwnerCompany() {
        Installation installation = Installation.find(
                "select i from Installation i join fetch i.company c left join fetch c.users where i.singletonKey = ?1",
                "installation").firstResult();
        Company company = installation == null ? null : installation.company;
        if (company == null) {
            company = Company
                    .find("select distinct c from Company c left join fetch c.users where lower(c.name) = lower(?1)",
                            "mnemosyne systems")
                    .firstResult();
        }
        if (company == null) {
            throw new NotFoundException();
        }
        return company;
    }

    static List<User> resolveSelectedUsers(List<Long> supportIds, List<Long> tamIds) {
        List<Long> ids = new ArrayList<>();
        if (supportIds != null) {
            ids.addAll(supportIds);
        }
        if (tamIds != null) {
            ids.addAll(tamIds);
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        return User.list("id in ?1 and type in ?2", ids, List.of(User.TYPE_SUPPORT, User.TYPE_TAM));
    }

    static Installation findOrCreateInstallation(Company company) {
        Installation installation = Installation.find("singletonKey", "installation").firstResult();
        if (installation == null) {
            installation = new Installation();
            installation.singletonKey = "installation";
        }
        installation.company = company;
        if (installation.name == null || installation.name.isBlank()) {
            installation.name = company.name;
        }
        String baseColor = BrandingProvider.normalizeInstallationColor(
                firstNonBlank(installation.headerFooterColor, installation.headersColor, installation.buttonsColor));
        if (installation.headerFooterColor == null || installation.headerFooterColor.isBlank()) {
            installation.headerFooterColor = baseColor;
        }
        if (installation.headersColor == null || installation.headersColor.isBlank()) {
            installation.headersColor = baseColor;
        }
        if (installation.buttonsColor == null || installation.buttonsColor.isBlank()) {
            installation.buttonsColor = baseColor;
        }
        if (installation.use24HourClock == null) {
            installation.use24HourClock = false;
        }
        if (installation.ticketAutoCloseDays == null) {
            installation.ticketAutoCloseDays = 7;
        }
        installation.persist();
        return installation;
    }

    static String normalizeOwnerColor(String color) {
        if (color == null || color.isBlank()) {
            return BrandingProvider.DEFAULT_INSTALLATION_COLOR;
        }
        if (!BrandingProvider.isValidInstallationColor(color)) {
            throw new jakarta.ws.rs.BadRequestException("Color must be a valid hex value like #b00020");
        }
        return BrandingProvider.normalizeInstallationColor(color);
    }

    static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    static int clampAutoCloseDays(Integer value) {
        if (value == null) {
            return 7;
        }
        return Math.max(0, Math.min(365, value));
    }

    static User requireAdmin(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isAdmin(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }
}
