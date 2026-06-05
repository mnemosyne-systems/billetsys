/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.util;

import ai.mnemosyne_systems.model.User;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthHelper {

    public static final String AUTH_COOKIE = "authUserIdV3";
    public static final int INACTIVITY_TIMEOUT_SECONDS = 60 * 60;
    public static final int WARNING_LEAD_SECONDS = 5 * 60;
    public static final long INACTIVITY_TIMEOUT_MILLIS = INACTIVITY_TIMEOUT_SECONDS * 1000L;
    private static final Map<Long, ActiveSession> ACTIVE_TOKENS = new ConcurrentHashMap<>();

    private AuthHelper() {
    }

    public static User findUser(String cookieValue) {
        SessionCookie sessionCookie = parseSessionCookie(cookieValue);
        if (sessionCookie == null) {
            return null;
        }
        ActiveSession activeSession = ACTIVE_TOKENS.get(sessionCookie.userId());
        if (activeSession == null || !activeSession.token.equals(sessionCookie.token())) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now - activeSession.lastActivityAt >= INACTIVITY_TIMEOUT_MILLIS) {
            ACTIVE_TOKENS.remove(sessionCookie.userId(), activeSession);
            return null;
        }
        activeSession.lastActivityAt = now;
        User user = User.findById(sessionCookie.userId());
        if (user == null) {
            ACTIVE_TOKENS.remove(sessionCookie.userId(), activeSession);
            return null;
        }
        return user;
    }

    public static String createSessionCookieValue(User user) {
        if (user == null || user.id == null) {
            return null;
        }
        String token = UUID.randomUUID().toString();
        ACTIVE_TOKENS.put(user.id, new ActiveSession(token, System.currentTimeMillis()));
        return token + ":" + user.id;
    }

    public static void clearSession(String cookieValue) {
        SessionCookie sessionCookie = parseSessionCookie(cookieValue);
        if (sessionCookie == null) {
            return;
        }
        ActiveSession activeSession = ACTIVE_TOKENS.get(sessionCookie.userId());
        if (activeSession != null && activeSession.token.equals(sessionCookie.token())) {
            ACTIVE_TOKENS.remove(sessionCookie.userId(), activeSession);
        }
    }

    public static void setLastActivityForTesting(String cookieValue, long lastActivityAt) {
        SessionCookie sessionCookie = parseSessionCookie(cookieValue);
        if (sessionCookie == null) {
            return;
        }
        ActiveSession activeSession = ACTIVE_TOKENS.get(sessionCookie.userId());
        if (activeSession != null && activeSession.token.equals(sessionCookie.token())) {
            activeSession.lastActivityAt = lastActivityAt;
        }
    }

    public static boolean isAdmin(User user) {
        return user != null && User.TYPE_ADMIN.equalsIgnoreCase(user.type);
    }

    public static boolean isSupport(User user) {
        return user != null && User.TYPE_SUPPORT.equalsIgnoreCase(user.type);
    }

    public static boolean isTam(User user) {
        return user != null && User.TYPE_TAM.equalsIgnoreCase(user.type);
    }

    public static boolean isSuperuser(User user) {
        return user != null && User.TYPE_SUPERUSER.equalsIgnoreCase(user.type);
    }

    public static boolean isUser(User user) {
        return user != null
                && (User.TYPE_USER.equalsIgnoreCase(user.type) || User.TYPE_TAM.equalsIgnoreCase(user.type));
    }

    public static boolean isExternal(User user) {
        return user != null && User.TYPE_EXTERNAL.equalsIgnoreCase(user.type);
    }

    public static boolean isParticipant(User user) {
        return user != null && User.TYPE_PARTICIPANT.equalsIgnoreCase(user.type);
    }

    private static SessionCookie parseSessionCookie(String cookieValue) {
        if (cookieValue == null || cookieValue.isBlank()) {
            return null;
        }
        try {
            String[] parts = cookieValue.split(":", 2);
            if (parts.length != 2) {
                return null;
            }
            return new SessionCookie(parts[0], Long.parseLong(parts[1]));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static final class ActiveSession {
        private final String token;
        private volatile long lastActivityAt;

        private ActiveSession(String token, long lastActivityAt) {
            this.token = token;
            this.lastActivityAt = lastActivityAt;
        }
    }

    private record SessionCookie(String token, long userId) {
    }
}
