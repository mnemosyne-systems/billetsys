package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Entitlement;
import ai.mnemosyne_systems.model.Level;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.Version;
import ai.mnemosyne_systems.util.AuthHelper;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;

@Path("/api/entitlements")
@Produces(MediaType.APPLICATION_JSON)
public class EntitlementApiResource {

    @GET
    @Transactional
    public EntitlementListResponse list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireAdmin(auth);
        List<Entitlement> entitlements = Entitlement
                .find("select distinct e from Entitlement e left join fetch e.supportLevels").list();
        return new EntitlementListResponse("/entitlements/new", entitlements.stream().map(this::toListItem).toList());
    }

    @GET
    @Path("/bootstrap")
    @Transactional
    public EntitlementBootstrapResponse bootstrap(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireAdmin(auth);
        return bootstrapResponse();
    }

    @GET
    @Path("/{id}")
    @Transactional
    public EntitlementDetailResponse detail(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("id") Long id) {
        requireAdmin(auth);
        Entitlement entitlement = Entitlement
                .find("select e from Entitlement e left join fetch e.supportLevels where e.id = ?1", id).firstResult();
        if (entitlement == null) {
            throw new jakarta.ws.rs.NotFoundException();
        }
        return toDetail(entitlement);
    }

    private EntitlementListItem toListItem(Entitlement entitlement) {
        List<String> supportLevelNames = entitlement.supportLevels == null ? List.of()
                : entitlement.supportLevels.stream().map(level -> level.name).toList();
        return new EntitlementListItem(entitlement.id, entitlement.name, firstLinePlainText(entitlement.description),
                supportLevelNames, "/entitlements/" + entitlement.id, "/entitlements/" + entitlement.id + "/edit");
    }

    private EntitlementDetailResponse toDetail(Entitlement entitlement) {
        EntitlementBootstrapResponse bootstrap = bootstrapResponse();
        List<VersionEntry> versions = Version.<Version> list("entitlement = ?1 order by date asc, id asc", entitlement)
                .stream().map(version -> new VersionEntry(version.id, version.name,
                        version.date == null ? null : version.date.toString()))
                .toList();
        List<Long> selectedLevelIds = entitlement.supportLevels == null ? List.of()
                : entitlement.supportLevels.stream().filter(level -> level.id != null).map(level -> level.id).toList();
        return new EntitlementDetailResponse(entitlement.id, entitlement.name, entitlement.description,
                selectedLevelIds, bootstrap.supportLevels(), versions, "/entitlements/" + entitlement.id + "/edit");
    }

    private EntitlementBootstrapResponse bootstrapResponse() {
        return new EntitlementBootstrapResponse(Level
                .<Level> list("order by name").stream().map(level -> new SupportLevelOption(level.id, level.name,
                        formatDayTime(level.fromDay, level.fromTime), formatDayTime(level.toDay, level.toTime)))
                .toList(), LocalDate.now().toString());
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
            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        return user;
    }

    public record EntitlementListResponse(String createPath, List<EntitlementListItem> items) {
    }

    public record EntitlementListItem(Long id, String name, String descriptionPreview, List<String> supportLevels,
            String detailPath, String editPath) {
    }

    public record EntitlementBootstrapResponse(List<SupportLevelOption> supportLevels, String todayDate) {
    }

    public record EntitlementDetailResponse(Long id, String name, String description, List<Long> selectedLevelIds,
            List<SupportLevelOption> supportLevels, List<VersionEntry> versions, String editPath) {
    }

    public record SupportLevelOption(Long id, String name, String fromLabel, String toLabel) {
    }

    public record VersionEntry(Long id, String name, String date) {
    }
}
