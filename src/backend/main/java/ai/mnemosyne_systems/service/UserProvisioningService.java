/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.JsonString;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class UserProvisioningService {
    @Transactional
    public User getOrCreateUser(JsonWebToken jwt) {

        String keycloakId = jwt.getSubject(); // "sub"
        String email = jwt.getClaim("email");
        String username = jwt.getClaim("preferred_username");
        String fullName = jwt.getClaim("name");
        jakarta.json.JsonObject realmAccess = jwt.getClaim("realm_access");
        List<String> roles = realmAccess != null
                ? realmAccess.getJsonArray("roles").getValuesAs(JsonString.class).stream().map(JsonString::getString)
                        .toList()
                : List.of();

        // look up by keycloakId first, fall back to email for existing
        // users that were created before keycloakId column was added
        User user = User.find("keycloakId", keycloakId).firstResult();
        if (user == null && email != null) {
            user = User.find("email", email).firstResult();
        }

        if (user != null) {
            // sync fields that may have changed in Keycloak
            user.keycloakId = keycloakId; // stamp keycloakId if missing
            user.email = email;
            user.name = username;
            user.fullName = fullName;
            user.type = resolveType(roles);
            return user;
        }

        // JIT provision new user
        user = new User();
        user.keycloakId = keycloakId;
        user.email = email;
        user.name = username;
        user.fullName = fullName;
        user.type = resolveType(roles);

        user.persist();
        return user;
    }

    private String resolveType(List<String> roles) {
        if (roles.contains("admin"))
            return User.TYPE_ADMIN;
        if (roles.contains("support"))
            return User.TYPE_SUPPORT;
        if (roles.contains("tam"))
            return User.TYPE_TAM;
        if (roles.contains("superuser"))
            return User.TYPE_SUPERUSER;
        return User.TYPE_USER;
    }
}
