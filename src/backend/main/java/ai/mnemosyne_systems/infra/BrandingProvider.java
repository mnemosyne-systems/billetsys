/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.infra;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.Installation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;

@Named("branding")
@ApplicationScoped
public class BrandingProvider {

    private static final String DEFAULT_INSTALLATION_LOGO_PATH = "doc/logo/logo.svg";
    public static final String DEFAULT_INSTALLATION_COLOR = "#b00020";
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9a-fA-F]{6}$");

    public String installationCompanyName() {
        Installation installation = installation();
        if (installation != null) {
            if (installation.name != null && !installation.name.isBlank()) {
                return installation.name;
            }
            if (installation.company != null && installation.company.name != null
                    && !installation.company.name.isBlank()) {
                return installation.company.name;
            }
        }
        return "billetsys";
    }

    public String installationLogoBase64() {
        Installation installation = installation();
        if (installation != null && installation.logoBase64 != null && !installation.logoBase64.isBlank()) {
            return installation.logoBase64;
        }
        return defaultInstallationLogoBase64();
    }

    public String installationHeaderFooterColor() {
        Installation installation = installation();
        return normalizeInstallationColor(installation == null ? null : installation.headerFooterColor);
    }

    public String installationHeadersColor() {
        Installation installation = installation();
        return normalizeInstallationColor(installation == null ? null : installation.headersColor);
    }

    public String installationButtonsColor() {
        Installation installation = installation();
        return normalizeInstallationColor(installation == null ? null : installation.buttonsColor);
    }

    public String installationBackgroundBase64() {
        Installation installation = installation();
        if (installation == null || installation.backgroundBase64 == null || installation.backgroundBase64.isBlank()) {
            return null;
        }
        return installation.backgroundBase64;
    }

    public boolean installationUse24HourClock() {
        Installation installation = installation();
        return installation != null && Boolean.TRUE.equals(installation.use24HourClock);
    }

    public String defaultInstallationLogoBase64() {
        try {
            Path repoLogo = Path.of(DEFAULT_INSTALLATION_LOGO_PATH);
            if (Files.exists(repoLogo)) {
                return svgDataUri(Files.readAllBytes(repoLogo));
            }
            try (InputStream stream = BrandingProvider.class.getResourceAsStream("/branding/logo.svg")) {
                if (stream == null) {
                    throw new IllegalStateException("Default installation logo resource is missing");
                }
                return svgDataUri(stream.readAllBytes());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load default installation logo", e);
        }
    }

    public String installationAdminRoleIcon() {
        Installation installation = installation();
        return (installation != null && installation.adminRoleIcon != null && !installation.adminRoleIcon.isBlank())
                ? installation.adminRoleIcon
                : "shield-check";
    }

    public String installationSupportRoleIcon() {
        Installation installation = installation();
        return (installation != null && installation.supportRoleIcon != null && !installation.supportRoleIcon.isBlank())
                ? installation.supportRoleIcon
                : "headset";
    }

    public String installationSuperuserRoleIcon() {
        Installation installation = installation();
        return (installation != null && installation.superuserRoleIcon != null
                && !installation.superuserRoleIcon.isBlank()) ? installation.superuserRoleIcon : "crown";
    }

    public String installationTamRoleIcon() {
        Installation installation = installation();
        return (installation != null && installation.tamRoleIcon != null && !installation.tamRoleIcon.isBlank())
                ? installation.tamRoleIcon
                : "briefcase";
    }

    public String installationUserRoleIcon() {
        Installation installation = installation();
        return (installation != null && installation.userRoleIcon != null && !installation.userRoleIcon.isBlank())
                ? installation.userRoleIcon
                : "user";
    }

    public String installationExternalRoleIcon() {
        Installation installation = installation();
        return (installation != null && installation.externalRoleIcon != null
                && !installation.externalRoleIcon.isBlank()) ? installation.externalRoleIcon : "user-star";
    }

    public String installationAdminRoleColor() {
        Installation installation = installation();
        return normalizeRoleColor(installation == null ? null : installation.adminRoleColor);
    }

    public String installationSupportRoleColor() {
        Installation installation = installation();
        return normalizeRoleColor(installation == null ? null : installation.supportRoleColor);
    }

    public String installationSuperuserRoleColor() {
        Installation installation = installation();
        return normalizeRoleColor(installation == null ? null : installation.superuserRoleColor);
    }

    public String installationTamRoleColor() {
        Installation installation = installation();
        return normalizeRoleColor(installation == null ? null : installation.tamRoleColor);
    }

    public String installationUserRoleColor() {
        Installation installation = installation();
        return normalizeRoleColor(installation == null ? null : installation.userRoleColor);
    }

    public String installationExternalRoleColor() {
        Installation installation = installation();
        return normalizeRoleColor(installation == null ? null : installation.externalRoleColor);
    }

    private Installation installation() {
        return Installation.find("singletonKey", "installation").firstResult();
    }

    public static String normalizeInstallationColor(String color) {
        if (color == null || color.isBlank()) {
            return DEFAULT_INSTALLATION_COLOR;
        }
        String normalized = color.trim();
        if (!HEX_COLOR_PATTERN.matcher(normalized).matches()) {
            return DEFAULT_INSTALLATION_COLOR;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    public static String normalizeRoleColor(String color) {
        if (color == null || color.isBlank()) {
            return null;
        }
        String normalized = color.trim();
        if (!HEX_COLOR_PATTERN.matcher(normalized).matches()) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    public static boolean isValidInstallationColor(String color) {
        return color != null && HEX_COLOR_PATTERN.matcher(color.trim()).matches();
    }

    private String svgDataUri(byte[] svgBytes) {
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svgBytes);
    }
}
