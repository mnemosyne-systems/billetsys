/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.Entitlement;
import ai.mnemosyne_systems.model.Level;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/entitlements")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class EntitlementResource {

    @Location("entitlement/entitlements.html")
    Template entitlementsTemplate;

    @Location("entitlement/entitlement-form.html")
    Template entitlementFormTemplate;

    @Location("entitlement/entitlement-view.html")
    Template entitlementViewTemplate;

    @GET
    public TemplateInstance listEntitlements(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        List<Entitlement> entitlements = Entitlement
                .find("select distinct e from Entitlement e left join fetch e.supportLevels").list();
        Map<Long, String> descriptionPreviews = new LinkedHashMap<>();
        for (Entitlement entitlement : entitlements) {
            if (entitlement.id != null) {
                descriptionPreviews.put(entitlement.id, firstLinePlainText(entitlement.description));
            }
        }
        return entitlementsTemplate.data("entitlements", entitlements).data("descriptionPreviews", descriptionPreviews)
                .data("currentUser", user);
    }

    @GET
    @Path("create")
    public TemplateInstance createForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        Entitlement entitlement = new Entitlement();
        entitlement.description = "";
        entitlement.supportLevels = java.util.List.of();
        return entitlementFormTemplate.data("entitlement", entitlement).data("action", "/entitlements")
                .data("supportLevels", Level.listAll()).data("selectedLevelIds", java.util.Set.of())
                .data("title", "New entitlement").data("currentUser", user);
    }

    @GET
    @Path("{id}")
    public TemplateInstance viewEntitlement(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        User user = requireAdmin(auth);
        Entitlement entitlement = Entitlement
                .find("select e from Entitlement e left join fetch e.supportLevels where e.id = ?1", id).firstResult();
        if (entitlement == null) {
            throw new NotFoundException();
        }
        Map<Long, String> fromValues = new LinkedHashMap<>();
        Map<Long, String> toValues = new LinkedHashMap<>();
        if (entitlement.supportLevels != null) {
            for (Level level : entitlement.supportLevels) {
                if (level != null && level.id != null) {
                    fromValues.put(level.id, formatDayTime(level.fromDay, level.fromTime));
                    toValues.put(level.id, formatDayTime(level.toDay, level.toTime));
                }
            }
        }
        return entitlementViewTemplate.data("entitlement", entitlement).data("fromValues", fromValues)
                .data("toValues", toValues).data("currentUser", user);
    }

    @GET
    @Path("{id}/edit")
    public TemplateInstance editForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireAdmin(auth);
        Entitlement entitlement = Entitlement
                .find("select e from Entitlement e left join fetch e.supportLevels where e.id = ?1", id).firstResult();
        if (entitlement == null) {
            throw new NotFoundException();
        }
        java.util.Set<Long> selectedLevelIds = new java.util.LinkedHashSet<>();
        if (entitlement.supportLevels != null) {
            for (Level level : entitlement.supportLevels) {
                if (level != null && level.id != null) {
                    selectedLevelIds.add(level.id);
                }
            }
        }
        return entitlementFormTemplate.data("entitlement", entitlement).data("action", "/entitlements/" + id)
                .data("supportLevels", Level.listAll()).data("selectedLevelIds", selectedLevelIds)
                .data("title", "Edit entitlement").data("currentUser", user);
    }

    @POST
    @Path("")
    @Transactional
    public Response createEntitlement(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @FormParam("name") String name,
            @FormParam("description") String description, @FormParam("levelIds") List<Long> levelIds) {
        requireAdmin(auth);
        validate(name, description);
        Entitlement entitlement = new Entitlement();
        entitlement.name = name.trim();
        entitlement.description = description.trim();
        entitlement.supportLevels = resolveLevels(levelIds);
        entitlement.persist();
        return Response.seeOther(URI.create("/entitlements")).build();
    }

    @POST
    @Path("{id}")
    @Transactional
    public Response updateEntitlement(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            @FormParam("name") String name, @FormParam("description") String description,
            @FormParam("levelIds") List<Long> levelIds) {
        requireAdmin(auth);
        Entitlement entitlement = Entitlement
                .find("select e from Entitlement e left join fetch e.supportLevels where e.id = ?1", id).firstResult();
        if (entitlement == null) {
            throw new NotFoundException();
        }
        validate(name, description);
        entitlement.name = name.trim();
        entitlement.description = description.trim();
        entitlement.supportLevels = resolveLevels(levelIds);
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

    private List<Level> resolveLevels(List<Long> levelIds) {
        if (levelIds == null || levelIds.isEmpty()) {
            return java.util.List.of();
        }
        return Level.list("id in ?1", levelIds);
    }

    private String firstLinePlainText(String description) {
        if (description == null || description.isBlank()) {
            return "";
        }
        String firstLine = description.replace("\r\n", "\n").split("\n", 2)[0].trim();
        firstLine = firstLine.replaceAll("\\[([^\\]]+)]\\(([^)]+)\\)", "$1");
        firstLine = firstLine.replaceAll("^```[a-zA-Z0-9_+\\-]*\\s*", "");
        firstLine = firstLine.replace("```", "");
        firstLine = firstLine.replaceAll("^[>#*\\-\\s]+", "");
        firstLine = firstLine.replace("**", "").replace("__", "").replace("`", "").replace("*", "").replace("_", "");
        return firstLine.replaceAll("\\s+", " ").trim();
    }

    private String formatDayTime(Integer dayCode, Integer timeCode) {
        return dayLabel(dayCode) + " (" + hourLabel(timeCode) + ")";
    }

    private String dayLabel(Integer code) {
        if (code == null) {
            return "";
        }
        for (Level.DayOption option : Level.DayOption.values()) {
            if (option.getCode() == code) {
                return option.getLabel();
            }
        }
        return "";
    }

    private String hourLabel(Integer code) {
        if (code == null) {
            return "";
        }
        for (Level.HourOption option : Level.HourOption.values()) {
            if (option.getCode() == code) {
                return option.getLabel();
            }
        }
        return "";
    }

    private User requireAdmin(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isAdmin(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }
}
