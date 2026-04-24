/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@Path("/api/cap")
@Produces(MediaType.APPLICATION_JSON)
public class CapEndpointResource {

    @ConfigProperty(name = "cap.api.endpoint")
    Optional<String> capApiEndpoint;

    @GET
    @Path("/endpoint")
    public CapEndpointResponse endpoint() {
        return new CapEndpointResponse(capApiEndpoint.orElse(""));
    }

    public record CapEndpointResponse(String endpoint) {
    }
}
