/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;

import jakarta.ws.rs.FormParam;

@Path("/api/{role}/tickets/{ticketId}/participants")
@Produces(MediaType.APPLICATION_JSON)
public class TicketParticipantUsersApiResource {

    @POST
    @Path("/add")
    @Transactional
    public Response add(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("role") String role,
            @PathParam("ticketId") Long ticketId, @FormParam("email") String email) {
        User currentUser = requireRole(auth, role);

        Ticket ticket = Ticket.findById(ticketId);
        if (ticket == null) {
            throw new NotFoundException();
        }

        Company roleCompany = ticket.company;
        if (roleCompany == null) {
            throw new jakarta.ws.rs.BadRequestException("Ticket has no associated company");
        }

        if (email == null || email.isBlank()) {
            throw new jakarta.ws.rs.BadRequestException("Email is required");
        }
        String normalizedEmail = email.trim().toLowerCase();

        User participantUser = User.find("email", normalizedEmail).firstResult();
        if (participantUser != null && !User.TYPE_PARTICIPANT.equals(participantUser.type)
                && !User.TYPE_EXTERNAL.equals(participantUser.type)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("User with this email already exists and is not a participant or external user.").build();
        }

        if (participantUser == null) {
            participantUser = new User();
            participantUser.email = normalizedEmail;
            participantUser.name = normalizedEmail;
            participantUser.fullName = normalizedEmail;
            participantUser.type = User.TYPE_PARTICIPANT;
            participantUser.passwordHash = User.DISABLED_PASSWORD_HASH;
            participantUser.persist();

            // Link to company
            roleCompany.users.add(participantUser);
        } else {
            Company participantUserCompany = Company
                    .<Company> find("select c from Company c join c.users u where u = ?1", participantUser)
                    .firstResult();
            if (participantUserCompany == null || !participantUserCompany.id.equals(roleCompany.id)) {
                throw new NotFoundException();
            }
        }

        if (!ticket.userUsers.contains(participantUser)) {
            ticket.userUsers.add(participantUser);
        }

        return Response.seeOther(URI.create("/api/" + role + "/tickets/" + ticket.id)).build();
    }

    @POST
    @Path("/{userId}/remove")
    @Transactional
    public Response remove(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("role") String role,
            @PathParam("ticketId") Long ticketId, @PathParam("userId") Long userId) {
        User currentUser = requireRole(auth, role);

        Ticket ticket = Ticket.findById(ticketId);
        if (ticket == null) {
            throw new NotFoundException();
        }

        Company roleCompany = ticket.company;
        if (roleCompany == null) {
            throw new jakarta.ws.rs.BadRequestException("Ticket has no associated company");
        }

        User participantUser = User.findById(userId);
        if (participantUser == null || (!User.TYPE_PARTICIPANT.equals(participantUser.type)
                && !User.TYPE_EXTERNAL.equals(participantUser.type))) {
            throw new NotFoundException();
        }

        Company participantUserCompany = Company
                .<Company> find("select c from Company c join c.users u where u = ?1", participantUser).firstResult();
        if (participantUserCompany == null || !participantUserCompany.id.equals(roleCompany.id)) {
            throw new NotFoundException();
        }

        ticket.userUsers.remove(participantUser);

        return Response.seeOther(URI.create("/api/" + role + "/tickets/" + ticket.id)).build();
    }

    private User requireRole(String auth, String role) {
        User user = AuthHelper.findUser(auth);
        if (user == null) {
            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        if ("superuser".equals(role) && AuthHelper.isSuperuser(user))
            return user;
        if ("user".equals(role) && AuthHelper.isUser(user))
            return user;

        throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
    }
}
