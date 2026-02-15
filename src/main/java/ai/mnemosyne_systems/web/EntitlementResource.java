/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.Entitlement;
import ai.mnemosyne_systems.model.User;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("/entitlements")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class EntitlementResource {

    @Location("entitlement/entitlements.html")
    Template entitlementsTemplate;

    @Location("entitlement/entitlement-form.html")
    Template entitlementFormTemplate;

    @GET
    public TemplateInstance listEntitlements(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        return entitlementsTemplate.data("entitlements", Entitlement.listAll()).data("currentUser", user);
    }

    @GET
    @Path("create")
    public TemplateInstance createForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        Entitlement entitlement = new Entitlement();
        entitlement.description = "";
        return entitlementFormTemplate.data("entitlement", entitlement).data("action", "/entitlements")
                .data("title", "New entitlement").data("currentUser", user);
    }

    @GET
    @Path("{id}/edit")
    public TemplateInstance editForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireAdmin(auth);
        Entitlement entitlement = Entitlement.findById(id);
        if (entitlement == null) {
            throw new NotFoundException();
        }
        return entitlementFormTemplate.data("entitlement", entitlement).data("action", "/entitlements/" + id)
                .data("title", "Edit entitlement").data("currentUser", user);
    }

    @POST
    @Path("")
    @Transactional
    public Response createEntitlement(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @FormParam("name") String name,
            @FormParam("description") String description) {
        requireAdmin(auth);
        validate(name, description);
        Entitlement entitlement = new Entitlement();
        entitlement.name = name.trim();
        entitlement.description = description.trim();
        entitlement.persist();
        return Response.seeOther(URI.create("/entitlements")).build();
    }

    @POST
    @Path("{id}")
    @Transactional
    public Response updateEntitlement(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            @FormParam("name") String name, @FormParam("description") String description) {
        requireAdmin(auth);
        Entitlement entitlement = Entitlement.findById(id);
        if (entitlement == null) {
            throw new NotFoundException();
        }
        validate(name, description);
        entitlement.name = name.trim();
        entitlement.description = description.trim();
        return Response.seeOther(URI.create("/entitlements")).build();
    }

    @POST
    @Path("{id}/delete")
    @Transactional
    public Response deleteEntitlement(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireAdmin(auth);
        Entitlement entitlement = Entitlement.findById(id);
        if (entitlement == null) {
            throw new NotFoundException();
        }
        entitlement.delete();
        return Response.seeOther(URI.create("/entitlements")).build();
    }

    private void validate(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
        if (description == null || description.isBlank()) {
            throw new BadRequestException("Description is required");
        }
    }

    private User requireAdmin(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isAdmin(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }
}
