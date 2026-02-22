/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.User;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthHelper {

    public static final String AUTH_COOKIE = "authUserIdV3";
    private static final Map<Long, String> ACTIVE_TOKENS = new ConcurrentHashMap<>();

    private AuthHelper() {
    }

    public static User findUser(String cookieValue) {
        if (cookieValue == null || cookieValue.isBlank()) {
            return null;
        }
        try {
            String[] parts = cookieValue.split(":", 2);
            if (parts.length != 2) {
                return null;
            }
            String token = parts[0];
            long id = Long.parseLong(parts[1]);
            String activeToken = ACTIVE_TOKENS.get(id);
            if (activeToken == null || !activeToken.equals(token)) {
                return null;
            }
            return User.findById(id);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static String createSessionCookieValue(User user) {
        if (user == null || user.id == null) {
            return null;
        }
        String token = UUID.randomUUID().toString();
        ACTIVE_TOKENS.put(user.id, token);
        return token + ":" + user.id;
    }

    public static void clearSession(String cookieValue) {
        if (cookieValue == null || cookieValue.isBlank()) {
            return;
        }
        try {
            String[] parts = cookieValue.split(":", 2);
            if (parts.length != 2) {
                return;
            }
            String token = parts[0];
            long id = Long.parseLong(parts[1]);
            ACTIVE_TOKENS.remove(id, token);
        } catch (NumberFormatException ex) {
            // Ignore malformed cookie value.
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
