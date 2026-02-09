/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.User;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("/")
@Produces(MediaType.TEXT_HTML)
@Blocking
public class HomeResource {

    @Location("login.html")
    Template login;

    @GET
    public Object index(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @QueryParam("error") String error) {
        User user = AuthHelper.findUser(auth);
        if (AuthHelper.isAdmin(user)) {
            return Response.seeOther(URI.create("/admin/companies")).build();
        }
        if (AuthHelper.isSupport(user)) {
            return Response.seeOther(URI.create("/support")).build();
        }
        if (AuthHelper.isUser(user)) {
            return Response.seeOther(URI.create("/user")).build();
        }
        TemplateInstance instance = login.data("error", error);
        return instance;
    }
}
