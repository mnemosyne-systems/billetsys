/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Path("/login")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class AuthResource {

    @GET
    public Response loginPage() {
        return seeOther("/").build();
    }

    @POST
    public Response login(@FormParam("username") String username, @FormParam("password") String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return seeOther(errorRedirect("Username and password are required")).build();
        }
        User user = User.find("name", username.trim()).firstResult();
        if (user == null || user.passwordHash == null || !BcryptUtil.matches(password, user.passwordHash)) {
            return seeOther(errorRedirect("Invalid credentials")).build();
        }
        String cookieValue = AuthHelper.createSessionCookieValue(user);
        NewCookie cookie = new NewCookie.Builder(AuthHelper.AUTH_COOKIE).value(cookieValue).path("/").comment("auth")
                .maxAge(3600).secure(false).build();
        String redirect = redirectPath(user);
        return seeOther(redirect).cookie(cookie).build();
    }

    private String errorRedirect(String message) {
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return "/login?error=" + encoded;
    }

    private String redirectPath(User user) {
        if (AuthHelper.isAdmin(user)) {
            return "/";
        }
        if (AuthHelper.isSupport(user)) {
            return "/support/tickets";
        }
        if (AuthHelper.isSuperuser(user)) {
            return "/superuser/tickets";
        }
        return "/user/tickets";
    }

    private Response.ResponseBuilder seeOther(String location) {
        return Response.status(Response.Status.SEE_OTHER).header("Location", location);
    }
}
