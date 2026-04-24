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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

@Path("/api/password-reset")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Blocking
public class PasswordResetResource {

    private static final long TOKEN_TTL_SECONDS = 30 * 60;

    @Inject
    Mailer mailer;

    @Inject
    ObjectMapper objectMapper;

    @Context
    UriInfo uriInfo;

    @Location("mail/password-reset-subject.txt")
    Template subjectTemplate;

    @Location("mail/password-reset-body.txt")
    Template bodyTextTemplate;

    @Location("mail/password-reset-body.html")
    Template bodyHtmlTemplate;

    @ConfigProperty(name = "ticket.mailer.from")
    String fromAddress;

    @ConfigProperty(name = "cap.siteverify.url")
    Optional<String> capSiteverifyUrl;

    @ConfigProperty(name = "cap.secret.key")
    Optional<String> capSecretKey;

    @ConfigProperty(name = "app.public-base-url")
    Optional<String> publicBaseUrl;

    @POST
    @Path("/request")
    @Transactional(dontRollbackOn = WebApplicationException.class)
    public ResetRequestResponse requestReset(ResetRequestPayload payload) {
        if (payload == null || payload.getEmail() == null || payload.getEmail().isBlank()) {
            throw badRequest("Email is required");
        }

        verifyCap(payload.getCapToken());
        purgeExpired();

        String email = payload.getEmail().trim().toLowerCase();
        User user = User.find("lower(email) = ?1", email).firstResult();

        if (user == null) {
            throw badRequest("No account found with that email address");
        }

        PasswordResetToken.delete("user", user);
        PasswordResetToken resetToken = PasswordResetToken.issue(user, TOKEN_TTL_SECONDS);
        sendResetEmail(user, buildResetLink(resetToken.token));

        return new ResetRequestResponse("Password reset instructions have been sent to your email.");
    }

    @POST
    @Path("/reset")
    @Transactional(dontRollbackOn = WebApplicationException.class)
    public ResetResponse resetPassword(ResetPayload payload) {
        if (payload == null || payload.getToken() == null || payload.getToken().isBlank()) {
            throw badRequest("Reset token is required");
        }
        if (payload.getNewPassword() == null || payload.getNewPassword().isBlank()) {
            throw badRequest("New password is required");
        }
        if (!payload.getNewPassword().equals(payload.getConfirmPassword())) {
            throw badRequest("Passwords do not match");
        }

        purgeExpired();

        PasswordResetToken entry = PasswordResetToken.find("token", payload.getToken()).firstResult();
        if (entry == null || Instant.now().isAfter(entry.expiresAt)) {
            if (entry != null) {
                entry.delete();
            }
            throw badRequest("Invalid or expired reset link");
        }

        User user = entry.user;
        if (user == null) {
            throw badRequest("User not found");
        }

        user.passwordHash = BcryptUtil.bcryptHash(payload.getNewPassword());
        entry.delete();
        return new ResetResponse(true);
    }

    private void verifyCap(String capToken) {
        String siteverifyUrl = capSiteverifyUrl.orElse("");
        String secretKey = capSecretKey.orElse("");

        if (siteverifyUrl.isBlank() || secretKey.isBlank()) {
            return;
        }

        if (capToken == null || capToken.isBlank()) {
            throw badRequest("Please complete the CAPTCHA verification");
        }

        try {
            String body = "secret=" + urlEncode(secretKey) + "&response=" + urlEncode(capToken);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(siteverifyUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                    HttpResponse.BodyHandlers.ofString());
            JsonNode responseBody = objectMapper.readTree(response.body());
            if (response.statusCode() != 200 || !responseBody.path("success").asBoolean(false)) {
                throw badRequest("CAPTCHA verification failed");
            }
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (Exception ex) {
            throw badRequest("CAPTCHA verification failed");
        }
    }

    private void sendResetEmail(User user, String resetLink) {
        String subject = subjectTemplate.render();
        String text = bodyTextTemplate.data("username", user.name).data("resetLink", resetLink).render();
        String html = bodyHtmlTemplate.data("username", user.name).data("resetLink", resetLink).render();
        Mail mail = Mail.withHtml(user.email, subject, html).setFrom(fromAddress).setText(text);
        mailer.send(mail);
    }

    private void purgeExpired() {
        PasswordResetToken.delete("expiresAt <= ?1", Instant.now());
    }

    private String buildResetLink(String token) {
        String configuredBaseUrl = publicBaseUrl.orElse("").trim();
        String baseUrl = configuredBaseUrl.isBlank() ? trimTrailingSlash(uriInfo.getBaseUri().toString())
                : trimTrailingSlash(configuredBaseUrl);
        return baseUrl + "/reset-password?token=" + urlEncode(token);
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private WebApplicationException badRequest(String message) {
        return new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(message).build());
    }

    public static class ResetRequestPayload {

        private String email;
        private String capToken;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getCapToken() {
            return capToken;
        }

        public void setCapToken(String capToken) {
            this.capToken = capToken;
        }
    }

    public record ResetRequestResponse(String message) {
    }

    public static class ResetPayload {

        private String token;
        private String newPassword;
        private String confirmPassword;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

        public String getConfirmPassword() {
            return confirmPassword;
        }

        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }
    }

    public record ResetResponse(boolean updated) {
    }
}
