/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.util.AuthHelper;
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
        NewCookie expired = new NewCookie.Builder(AuthHelper.AUTH_COOKIE).value("").path("/").comment("auth").maxAge(0)
                .secure(false).build();
        NewCookie expiredJSessionId = new NewCookie.Builder("JSESSIONID").value("").path("/").comment("session")
                .maxAge(0).secure(false).build();
        return Response.status(Response.Status.SEE_OTHER).header("Location", "/").cookie(expired)
                .cookie(expiredJSessionId).build();
    }
}
