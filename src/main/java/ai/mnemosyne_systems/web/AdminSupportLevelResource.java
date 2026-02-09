/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.SupportLevel;
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

@Path("/admin/support-levels")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class AdminSupportLevelResource {

    @Location("admin/support-levels.html")
    Template supportLevelsTemplate;

    @Location("admin/support-level-form.html")
    Template supportLevelFormTemplate;

    @GET
    public TemplateInstance listSupportLevels(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        return supportLevelsTemplate.data("supportLevels", SupportLevel.listAll()).data("currentUser", user);
    }

    @GET
    @Path("create")
    public TemplateInstance createForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        SupportLevel level = new SupportLevel();
        level.description = "";
        level.critical = 60;
        level.criticalColor = "Red";
        level.escalate = 120;
        level.escalateColor = "Yellow";
        level.normal = 720;
        level.normalColor = "White";
        return supportLevelFormTemplate.data("supportLevel", level).data("action", "/admin/support-levels")
                .data("title", "New support level").data("currentUser", user);
    }

    @GET
    @Path("{id}/edit")
    public TemplateInstance editForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireAdmin(auth);
        SupportLevel level = SupportLevel.findById(id);
        if (level == null) {
            throw new NotFoundException();
        }
        return supportLevelFormTemplate.data("supportLevel", level).data("action", "/admin/support-levels/" + id)
                .data("title", "Edit support level").data("currentUser", user);
    }

    @POST
    @Path("")
    @Transactional
    public Response createSupportLevel(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @FormParam("name") String name,
            @FormParam("description") String description, @FormParam("critical") Integer critical,
            @FormParam("criticalColor") String criticalColor, @FormParam("escalate") Integer escalate,
            @FormParam("escalateColor") String escalateColor, @FormParam("normal") Integer normal,
            @FormParam("normalColor") String normalColor) {
        requireAdmin(auth);
        validate(name, description, critical, criticalColor, escalate, escalateColor, normal, normalColor);
        SupportLevel level = new SupportLevel();
        level.name = name.trim();
        level.description = description.trim();
        level.critical = critical;
        level.criticalColor = criticalColor.trim();
        level.escalate = escalate;
        level.escalateColor = escalateColor.trim();
        level.normal = normal;
        level.normalColor = normalColor.trim();
        level.persist();
        return Response.seeOther(URI.create("/admin/support-levels")).build();
    }

    @POST
    @Path("{id}")
    @Transactional
    public Response updateSupportLevel(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            @FormParam("name") String name, @FormParam("description") String description,
            @FormParam("critical") Integer critical, @FormParam("criticalColor") String criticalColor,
            @FormParam("escalate") Integer escalate, @FormParam("escalateColor") String escalateColor,
            @FormParam("normal") Integer normal, @FormParam("normalColor") String normalColor) {
        requireAdmin(auth);
        SupportLevel level = SupportLevel.findById(id);
        if (level == null) {
            throw new NotFoundException();
        }
        validate(name, description, critical, criticalColor, escalate, escalateColor, normal, normalColor);
        level.name = name.trim();
        level.description = description.trim();
        level.critical = critical;
        level.criticalColor = criticalColor.trim();
        level.escalate = escalate;
        level.escalateColor = escalateColor.trim();
        level.normal = normal;
        level.normalColor = normalColor.trim();
        return Response.seeOther(URI.create("/admin/support-levels")).build();
    }

    @POST
    @Path("{id}/delete")
    @Transactional
    public Response deleteSupportLevel(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireAdmin(auth);
        SupportLevel level = SupportLevel.findById(id);
        if (level == null) {
            throw new NotFoundException();
        }
        level.delete();
        return Response.seeOther(URI.create("/admin/support-levels")).build();
    }

    private void validate(String name, String description, Integer critical, String criticalColor, Integer escalate,
            String escalateColor, Integer normal, String normalColor) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
        if (description == null || description.isBlank()) {
            throw new BadRequestException("Description is required");
        }
        if (critical == null || critical < 0) {
            throw new BadRequestException("Critical time must be zero or more");
        }
        if (criticalColor == null || criticalColor.isBlank()) {
            throw new BadRequestException("Critical color is required");
        }
        if (escalate == null || escalate < 0) {
            throw new BadRequestException("Escalate time must be zero or more");
        }
        if (escalateColor == null || escalateColor.isBlank()) {
            throw new BadRequestException("Escalate color is required");
        }
        if (normal == null || normal < 0) {
            throw new BadRequestException("Normal time must be zero or more");
        }
        if (normalColor == null || normalColor.isBlank()) {
            throw new BadRequestException("Normal color is required");
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
