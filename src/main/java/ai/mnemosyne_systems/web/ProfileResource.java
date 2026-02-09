/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.User;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Path("/profile")
@Produces(MediaType.TEXT_HTML)
@Blocking
public class ProfileResource {

    @Location("support/profile.html")
    Template supportProfileTemplate;

    @Location("admin/profile.html")
    Template adminProfileTemplate;

    @Location("support/profile-password.html")
    Template supportPasswordTemplate;

    @Location("admin/profile-password.html")
    Template adminPasswordTemplate;

    @GET
    public TemplateInstance edit(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireUser(auth);
        return profileTemplate(user, null);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Object update(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, MultivaluedMap<String, String> form) {
        User user = requireUser(auth);
        String name = value(form, "name");
        if (name == null || name.isBlank()) {
            return profileTemplate(user, "Username is required");
        }
        user.name = name.trim();
        String logoData = value(form, "logoData");
        if (logoData != null && !logoData.isBlank()) {
            user.logoBase64 = logoData.trim();
        }
        return Response.seeOther(URI.create("/profile")).build();
    }

    @GET
    @Path("/password")
    public TemplateInstance passwordForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireUser(auth);
        return passwordTemplate(user, null);
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
            return passwordTemplate(user, "Old password is required");
        }
        if (!BcryptUtil.matches(oldPassword, user.passwordHash)) {
            return passwordTemplate(user, "Old password is incorrect");
        }
        if (newPassword == null || newPassword.isBlank()) {
            return passwordTemplate(user, "New password is required");
        }
        if (!newPassword.equals(confirmPassword)) {
            return passwordTemplate(user, "Passwords do not match");
        }
        user.passwordHash = BcryptUtil.bcryptHash(newPassword);
        return Response.seeOther(URI.create("/profile")).build();
    }

    private TemplateInstance profileTemplate(User user, String error) {
        Template template = AuthHelper.isAdmin(user) ? adminProfileTemplate : supportProfileTemplate;
        String cancelUrl = AuthHelper.isAdmin(user) ? "/admin/companies" : "/support";
        TemplateInstance instance = template.data("user", user).data("currentUser", user).data("error", error)
                .data("cancelUrl", cancelUrl);
        if (!AuthHelper.isAdmin(user)) {
            SupportResource.SupportTicketCounts counts = SupportResource.loadTicketCounts(user);
            instance.data("assignedCount", counts.assignedCount).data("openCount", counts.openCount)
                    .data("ticketsBase", "/support").data("showSupportUsers", true);
        }
        return instance;
    }

    private TemplateInstance passwordTemplate(User user, String error) {
        Template template = AuthHelper.isAdmin(user) ? adminPasswordTemplate : supportPasswordTemplate;
        String cancelUrl = "/profile";
        TemplateInstance instance = template.data("currentUser", user).data("error", error).data("cancelUrl",
                cancelUrl);
        if (!AuthHelper.isAdmin(user)) {
            SupportResource.SupportTicketCounts counts = SupportResource.loadTicketCounts(user);
            instance.data("assignedCount", counts.assignedCount).data("openCount", counts.openCount)
                    .data("ticketsBase", "/support").data("showSupportUsers", true);
        }
        return instance;
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
}
