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
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Path("/login")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class AuthResource {

    @GET
    public Response loginPage() {
        return Response.seeOther(URI.create("/")).build();
    }

    @POST
    public Response login(@FormParam("username") String username, @FormParam("password") String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Response.seeOther(errorRedirect("Username and password are required")).build();
        }
        User user = User.find("name", username.trim()).firstResult();
        if (user == null || user.passwordHash == null || !BcryptUtil.matches(password, user.passwordHash)) {
            return Response.seeOther(errorRedirect("Invalid credentials")).build();
        }
        String cookieValue = AuthHelper.SESSION_NONCE + ":" + user.id;
        NewCookie cookie = new NewCookie(AuthHelper.AUTH_COOKIE, cookieValue, "/", null, NewCookie.DEFAULT_VERSION,
                "auth", 3600, false);
        String redirect;
        if (AuthHelper.isAdmin(user)) {
            redirect = "/companies";
        } else if (AuthHelper.isSupport(user)) {
            redirect = "/support";
        } else {
            redirect = "/user";
        }
        return Response.seeOther(URI.create(redirect)).cookie(cookie).build();
    }

    private URI errorRedirect(String message) {
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return URI.create("/?error=" + encoded);
    }
}
