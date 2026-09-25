/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.infra;

import ai.mnemosyne_systems.model.Installation;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Installation-wide branding lookup backed by the {@code branding} cache.
 * <p>
 * The cached method takes no arguments, so all callers share a single cache entry (the installation is a singleton row,
 * no company scoping applies). Only the detached {@link BrandingSnapshot} DTO is cached — never the managed
 * {@link Installation} entity — so callers cannot hit detached-entity or lazy-loading failures.
 * <p>
 * Lives on its own bean (instead of {@link BrandingProvider}) so the cache interceptor is actually applied;
 * self-invocation within one bean would bypass it.
 */
@ApplicationScoped
public class BrandingService {

    @CacheResult(cacheName = "branding")
    public BrandingSnapshot snapshot() {
        Installation installation = Installation.find("singletonKey", "installation").firstResult();
        if (installation == null) {
            return new BrandingSnapshot(null, null, null, null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null);
        }
        String companyName = installation.name;
        if ((companyName == null || companyName.isBlank()) && installation.company != null) {
            companyName = installation.company.name;
        }
        return new BrandingSnapshot(companyName, installation.logoBase64, installation.headerFooterColor,
                installation.headersColor, installation.buttonsColor, installation.backgroundBase64,
                installation.use24HourClock, installation.adminRoleIcon, installation.supportRoleIcon,
                installation.superuserRoleIcon, installation.tamRoleIcon, installation.userRoleIcon,
                installation.externalRoleIcon, installation.adminRoleColor, installation.supportRoleColor,
                installation.superuserRoleColor, installation.tamRoleColor, installation.userRoleColor,
                installation.externalRoleColor);
    }

    /**
     * Drops the cached snapshot. Must be called after every write to the {@link Installation} row (owner settings
     * updates) so stale branding is never served.
     */
    @CacheInvalidate(cacheName = "branding")
    public void invalidate() {
    }
}
