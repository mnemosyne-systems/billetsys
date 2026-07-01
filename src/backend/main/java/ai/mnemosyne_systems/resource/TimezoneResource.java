/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.CurrentUser;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
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
@RolesAllowed({ "admin", "support", "superuser", "tam", "user" })
public class TimezoneResource {

    @Inject
    CurrentUser currentUser;

    @GET
    public Response list(@QueryParam("countryId") Long countryId) {
        User user = currentUser.get();

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
