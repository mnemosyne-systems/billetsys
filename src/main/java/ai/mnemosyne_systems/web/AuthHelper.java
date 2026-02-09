/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.User;
import java.util.UUID;

public final class AuthHelper {

    public static final String AUTH_COOKIE = "authUserIdV3";
    public static final String SESSION_NONCE = UUID.randomUUID().toString();

    private AuthHelper() {
    }

    public static User findUser(String cookieValue) {
        if (cookieValue == null || cookieValue.isBlank()) {
            return null;
        }
        try {
            String[] parts = cookieValue.split(":", 2);
            if (parts.length != 2 || !SESSION_NONCE.equals(parts[0])) {
                return null;
            }
            long id = Long.parseLong(parts[1]);
            return User.findById(id);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static boolean isAdmin(User user) {
        return user != null && User.TYPE_ADMIN.equalsIgnoreCase(user.type);
    }

    public static boolean isSupport(User user) {
        return user != null && User.TYPE_SUPPORT.equalsIgnoreCase(user.type);
    }

    public static boolean isUser(User user) {
        return user != null
                && (User.TYPE_USER.equalsIgnoreCase(user.type) || User.TYPE_TAM.equalsIgnoreCase(user.type));
    }
}
