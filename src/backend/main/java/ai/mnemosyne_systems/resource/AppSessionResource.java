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
import ai.mnemosyne_systems.util.CurrentUser;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import jakarta.annotation.security.PermitAll;

@Path("/api/app/session")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class AppSessionResource {

    @Inject
    BrandingProvider brandingProvider;

    @Inject
    CurrentUser currentUser;

    @GET
    public SessionResponse session() {

        String installationCompanyName = brandingProvider.installationCompanyName();
        String installationLogoBase64 = brandingProvider.installationLogoBase64();
        String installationBackgroundBase64 = brandingProvider.installationBackgroundBase64();
        String installationHeaderFooterColor = brandingProvider.installationHeaderFooterColor();
        String installationHeadersColor = brandingProvider.installationHeadersColor();
        String installationButtonsColor = brandingProvider.installationButtonsColor();
        boolean installationUse24HourClock = brandingProvider.installationUse24HourClock();
        String installationAdminRoleIcon = brandingProvider.installationAdminRoleIcon();
        String installationSupportRoleIcon = brandingProvider.installationSupportRoleIcon();
        String installationSuperuserRoleIcon = brandingProvider.installationSuperuserRoleIcon();
        String installationTamRoleIcon = brandingProvider.installationTamRoleIcon();
        String installationUserRoleIcon = brandingProvider.installationUserRoleIcon();
        String installationExternalRoleIcon = brandingProvider.installationExternalRoleIcon();
        String installationAdminRoleColor = brandingProvider.installationAdminRoleColor();
        String installationSupportRoleColor = brandingProvider.installationSupportRoleColor();
        String installationSuperuserRoleColor = brandingProvider.installationSuperuserRoleColor();
        String installationTamRoleColor = brandingProvider.installationTamRoleColor();
        String installationUserRoleColor = brandingProvider.installationUserRoleColor();
        String installationExternalRoleColor = brandingProvider.installationExternalRoleColor();

        // resolve user from Keycloak
        User user = currentUser.getOrNull();

        if (user == null) {
            return new SessionResponse(false, null, null, null, null, null, installationCompanyName,
                    installationLogoBase64, installationBackgroundBase64, installationHeaderFooterColor,
                    installationHeadersColor, installationButtonsColor, installationUse24HourClock,
                    installationAdminRoleIcon, installationSupportRoleIcon, installationSuperuserRoleIcon,
                    installationTamRoleIcon, installationUserRoleIcon, installationExternalRoleIcon,
                    installationAdminRoleColor, installationSupportRoleColor, installationSuperuserRoleColor,
                    installationTamRoleColor, installationUserRoleColor, installationExternalRoleColor,
                    AuthHelper.INACTIVITY_TIMEOUT_SECONDS, AuthHelper.WARNING_LEAD_SECONDS, null, "/login", List.of(),
                    List.of("The React shell now uses clean URLs for login, tickets, and admin pages.",
                            "Sign in to see role-aware navigation."));
        }
        return new SessionResponse(true, user.name, user.getDisplayName(), user.email, user.type, user.logoBase64,
                installationCompanyName, installationLogoBase64, installationBackgroundBase64,
                installationHeaderFooterColor, installationHeadersColor, installationButtonsColor,
                installationUse24HourClock, installationAdminRoleIcon, installationSupportRoleIcon,
                installationSuperuserRoleIcon, installationTamRoleIcon, installationUserRoleIcon,
                installationExternalRoleIcon, installationAdminRoleColor, installationSupportRoleColor,
                installationSuperuserRoleColor, installationTamRoleColor, installationUserRoleColor,
                installationExternalRoleColor, AuthHelper.INACTIVITY_TIMEOUT_SECONDS, AuthHelper.WARNING_LEAD_SECONDS,
                user.pageSize, homePath(user), navigation(user),
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
        if (user != null && User.TYPE_USER.equalsIgnoreCase(user.type)) {
            return List.of(new NavLink("Tickets", "/user/tickets"), new NavLink("Articles", "/articles"),
                    new NavLink("External", "/user/externals"), new NavLink("Profile", "/profile"));
        }
        if (AuthHelper.isSuperuser(user)) {
            return List.of(new NavLink("Tickets", "/superuser/tickets"), new NavLink("Users", "/superuser/users"),
                    new NavLink("Reports", "/reports"), new NavLink("Articles", "/articles"),
                    new NavLink("Profile", "/profile"));
        }
        if (AuthHelper.isTam(user)) {
            return List.of(new NavLink("Tickets", "/user/tickets"), new NavLink("Reports", "/reports"),
                    new NavLink("Articles", "/articles"), new NavLink("Users", "/tam/users"),
                    new NavLink("Profile", "/profile"));
        }
        return List.of(new NavLink("Tickets", "/user/tickets"), new NavLink("Articles", "/articles"),
                new NavLink("Profile", "/profile"));
    }

    public record SessionResponse(boolean authenticated, String username, String displayName, String email, String role,
            String logoBase64, String installationCompanyName, String installationLogoBase64,
            String installationBackgroundBase64, String installationHeaderFooterColor, String installationHeadersColor,
            String installationButtonsColor, boolean installationUse24HourClock, String installationAdminRoleIcon,
            String installationSupportRoleIcon, String installationSuperuserRoleIcon, String installationTamRoleIcon,
            String installationUserRoleIcon, String installationExternalRoleIcon, String installationAdminRoleColor,
            String installationSupportRoleColor, String installationSuperuserRoleColor, String installationTamRoleColor,
            String installationUserRoleColor, String installationExternalRoleColor, int inactivityTimeoutSeconds,
            int inactivityWarningSeconds, Integer defaultPageSize, String homePath, List<NavLink> navigation,
            List<String> notices) {
    }

    public record NavLink(String label, String href) {
    }
}
