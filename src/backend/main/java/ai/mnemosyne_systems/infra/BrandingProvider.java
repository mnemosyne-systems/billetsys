/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.infra;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

    @Inject
    BrandingService brandingService;

    public String installationCompanyName() {
        String companyName = brandingService.snapshot().companyName();
        if (companyName != null && !companyName.isBlank()) {
            return companyName;
        }
        return "billetsys";
    }

    public String installationLogoBase64() {
        String logoBase64 = brandingService.snapshot().logoBase64();
        if (logoBase64 != null && !logoBase64.isBlank()) {
            return logoBase64;
        }
        return defaultInstallationLogoBase64();
    }

    public String installationHeaderFooterColor() {
        return normalizeInstallationColor(brandingService.snapshot().headerFooterColor());
    }

    public String installationHeadersColor() {
        return normalizeInstallationColor(brandingService.snapshot().headersColor());
    }

    public String installationButtonsColor() {
        return normalizeInstallationColor(brandingService.snapshot().buttonsColor());
    }

    public String installationBackgroundBase64() {
        String backgroundBase64 = brandingService.snapshot().backgroundBase64();
        if (backgroundBase64 == null || backgroundBase64.isBlank()) {
            return null;
        }
        return backgroundBase64;
    }

    public boolean installationUse24HourClock() {
        return Boolean.TRUE.equals(brandingService.snapshot().use24HourClock());
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
        String icon = brandingService.snapshot().adminRoleIcon();
        return (icon != null && !icon.isBlank()) ? icon : "shield-check";
    }

    public String installationSupportRoleIcon() {
        String icon = brandingService.snapshot().supportRoleIcon();
        return (icon != null && !icon.isBlank()) ? icon : "headset";
    }

    public String installationSuperuserRoleIcon() {
        String icon = brandingService.snapshot().superuserRoleIcon();
        return (icon != null && !icon.isBlank()) ? icon : "crown";
    }

    public String installationTamRoleIcon() {
        String icon = brandingService.snapshot().tamRoleIcon();
        return (icon != null && !icon.isBlank()) ? icon : "briefcase";
    }

    public String installationUserRoleIcon() {
        String icon = brandingService.snapshot().userRoleIcon();
        return (icon != null && !icon.isBlank()) ? icon : "user";
    }

    public String installationExternalRoleIcon() {
        String icon = brandingService.snapshot().externalRoleIcon();
        return (icon != null && !icon.isBlank()) ? icon : "user-star";
    }

    public String installationAdminRoleColor() {
        return normalizeRoleColor(brandingService.snapshot().adminRoleColor());
    }

    public String installationSupportRoleColor() {
        return normalizeRoleColor(brandingService.snapshot().supportRoleColor());
    }

    public String installationSuperuserRoleColor() {
        return normalizeRoleColor(brandingService.snapshot().superuserRoleColor());
    }

    public String installationTamRoleColor() {
        return normalizeRoleColor(brandingService.snapshot().tamRoleColor());
    }

    public String installationUserRoleColor() {
        return normalizeRoleColor(brandingService.snapshot().userRoleColor());
    }

    public String installationExternalRoleColor() {
        return normalizeRoleColor(brandingService.snapshot().externalRoleColor());
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
