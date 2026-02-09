/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("/logout")
@Produces(MediaType.TEXT_HTML)
@Blocking
public class LogoutResource {

    @GET
    public Response logout() {
        NewCookie expired = new NewCookie(AuthHelper.AUTH_COOKIE, "", "/", null, NewCookie.DEFAULT_VERSION, "auth", 0,
                false);
        return Response.seeOther(URI.create("/")).cookie(expired).build();
    }
}
