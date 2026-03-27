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
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
@Produces(MediaType.TEXT_HTML)
@Blocking
public class HomeResource {

    @GET
    public Object index(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @QueryParam("error") String error) {
        User user = AuthHelper.findUser(auth);
        if (AuthHelper.isAdmin(user) || AuthHelper.isSupport(user) || AuthHelper.isSuperuser(user)
                || AuthHelper.isUser(user)) {
            return Response.status(Response.Status.SEE_OTHER).header("Location", "/").build();
        }
        String location = error == null || error.isBlank() ? "/login" : "/login?error=" + error;
        return Response.status(Response.Status.SEE_OTHER).header("Location", location).build();
    }

    @GET
    @Path("/admin")
    public Object adminHome(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isAdmin(user)) {
            return Response.status(Response.Status.SEE_OTHER).header("Location", "/").build();
        }
        return Response.status(Response.Status.SEE_OTHER).header("Location", "/").build();
    }
}
