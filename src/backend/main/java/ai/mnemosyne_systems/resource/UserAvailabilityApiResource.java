/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THE ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THIS PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THE AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.UserAvailability;
import ai.mnemosyne_systems.service.UserAvailabilityService;
import ai.mnemosyne_systems.util.AuthHelper;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;

@Path("/api/availability")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Blocking
public class UserAvailabilityApiResource {

    @Inject
    UserAvailabilityService service;

    @GET
    @Path("/me")
    public Response getMyAvailability(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {

        User user = requireAuthenticated(auth);

        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        return Response.ok(service.getForUser(user).stream().map(this::toResponse).toList()).build();
    }

    @POST
    public Response createPersonal(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, AvailabilityRequest request) {

        User user = requireAuthenticated(auth);

        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Company company = Company.find("select c from Company c join c.users u where u = ?1", user).firstResult();

        if (company == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("User does not belong to a company").build();
        }

        UserAvailability availability = service.createPersonal(user, company, request.startDate, request.endDate,
                request.reason);

        return Response.status(Response.Status.CREATED).entity(toResponse(availability)).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            AvailabilityRequest request) {

        User user = requireAuthenticated(auth);

        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        UserAvailability availability = service.update(user, id, request.startDate, request.endDate, request.reason);

        return Response.ok(toResponse(availability)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {

        User user = requireAuthenticated(auth);

        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        service.delete(user, id);

        return Response.noContent().build();
    }

    @GET
    @Path("/company")
    public Response getCompanyAvailability(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {

        User user = requireSupportOrSuperuser(auth);

        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Company company = Company.find("select c from Company c join c.users u where u = ?1", user).firstResult();

        if (company == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("User does not belong to a company").build();
        }

        return Response.ok(service.getForCompany(company).stream().map(this::toResponse).toList()).build();
    }

    @POST
    @Path("/company")
    public Response createCompany(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, AvailabilityRequest request) {

        User user = requireSupportOrSuperuser(auth);

        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Company company = Company.find("select c from Company c join c.users u where u = ?1", user).firstResult();

        if (company == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("User does not belong to a company").build();
        }

        UserAvailability availability = service.createCompany(user, company, request.startDate, request.endDate,
                request.reason);

        return Response.status(Response.Status.CREATED).entity(toResponse(availability)).build();
    }

    private User requireAuthenticated(String auth) {
        User user = AuthHelper.findUser(auth);
        if (user == null) {
            return null;
        }
        if (AuthHelper.isExternal(user) || AuthHelper.isParticipant(user)) {
            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        return user;
    }

    private User requireSupportOrSuperuser(String auth) {
        User user = AuthHelper.findUser(auth);
        if (user == null) {
            return null;
        }
        if (!AuthHelper.isSupport(user) && !AuthHelper.isSuperuser(user)) {
            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        return user;
    }

    public static class AvailabilityRequest {

        public LocalDate startDate;
        public LocalDate endDate;
        public String reason;
    }

    private AvailabilityResponse toResponse(UserAvailability availability) {
        return new AvailabilityResponse(availability.id, availability.scope, availability.startDate,
                availability.endDate, availability.reason);
    }

    public record AvailabilityResponse(Long id, String scope, LocalDate startDate, LocalDate endDate, String reason) {
    }
}
