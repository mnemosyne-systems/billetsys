package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Level;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
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
import java.util.List;

@Path("/api/levels")
@Produces(MediaType.APPLICATION_JSON)
public class LevelApiResource {

    @GET
    @Transactional
    public LevelListResponse list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireAdmin(auth);
        List<Level> levels = Level.list("order by name");
        return new LevelListResponse("/levels/new", levels.stream().map(this::toListItem).toList());
    }

    @GET
    @Path("/bootstrap")
    @Transactional
    public LevelBootstrapResponse bootstrap(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireAdmin(auth);
        return bootstrapResponse();
    }

    @GET
    @Path("/{id}")
    @Transactional
    public LevelDetailResponse detail(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireAdmin(auth);
        Level level = Level.findById(id);
        if (level == null) {
            throw new jakarta.ws.rs.NotFoundException();
        }
        return toDetail(level);
    }

    private LevelListItem toListItem(Level level) {
        return new LevelListItem(level.id, level.name, firstLinePlainText(level.description), level.level, level.color,
                formatColorDisplay(level.color), formatDayTime(level.fromDay, level.fromTime),
                formatDayTime(level.toDay, level.toTime), level.country == null ? null : level.country.name,
                level.timezone == null ? null : level.timezone.name, "/levels/" + level.id,
                "/levels/" + level.id + "/edit");
    }

    private LevelDetailResponse toDetail(Level level) {
        LevelBootstrapResponse bootstrap = bootstrapResponse();
        return new LevelDetailResponse(level.id, level.name, level.description, level.level, level.color, level.fromDay,
                level.fromTime, level.toDay, level.toTime, level.country == null ? null : level.country.id,
                level.country == null ? null : level.country.name, level.timezone == null ? null : level.timezone.id,
                level.timezone == null ? null : level.timezone.name, formatDayTime(level.fromDay, level.fromTime),
                formatDayTime(level.toDay, level.toTime), formatColorDisplay(level.color),
                "/levels/" + level.id + "/edit", bootstrap.dayOptions(), bootstrap.hourOptions(),
                bootstrap.colorOptions(), bootstrap.countries(), bootstrap.timezones());
    }

    private LevelBootstrapResponse bootstrapResponse() {
        return new LevelBootstrapResponse(
                List.of(Level.DayOption.values()).stream()
                        .map(option -> new NumericOption(option.getCode(), option.getLabel())).toList(),
                List.of(Level.HourOption.values()).stream()
                        .map(option -> new NumericOption(option.getCode(), option.getLabel())).toList(),
                List.of("Black", "Silver", "Gray", "White", "Maroon", "Red", "Purple", "Fuchsia", "Green", "Lime",
                        "Olive", "Yellow", "Navy", "Blue", "Teal", "Aqua").stream()
                        .map(color -> new StringOption(color, "(" + color + ")")).toList(),
                Country.<Country> list("order by name").stream()
                        .map(country -> new IdNameOption(country.id, country.name)).toList(),
                Timezone.<Timezone> list("order by name").stream()
                        .map(timezone -> new TimezoneOption(timezone.id, timezone.name,
                                timezone.country == null ? null : timezone.country.id))
                        .toList(),
                "White", 0, Level.DayOption.MONDAY.getCode(), Level.HourOption.H00.getCode(),
                Level.DayOption.SUNDAY.getCode(), Level.HourOption.H23.getCode(), defaultCountryId(),
                defaultTimezoneId());
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

    private String formatColorDisplay(String color) {
        if (color == null || color.isBlank()) {
            return "";
        }
        return "(" + color + ")";
    }

    private User requireAdmin(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isAdmin(user)) {
            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        return user;
    }

    public record LevelListResponse(String createPath, List<LevelListItem> items) {
    }

    public record LevelListItem(Long id, String name, String descriptionPreview, Integer level, String color,
            String colorDisplay, String fromLabel, String toLabel, String countryName, String timezoneName,
            String detailPath, String editPath) {
    }

    public record LevelBootstrapResponse(List<NumericOption> dayOptions, List<NumericOption> hourOptions,
            List<StringOption> colorOptions, List<IdNameOption> countries, List<TimezoneOption> timezones,
            String defaultColor, Integer defaultLevel, Integer defaultFromDay, Integer defaultFromTime,
            Integer defaultToDay, Integer defaultToTime, Long defaultCountryId, Long defaultTimezoneId) {
    }

    public record LevelDetailResponse(Long id, String name, String description, Integer level, String color,
            Integer fromDay, Integer fromTime, Integer toDay, Integer toTime, Long countryId, String countryName,
            Long timezoneId, String timezoneName, String fromLabel, String toLabel, String colorDisplay,
            String editPath, List<NumericOption> dayOptions, List<NumericOption> hourOptions,
            List<StringOption> colorOptions, List<IdNameOption> countries, List<TimezoneOption> timezones) {
    }

    public record NumericOption(Integer value, String label) {
    }

    public record StringOption(String value, String label) {
    }

    public record IdNameOption(Long id, String name) {
    }

    public record TimezoneOption(Long id, String name, Long countryId) {
    }

    private Long defaultCountryId() {
        Country country = Country.find("code", "US").firstResult();
        return country == null ? null : country.id;
    }

    private Long defaultTimezoneId() {
        Timezone timezone = Timezone.find("name", "America/New_York").firstResult();
        return timezone == null ? null : timezone.id;
    }
}
