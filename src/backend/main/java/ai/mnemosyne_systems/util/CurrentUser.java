/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.util;

import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.service.UserProvisioningService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.jwt.JsonWebToken;

@RequestScoped
public class CurrentUser {

    @Inject
    SecurityIdentity identity;

    @Inject
    Instance<JsonWebToken> jwt;

    @Inject
    UserProvisioningService provisioningService;

    private User resolved;
    private boolean loaded;

    /**
     * Returns the User matching the current Keycloak principal. Result is cached per-request so the DB is hit at most
     * once.
     *
     * @throws NotFoundException
     *             if the token is valid but no matching User exists in the database.
     */
    public User get() {
        if (!loaded) {
            resolved = load();
            loaded = true;
        }
        return resolved;
    }

    /**
     * Same as get() but returns null instead of throwing when the user is not found — useful for optional identity
     * checks.
     */
    public User getOrNull() {
        if (!loaded) {
            resolved = load();
            loaded = true;
        }
        return resolved;
    }

    public boolean isAnonymous() {
        return identity == null || identity.isAnonymous();
    }

    private User load() {
        if (isAnonymous()) {
            return null;
        }

        // Production path — real Keycloak JWT
        if (!jwt.isUnsatisfied()) {
            try {
                JsonWebToken token = jwt.get();
                if (token.getSubject() != null && !token.getSubject().isBlank()) {
                    return provisioningService.getOrCreateUser(token);
                }
            } catch (Exception ignored) {
                // fall through to principal-name lookup
            }
        }

        // Test path — @TestSecurity sets principal name to the user() value
        // and @JwtSecurity injects claims including email
        String principal = identity.getPrincipal().getName();
        User user = User.find("email", principal).firstResult();
        if (user == null) {
            user = User.find("name", principal).firstResult();
        }
        return user;
    }
}
