/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.infra;

/**
 * Detached, immutable copy of the installation-wide branding values.
 * <p>
 * Cached as a whole via {@link BrandingService} so every page render and session lookup resolves branding from a single
 * cache entry instead of ~19 sequential {@code Installation} queries. Raw (possibly {@code null}) values only —
 * {@link BrandingProvider} keeps applying its fallback/default logic.
 */
public record BrandingSnapshot(String companyName, String logoBase64, String headerFooterColor, String headersColor,
        String buttonsColor, String backgroundBase64, Boolean use24HourClock, String adminRoleIcon,
        String supportRoleIcon, String superuserRoleIcon, String tamRoleIcon, String userRoleIcon,
        String externalRoleIcon, String adminRoleColor, String supportRoleColor, String superuserRoleColor,
        String tamRoleColor, String userRoleColor, String externalRoleColor) {
}
