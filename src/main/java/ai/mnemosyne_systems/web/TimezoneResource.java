/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/timezones")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
public class TimezoneResource {

    @GET
    public Response list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @QueryParam("countryId") Long countryId) {
        User user = AuthHelper.findUser(auth);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        if (countryId == null) {
            return Response.ok("[]").build();
        }
        Country country = Country.findById(countryId);
        if (country == null) {
            return Response.ok("[]").build();
        }
        List<Timezone> timezones = Timezone.list("country = ?1 order by name", country);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < timezones.size(); i++) {
            Timezone tz = timezones.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\"id\":").append(tz.id).append(",\"name\":\"").append(tz.name.replace("\"", "\\\""))
                    .append("\"}");
        }
        sb.append("]");
        return Response.ok(sb.toString()).build();
    }
}
