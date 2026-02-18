/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Level;
import ai.mnemosyne_systems.model.Timezone;
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

@Path("/levels")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class LevelResource {
    public static final class ColorOption {
        private final String value;
        private final String label;
        private final String display;

        ColorOption(String value) {
            this.value = value;
            this.label = value;
            this.display = "(" + value + ")";
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }

        public String getDisplay() {
            return display;
        }
    }

    private static final List<Level.DayOption> DAY_OPTIONS = List.of(Level.DayOption.values());
    private static final List<Level.HourOption> HOUR_OPTIONS = List.of(Level.HourOption.values());
    private static final List<ColorOption> COLOR_OPTIONS = List.of(new ColorOption("Black"), new ColorOption("Silver"),
            new ColorOption("Gray"), new ColorOption("White"), new ColorOption("Maroon"), new ColorOption("Red"),
            new ColorOption("Purple"), new ColorOption("Fuchsia"), new ColorOption("Green"), new ColorOption("Lime"),
            new ColorOption("Olive"), new ColorOption("Yellow"), new ColorOption("Navy"), new ColorOption("Blue"),
            new ColorOption("Teal"), new ColorOption("Aqua"));

    @Location("level/levels.html")
    Template levelsTemplate;

    @Location("level/level-form.html")
    Template levelFormTemplate;

    @GET
    public TemplateInstance listLevels(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        List<Level> levels = Level.list("order by name");
        Map<Long, String> descriptionPreviews = new LinkedHashMap<>();
        Map<Long, String> fromValues = new LinkedHashMap<>();
        Map<Long, String> toValues = new LinkedHashMap<>();
        Map<Long, String> countryNames = new LinkedHashMap<>();
        Map<Long, String> timezoneNames = new LinkedHashMap<>();
        Map<Long, String> colorDisplays = new LinkedHashMap<>();
        for (Level level : levels) {
            if (level.id != null) {
                descriptionPreviews.put(level.id, firstLinePlainText(level.description));
                fromValues.put(level.id, formatDayTime(level.fromDay, level.fromTime));
                toValues.put(level.id, formatDayTime(level.toDay, level.toTime));
                countryNames.put(level.id, level.country != null ? level.country.name : "");
                timezoneNames.put(level.id, level.timezone != null ? level.timezone.name : "");
                colorDisplays.put(level.id, formatColorDisplay(level.color));
            }
        }
        return levelsTemplate.data("levels", levels).data("descriptionPreviews", descriptionPreviews)
                .data("fromValues", fromValues).data("toValues", toValues).data("countryNames", countryNames)
                .data("timezoneNames", timezoneNames).data("colorDisplays", colorDisplays).data("currentUser", user);
    }

    @GET
    @Path("create")
    public TemplateInstance createForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        Level level = new Level();
        level.description = "";
        level.level = 0;
        level.color = "White";
        level.fromDay = Level.DayOption.MONDAY.getCode();
        level.fromTime = Level.HourOption.H00.getCode();
        level.toDay = Level.DayOption.SUNDAY.getCode();
        level.toTime = Level.HourOption.H23.getCode();
        level.country = Country.find("code", "US").firstResult();
        level.timezone = Timezone.find("name", "America/New_York").firstResult();
        return levelFormTemplate.data("level", level).data("action", "/levels").data("dayOptions", DAY_OPTIONS)
                .data("countries", Country.list("order by name")).data("hourOptions", HOUR_OPTIONS)
                .data("colorOptions", COLOR_OPTIONS)
                .data("timezones",
                        level.country != null ? Timezone.list("country = ?1 order by name", level.country) : List.of())
                .data("title", "New level").data("currentUser", user);
    }

    @GET
    @Path("{id}/edit")
    public TemplateInstance editForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireAdmin(auth);
        Level level = Level.findById(id);
        if (level == null) {
            throw new NotFoundException();
        }
        return levelFormTemplate.data("level", level).data("action", "/levels/" + id).data("dayOptions", DAY_OPTIONS)
                .data("countries", Country.list("order by name")).data("hourOptions", HOUR_OPTIONS)
                .data("colorOptions", COLOR_OPTIONS)
                .data("timezones",
                        level.country != null ? Timezone.list("country = ?1 order by name", level.country) : List.of())
                .data("title", "Edit level").data("currentUser", user);
    }

    @POST
    @Path("")
    @Transactional
    public Response createLevel(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @FormParam("name") String name,
            @FormParam("description") String description, @FormParam("level") Integer levelValue,
            @FormParam("color") String color, @FormParam("fromDay") Integer fromDay,
            @FormParam("fromTime") Integer fromTime, @FormParam("toDay") Integer toDay,
            @FormParam("toTime") Integer toTime, @FormParam("countryId") Long countryId,
            @FormParam("timezoneId") Long timezoneId) {
        requireAdmin(auth);
        Integer normalizedFromDay = normalizeDay(fromDay, Level.DayOption.MONDAY.getCode());
        Integer normalizedFromTime = normalizeTime(fromTime, Level.HourOption.H00.getCode());
        Integer normalizedToDay = normalizeDay(toDay, Level.DayOption.SUNDAY.getCode());
        Integer normalizedToTime = normalizeTime(toTime, Level.HourOption.H23.getCode());
        String normalizedColor = normalizeColor(color, "White");
        validate(name, description, levelValue, normalizedColor, normalizedFromDay, normalizedFromTime, normalizedToDay,
                normalizedToTime);
        Level level = new Level();
        level.name = name.trim();
        level.description = description.trim();
        level.level = levelValue;
        level.color = normalizedColor;
        level.fromDay = normalizedFromDay;
        level.fromTime = normalizedFromTime;
        level.toDay = normalizedToDay;
        level.toTime = normalizedToTime;
        level.country = countryId != null ? Country.findById(countryId) : Country.find("code", "US").firstResult();
        level.timezone = timezoneId != null ? Timezone.findById(timezoneId)
                : Timezone.find("name", "America/New_York").firstResult();
        level.persist();
        return Response.seeOther(URI.create("/levels")).build();
    }

    @POST
    @Path("{id}")
    @Transactional
    public Response updateLevel(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            @FormParam("name") String name, @FormParam("description") String description,
            @FormParam("level") Integer levelValue, @FormParam("color") String color,
            @FormParam("fromDay") Integer fromDay, @FormParam("fromTime") Integer fromTime,
            @FormParam("toDay") Integer toDay, @FormParam("toTime") Integer toTime,
            @FormParam("countryId") Long countryId, @FormParam("timezoneId") Long timezoneId) {
        requireAdmin(auth);
        Level level = Level.findById(id);
        if (level == null) {
            throw new NotFoundException();
        }
        Integer normalizedFromDay = normalizeDay(fromDay,
                level.fromDay != null ? level.fromDay : Level.DayOption.MONDAY.getCode());
        Integer normalizedFromTime = normalizeTime(fromTime,
                level.fromTime != null ? level.fromTime : Level.HourOption.H00.getCode());
        Integer normalizedToDay = normalizeDay(toDay,
                level.toDay != null ? level.toDay : Level.DayOption.SUNDAY.getCode());
        Integer normalizedToTime = normalizeTime(toTime,
                level.toTime != null ? level.toTime : Level.HourOption.H23.getCode());
        String normalizedColor = normalizeColor(color, level.color != null ? level.color : "White");
        validate(name, description, levelValue, normalizedColor, normalizedFromDay, normalizedFromTime, normalizedToDay,
                normalizedToTime);
        level.name = name.trim();
        level.description = description.trim();
        level.level = levelValue;
        level.color = normalizedColor;
        level.fromDay = normalizedFromDay;
        level.fromTime = normalizedFromTime;
        level.toDay = normalizedToDay;
        level.toTime = normalizedToTime;
        level.country = countryId != null ? Country.findById(countryId) : Country.find("code", "US").firstResult();
        level.timezone = timezoneId != null ? Timezone.findById(timezoneId)
                : Timezone.find("name", "America/New_York").firstResult();
        return Response.seeOther(URI.create("/levels")).build();
    }

    @POST
    @Path("{id}/delete")
    @Transactional
    public Response deleteLevel(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireAdmin(auth);
        Level level = Level.findById(id);
        if (level == null) {
            throw new NotFoundException();
        }
        level.delete();
        return Response.seeOther(URI.create("/levels")).build();
    }

    private void validate(String name, String description, Integer levelValue, String color, Integer fromDay,
            Integer fromTime, Integer toDay, Integer toTime) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
        if (description == null || description.isBlank()) {
            throw new BadRequestException("Description is required");
        }
        if (levelValue == null || levelValue < 0) {
            throw new BadRequestException("Level must be zero or more");
        }
        if (color == null || color.isBlank()) {
            throw new BadRequestException("Color is required");
        }
        if (resolveColorOption(color) == null) {
            throw new BadRequestException("Color is invalid");
        }
        if (!Level.DayOption.isValid(fromDay)) {
            throw new BadRequestException("From day is required");
        }
        if (!Level.DayOption.isValid(toDay)) {
            throw new BadRequestException("To day is required");
        }
        if (!Level.HourOption.isValid(fromTime)) {
            throw new BadRequestException("From time is required");
        }
        if (!Level.HourOption.isValid(toTime)) {
            throw new BadRequestException("To time is required");
        }
    }

    private Integer normalizeDay(Integer value, Integer fallback) {
        if (value == null) {
            return fallback;
        }
        return value;
    }

    private Integer normalizeTime(Integer value, Integer fallback) {
        if (value == null) {
            return fallback;
        }
        return value;
    }

    private String normalizeColor(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        ColorOption option = resolveColorOption(value);
        if (option == null) {
            return fallback;
        }
        return option.getValue();
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

    private String formatColorDisplay(String color) {
        ColorOption option = resolveColorOption(color);
        return option != null ? option.getDisplay() : "";
    }

    private String dayLabel(Integer code) {
        if (code == null) {
            return "";
        }
        for (Level.DayOption option : DAY_OPTIONS) {
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
        for (Level.HourOption option : HOUR_OPTIONS) {
            if (option.getCode() == code) {
                return option.getLabel();
            }
        }
        return "";
    }

    private ColorOption resolveColorOption(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (ColorOption option : COLOR_OPTIONS) {
            if (option.getValue().equalsIgnoreCase(value.trim())) {
                return option;
            }
        }
        return null;
    }

    private User requireAdmin(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isAdmin(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }
}
