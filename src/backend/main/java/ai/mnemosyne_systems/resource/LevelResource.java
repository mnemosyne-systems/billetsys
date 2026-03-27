/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Level;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
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
import java.util.List;

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

    @GET
    public Response listLevels(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireAdmin(auth);
        return Response.seeOther(URI.create("/levels")).build();
    }

    @GET
    @Path("create")
    public Response createForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireAdmin(auth);
        return Response.seeOther(URI.create("/levels/new")).build();
    }

    @GET
    @Path("{id}/edit")
    public Response editForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireAdmin(auth);
        if (Level.findById(id) == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/levels/" + id + "/edit")).build();
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
