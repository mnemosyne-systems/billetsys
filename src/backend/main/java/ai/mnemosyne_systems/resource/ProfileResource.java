/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.Installation;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Path("/profile")
@Produces(MediaType.TEXT_HTML)
@Blocking
public class ProfileResource {
    @Inject
    Logger logger;

    @GET
    public Object edit(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireUser(auth);
        return Response.seeOther(URI.create("/profile")).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Object update(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, MultivaluedMap<String, String> form) {
        User user = requireUser(auth);
        String name = value(form, "name");
        if (name == null || name.isBlank()) {
            return profileErrorRedirect("Username is required");
        }
        user.name = name.trim();

        String email = value(form, "email");
        if (email != null && !email.isBlank()) {
            user.email = email.trim();
        }

        String social = value(form, "social");
        user.social = trimOrNull(social);

        String fullName = value(form, "fullName");
        user.fullName = trimOrNull(fullName);

        String phoneNumber = value(form, "phoneNumber");
        user.phoneNumber = trimOrNull(phoneNumber);

        String phoneExtension = value(form, "phoneExtension");
        user.phoneExtension = trimOrNull(phoneExtension);

        String countryIdStr = value(form, "countryId");
        if (countryIdStr != null && !countryIdStr.isBlank()) {
            try {
                user.country = Country.findById(Long.valueOf(countryIdStr));
            } catch (NumberFormatException e) {
                logger.warn("Invalid countryId on profile update: " + countryIdStr);
                user.country = null;
            }
        } else {
            user.country = null;
        }

        String timezoneIdStr = value(form, "timezoneId");
        if (timezoneIdStr != null && !timezoneIdStr.isBlank()) {
            try {
                user.timezone = Timezone.findById(Long.valueOf(timezoneIdStr));
            } catch (NumberFormatException e) {
                logger.warn("Invalid timezoneId on profile update: " + timezoneIdStr);
                user.timezone = null;
            }
        } else {
            user.timezone = null;
        }

        if (User.TYPE_ADMIN.equalsIgnoreCase(user.type)) {
            ensureAdminAssignedToOwnerCompany(user);
        } else if (User.TYPE_SUPPORT.equalsIgnoreCase(user.type)) {
            String companyIdStr = value(form, "companyId");
            List<Company> currentCompanies = Company.find("select c from Company c join c.users u where u = ?1", user)
                    .list();
            for (Company c : currentCompanies) {
                c.users.removeIf(u -> u.id != null && u.id.equals(user.id));
            }
            if (companyIdStr != null && !companyIdStr.isBlank()) {
                try {
                    Company newCompany = Company.findById(Long.valueOf(companyIdStr));
                    if (newCompany != null) {
                        boolean exists = newCompany.users.stream().anyMatch(u -> u.id != null && u.id.equals(user.id));
                        if (!exists) {
                            newCompany.users.add(user);
                        }
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Invalid companyId on profile update: " + companyIdStr);
                }
            }
        }

        String logoData = value(form, "logoData");
        if (logoData != null && !logoData.isBlank()) {
            user.logoBase64 = logoData.trim();
        }
        return Response.seeOther(URI.create("/profile")).build();
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

    private String trimOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @GET
    @Path("/password")
    public Object passwordForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireUser(auth);
        return Response.seeOther(URI.create("/profile/password")).build();
    }

    @POST
    @Path("/password")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Object updatePassword(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @FormParam("oldPassword") String oldPassword, @FormParam("newPassword") String newPassword,
            @FormParam("confirmPassword") String confirmPassword) {
        User user = requireUser(auth);
        if (oldPassword == null || oldPassword.isBlank()) {
            return passwordErrorRedirect("Old password is required");
        }
        if (!BcryptUtil.matches(oldPassword, user.passwordHash)) {
            return passwordErrorRedirect("Old password is incorrect");
        }
        if (newPassword == null || newPassword.isBlank()) {
            return passwordErrorRedirect("New password is required");
        }
        if (!newPassword.equals(confirmPassword)) {
            return passwordErrorRedirect("Passwords do not match");
        }
        user.passwordHash = BcryptUtil.bcryptHash(newPassword);
        return Response.seeOther(URI.create("/profile")).build();
    }

    private Response profileErrorRedirect(String error) {
        return Response.seeOther(URI.create("/profile?error=" + encodeQueryValue(error))).build();
    }

    private Response passwordErrorRedirect(String error) {
        return Response.seeOther(URI.create("/profile/password?error=" + encodeQueryValue(error))).build();
    }

    private User requireUser(String auth) {
        User user = AuthHelper.findUser(auth);
        if (user == null) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }

    private String value(MultivaluedMap<String, String> form, String key) {
        if (form == null) {
            return null;
        }
        return form.getFirst(key);
    }

    public static String encodeLogo(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) {
            return null;
        }
        return dataUrl.trim();
    }

    private String encodeQueryValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
