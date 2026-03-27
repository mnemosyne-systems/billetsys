/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.infra.BrandingProvider;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/app/session")
@Produces(MediaType.APPLICATION_JSON)
public class AppSessionResource {

    @Inject
    BrandingProvider brandingProvider;

    @GET
    public SessionResponse session(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        String installationCompanyName = brandingProvider.installationCompanyName();
        User user = AuthHelper.findUser(auth);
        if (user == null) {
            return new SessionResponse(false, null, null, null, null, null, installationCompanyName, "/login",
                    List.of(), List.of("The React shell now uses clean URLs for login, tickets, and admin pages.",
                            "Sign in to see role-aware navigation."));
        }
        return new SessionResponse(true, user.name, user.getDisplayName(), user.email, user.type, user.logoBase64,
                installationCompanyName, homePath(user), navigation(user),
                List.of("The React shell now covers tickets, admin management, profile, and reports.",
                        "Legacy page routes now redirect into the React screens for the same workflows."));
    }

    private String homePath(User user) {
        if (AuthHelper.isSupport(user)) {
            return "/support/tickets";
        }
        if (AuthHelper.isSuperuser(user)) {
            return "/superuser/tickets";
        }
        if (AuthHelper.isTam(user) || AuthHelper.isUser(user)) {
            return "/user/tickets";
        }
        return "/";
    }

    private List<NavLink> navigation(User user) {
        if (AuthHelper.isAdmin(user)) {
            return List.of(new NavLink("Dashboard", "/"), new NavLink("Owner", "/owner"),
                    new NavLink("Companies", "/companies"), new NavLink("Tickets", "/tickets"),
                    new NavLink("Articles", "/articles"), new NavLink("Categories", "/categories"),
                    new NavLink("Entitlements", "/entitlements"), new NavLink("Levels", "/levels"),
                    new NavLink("Users", "/users"), new NavLink("Profile", "/profile"),
                    new NavLink("Reports", "/reports"));
        }
        if (AuthHelper.isSupport(user)) {
            return List.of(new NavLink("Tickets", "/support/tickets"), new NavLink("Articles", "/articles"),
                    new NavLink("Users", "/support/users"));
        }
        if (AuthHelper.isSuperuser(user)) {
            return List.of(new NavLink("Tickets", "/superuser/tickets"), new NavLink("Users", "/superuser/users"),
                    new NavLink("Reports", "/reports"), new NavLink("Articles", "/articles"),
                    new NavLink("Profile", "/profile"));
        }
        if (AuthHelper.isTam(user)) {
            return List.of(new NavLink("Tickets", "/user/tickets"), new NavLink("Reports", "/reports"),
                    new NavLink("Articles", "/articles"), new NavLink("Profile", "/profile"));
        }
        return List.of(new NavLink("Tickets", "/user/tickets"), new NavLink("Articles", "/articles"),
                new NavLink("Profile", "/profile"));
    }

    public record SessionResponse(boolean authenticated, String username, String displayName, String email, String role,
            String logoBase64, String installationCompanyName, String homePath, List<NavLink> navigation,
            List<String> notices) {
    }

    public record NavLink(String label, String href) {
    }
}
