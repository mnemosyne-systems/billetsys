/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import io.smallrye.common.annotation.Blocking;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
public class UserSearchApiResource {

    @GET
    @Path("/suggest")
    @Transactional
    public UserDirectoryApiModels.UserSuggestionResponse suggest(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("q") @DefaultValue("") String q) {

        User currentUser = requireUser(auth);

        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);

        List<User> users = scopedUsers(currentUser);
        List<UserDirectoryApiModels.UserSuggestion> matches = new ArrayList<>();

        for (User user : users) {
            if (user == null || user.id == null) {
                continue;
            }

            boolean nameMatches = user.name != null && user.name.toLowerCase(Locale.ROOT).contains(needle);

            boolean fullNameMatches = user.fullName != null && user.fullName.toLowerCase(Locale.ROOT).contains(needle);

            boolean emailMatches = user.email != null && user.email.toLowerCase(Locale.ROOT).contains(needle);

            if (needle.isEmpty() || nameMatches || fullNameMatches || emailMatches) {
                matches.add(new UserDirectoryApiModels.UserSuggestion(user.id, user.getDisplayName(), user.email,
                        detailPath(currentUser, user)));
            }

            if (matches.size() >= 6) {
                break;
            }
        }

        return new UserDirectoryApiModels.UserSuggestionResponse(matches);
    }

    private List<User> scopedUsers(User currentUser) {
        if (User.TYPE_USER.equalsIgnoreCase(currentUser.type)
                || User.TYPE_SUPERUSER.equalsIgnoreCase(currentUser.type)) {

            return User
                    .<User> find(
                            "select distinct target " + "from Company c " + "join c.users member "
                                    + "join c.users target " + "where member = ?1 " + "order by target.name, target.id",
                            currentUser)
                    .list();
        }

        return User.<User> list("order by lower(name), id");
    }

    private String detailPath(User currentUser, User targetUser) {
        String requesterType = currentUser.type == null ? "" : currentUser.type.toLowerCase(Locale.ROOT);

        String targetType = targetUser.type == null ? "" : targetUser.type.toLowerCase(Locale.ROOT);

        if (User.TYPE_SUPPORT.equals(requesterType)) {
            return switch (targetType) {
                case User.TYPE_SUPPORT -> "/support/support-users/" + targetUser.id;
                case User.TYPE_TAM -> "/support/tam-users/" + targetUser.id;
                case User.TYPE_SUPERUSER -> "/support/superuser-users/" + targetUser.id;
                case User.TYPE_EXTERNAL -> "/support/externals/" + targetUser.id;
                default -> "/support/user-profiles/" + targetUser.id;
            };
        }

        if (User.TYPE_SUPERUSER.equals(requesterType)) {
            return switch (targetType) {
                case User.TYPE_SUPPORT -> "/superuser/support-users/" + targetUser.id;
                case User.TYPE_SUPERUSER -> "/superuser/superuser-users/" + targetUser.id;
                case User.TYPE_EXTERNAL -> "/superuser/externals/" + targetUser.id;
                default -> "/superuser/user-profiles/" + targetUser.id;
            };
        }

        if (User.TYPE_ADMIN.equals(requesterType)) {
            return "/users/" + targetUser.id;
        }

        if (User.TYPE_TAM.equals(requesterType) && User.TYPE_EXTERNAL.equals(targetType)) {
            return "/tam/externals/" + targetUser.id;
        }

        return switch (targetType) {
            case User.TYPE_SUPPORT -> "/user/support-users/" + targetUser.id;
            case User.TYPE_TAM -> "/user/tam-users/" + targetUser.id;
            case User.TYPE_SUPERUSER -> "/user/superuser-users/" + targetUser.id;
            case User.TYPE_EXTERNAL -> "/user/externals/" + targetUser.id;
            default -> "/user/user-profiles/" + targetUser.id;
        };
    }

    private User requireUser(String auth) {
        User user = AuthHelper.findUser(auth);
        if (user == null) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        return user;
    }
}
