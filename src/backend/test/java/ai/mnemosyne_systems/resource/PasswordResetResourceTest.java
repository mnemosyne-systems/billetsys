/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.PasswordResetToken;
import ai.mnemosyne_systems.model.User;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.mailer.Mail;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(PasswordResetResourceTest.PasswordResetProfile.class)
class PasswordResetResourceTest extends AccessTestSupport {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([A-Za-z0-9%\\-]+)");

    @Inject
    PasswordResetResource passwordResetResource;

    @Test
    void requestResetSendsUsernameAndAbsoluteResetLink() {
        PasswordResetResource.ResetRequestResponse response = passwordResetResource
                .requestReset(requestPayload("user1@mnemosyne-systems.ai"));

        Assertions.assertEquals("Password reset instructions have been sent to your email.", response.message());
        Mail mail = latestMailTo("user1@mnemosyne-systems.ai");
        Assertions.assertTrue(mail.getText().contains("Your username is: user1"));
        Assertions.assertTrue(mail.getText().contains("http://localhost:8081/reset-password?token="));
        Assertions.assertTrue(mail.getHtml().contains("http://localhost:8081/reset-password?token="));
    }

    @Test
    void resetPasswordUpdatesPasswordAndDeletesToken() {
        requestReset("user2@mnemosyne-systems.ai");
        String token = extractToken(latestMailTo("user2@mnemosyne-systems.ai"));

        PasswordResetResource.ResetResponse response = passwordResetResource
                .resetPassword(resetPayload(token, "new-pass", "new-pass"));

        Assertions.assertTrue(response.updated());
        User refreshedUser = refreshedUser(findUserId("user2@mnemosyne-systems.ai"));
        Assertions.assertTrue(BcryptUtil.matches("new-pass", refreshedUser.passwordHash));
        Assertions.assertFalse(BcryptUtil.matches("user2", refreshedUser.passwordHash));
        Assertions.assertNull(findToken(token));
    }

    @Test
    void resetPasswordRejectsExpiredToken() {
        requestReset("userb@mnemosyne-systems.ai");
        String token = extractToken(latestMailTo("userb@mnemosyne-systems.ai"));
        expireToken(token);

        WebApplicationException exception = Assertions.assertThrows(WebApplicationException.class,
                () -> passwordResetResource.resetPassword(resetPayload(token, "new-pass", "new-pass")));

        Assertions.assertEquals(400, exception.getResponse().getStatus());
        Assertions.assertNull(findToken(token));
    }

    private void requestReset(String email) {
        PasswordResetResource.ResetRequestResponse response = passwordResetResource.requestReset(requestPayload(email));
        Assertions.assertEquals("Password reset instructions have been sent to your email.", response.message());
    }

    private PasswordResetResource.ResetRequestPayload requestPayload(String email) {
        PasswordResetResource.ResetRequestPayload payload = new PasswordResetResource.ResetRequestPayload();
        payload.setEmail(email);
        payload.setCapToken("");
        return payload;
    }

    private PasswordResetResource.ResetPayload resetPayload(String token, String newPassword, String confirmPassword) {
        PasswordResetResource.ResetPayload payload = new PasswordResetResource.ResetPayload();
        payload.setToken(token);
        payload.setNewPassword(newPassword);
        payload.setConfirmPassword(confirmPassword);
        return payload;
    }

    private String extractToken(Mail mail) {
        Matcher matcher = TOKEN_PATTERN.matcher(mail.getText());
        Assertions.assertTrue(matcher.find(), "Reset token not found in mail body");
        return matcher.group(1);
    }

    @Transactional
    void expireToken(String token) {
        PasswordResetToken resetToken = PasswordResetToken.find("token", token).firstResult();
        Assertions.assertNotNull(resetToken);
        resetToken.expiresAt = Instant.now().minusSeconds(5);
    }

    @Transactional
    PasswordResetToken findToken(String token) {
        return PasswordResetToken.find("token", token).firstResult();
    }

    @Transactional
    Long findUserId(String email) {
        User user = User.find("email", email).firstResult();
        Assertions.assertNotNull(user);
        return user.id;
    }

    public static class PasswordResetProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("app.public-base-url", "http://localhost:8081", "cap.api.endpoint", "", "cap.siteverify.url",
                    "", "cap.secret.key", "");
        }
    }
}
