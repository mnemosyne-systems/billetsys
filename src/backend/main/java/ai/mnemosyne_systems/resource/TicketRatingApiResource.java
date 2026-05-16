/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import io.smallrye.common.annotation.Blocking;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("/tickets")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class TicketRatingApiResource {

    private static final int MAX_COMMENT_LENGTH = 2000;

    @POST
    @Path("/{id}/rating")
    @Transactional
    public Response submitRating(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @HeaderParam("X-Billetsys-Client") String client, @PathParam("id") Long id,
            @FormParam("rating") Integer rating, @FormParam("ratingComment") String ratingComment) {

        User user = AuthHelper.findUser(auth);
        if (user == null || (!User.TYPE_USER.equalsIgnoreCase(user.type) && !AuthHelper.isSuperuser(user))) {
            throw new WebApplicationException(Response.seeOther(URI.create("/login")).build());
        }

        Ticket ticket = Ticket.findById(id);
        if (ticket == null) {
            throw new NotFoundException();
        }

        if (!MessageVisibilitySupport.canAccessTicket(user, ticket)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }

        if (!"Resolved".equalsIgnoreCase(ticket.status)) {
            throw new BadRequestException("Ticket must be resolved to be rated.");
        }

        if (ticket.rating != null) {
            throw new BadRequestException("This ticket has already been rated.");
        }

        if (rating == null || rating < 1 || rating > 10) {
            throw new BadRequestException("Rating must be between 1 and 10.");
        }

        ticket.rating = rating;
        ticket.ratingComment = normalizeComment(ratingComment);

        if ("react".equalsIgnoreCase(client)) {
            return Response.ok(java.util.Map.of("redirectTo", "")).type(MediaType.APPLICATION_JSON).build();
        }
        return ReactRedirectSupport.redirect(client, "/tickets/" + ticket.id);
    }

    private String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        String trimmed = comment.trim();
        if (trimmed.length() > MAX_COMMENT_LENGTH) {
            trimmed = trimmed.substring(0, MAX_COMMENT_LENGTH);
        }
        return trimmed;
    }
}
