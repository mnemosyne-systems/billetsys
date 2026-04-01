/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;

final class ReactRedirectSupport {

    private ReactRedirectSupport() {
    }

    static Response redirect(String client, String path) {
        if ("react".equalsIgnoreCase(client)) {
            return Response.ok(new RedirectResponse(path)).type(MediaType.APPLICATION_JSON).build();
        }
        return Response.seeOther(URI.create(path)).build();
    }

    record RedirectResponse(String redirectTo) {
    }
}
