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

@Path("/api/{role}/tickets/{ticketId}/externals")
@Produces(MediaType.APPLICATION_JSON)
public class TicketExternalUsersApiResource {

    @POST
    @Path("/add")
    @Transactional
    public Response add(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("role") String role,
            @PathParam("ticketId") Long ticketId, @FormParam("email") String email) {
        User currentUser = requireRole(auth, role);
        Company roleCompany = resolveCompanyForRole(currentUser, role, null);

        Ticket ticket = Ticket.findById(ticketId);
        if (ticket == null) {
            throw new NotFoundException();
        }

        if (email == null || email.isBlank()) {
            throw new jakarta.ws.rs.BadRequestException("Email is required");
        }
        String normalizedEmail = email.trim().toLowerCase();

        User externalUser = User.find("email", normalizedEmail).firstResult();
        if (externalUser != null && !User.TYPE_EXTERNAL.equals(externalUser.type)) {
            throw new jakarta.ws.rs.BadRequestException(
                    "User with this email already exists and is not an external user.");
        }

        if (externalUser == null) {
            externalUser = new User();
            externalUser.email = normalizedEmail;
            externalUser.name = normalizedEmail;
            externalUser.fullName = normalizedEmail;
            externalUser.type = User.TYPE_EXTERNAL;
            externalUser.passwordHash = User.DISABLED_PASSWORD_HASH;
            externalUser.persist();

            // Link to company
            roleCompany.users.add(externalUser);
        } else {
            Company externalUserCompany = Company
                    .<Company> find("select c from Company c join c.users u where u = ?1", externalUser).firstResult();
            if (externalUserCompany == null || roleCompany == null || !externalUserCompany.id.equals(roleCompany.id)) {
                throw new NotFoundException();
            }
        }

        if (!ticket.externalUsers.contains(externalUser)) {
            ticket.externalUsers.add(externalUser);
        }

        return Response.seeOther(URI.create("/api/" + role + "/tickets/" + ticket.id)).build();
    }

    @POST
    @Path("/{userId}/remove")
    @Transactional
    public Response remove(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("role") String role,
            @PathParam("ticketId") Long ticketId, @PathParam("userId") Long userId) {
        User currentUser = requireRole(auth, role);
        Company roleCompany = resolveCompanyForRole(currentUser, role, null);

        Ticket ticket = Ticket.findById(ticketId);
        if (ticket == null) {
            throw new NotFoundException();
        }

        User externalUser = User.findById(userId);
        if (externalUser == null || !User.TYPE_EXTERNAL.equals(externalUser.type)) {
            throw new NotFoundException();
        }

        Company externalUserCompany = Company
                .<Company> find("select c from Company c join c.users u where u = ?1", externalUser).firstResult();
        if (externalUserCompany == null || roleCompany == null || !externalUserCompany.id.equals(roleCompany.id)) {
            throw new NotFoundException();
        }

        ticket.externalUsers.remove(externalUser);

        return Response.seeOther(URI.create("/api/" + role + "/tickets/" + ticket.id)).build();
    }

    private User requireRole(String auth, String role) {
        User user = AuthHelper.findUser(auth);
        if (user == null) {
            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        if ("support".equals(role) && AuthHelper.isSupport(user))
            return user;
        if ("tam".equals(role) && AuthHelper.isTam(user))
            return user;

        throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
    }

    private Company resolveCompanyForRole(User user, String role, Long requestedCompanyId) {
        if ("support".equals(role) || "tam".equals(role)) {
            return OwnerResource.findOwnerCompany();
        }
        return Company.<Company> find("select c from Company c join c.users u where u = ?1", user).firstResult();
    }
}
