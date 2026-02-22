/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

@Path("/logout")
@Produces(MediaType.TEXT_HTML)
@Blocking
public class LogoutResource {

    @GET
    public Response logout(@CookieParam(AuthHelper.AUTH_COOKIE) String authCookieValue) {
        AuthHelper.clearSession(authCookieValue);
        NewCookie expired = new NewCookie(AuthHelper.AUTH_COOKIE, "", "/", null, NewCookie.DEFAULT_VERSION, "auth", 0,
                false);
        NewCookie expiredJSessionId = new NewCookie("JSESSIONID", "", "/", null, NewCookie.DEFAULT_VERSION, "session",
                0, false);
        return Response.status(Response.Status.SEE_OTHER).header("Location", "/").cookie(expired)
                .cookie(expiredJSessionId).build();
    }
}
