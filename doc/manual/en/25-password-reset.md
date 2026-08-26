\newpage

# Password Reset

The **Password Reset** flow provides a secure, self-service way for users to regain access to their accounts if they forget their credentials. In Billetsys, user credentials and password policies are centrally managed by **Keycloak**.

## Purpose

To maintain security while minimizing support overhead, billetsys integrates with Keycloak's self-service credential management workflows. Users can independently reset or update their passwords directly through Keycloak.

## Keycloak Password Reset Flow

1. **Initiating Reset**: Click "Forgot your password?" on the login page or navigate directly to the Keycloak authentication screen.
2. **Keycloak Verification**: Keycloak prompts for your registered email address or username.
3. **Email Delivery**: Keycloak sends a time-limited password reset link to your email address (if SMTP is configured in Keycloak).
4. **Updating Password**: Clicking the secure link takes you to Keycloak's **Update Password** page where you specify your new password.

---


## Security Considerations

- **Privacy by Design**: To prevent account enumeration, the system returns a generic success message regardless of whether the email address exists in the database.
- **Time-Limited Links**: Reset tokens expire automatically, reducing the risk of unauthorized access if an email is forwarded or discovered later.
- **Single-Use Tokens**: Once a password has been successfully reset, the token is invalidated and cannot be used again.
- **User Format Preference**: Reset emails respect the account's configured email format preference, allowing delivery as HTML, plain text, or multipart email depending on the user's profile settings.

## Administration and Configuration

For the password reset flow to function correctly in a live environment, administrators must explicitly configure the CAP integration and an outbound SMTP mailer.

### CAP Configuration

The system requires a CAP service to verify requests. Configure these application environment variables:

- `CAP_API_ENDPOINT`: The public widget endpoint shown for the site/application. This is the endpoint the frontend widget calls.
- `CAP_SITEVERIFY_URL`: The matching server-side verification endpoint for that same site/application.
- `CAP_SECRET_KEY`: The server secret shown by CAP for that site/application. Keep this value private.

*Note: For local compose environments, these are typically `http://localhost:3000` and `http://localhost:3000/siteverify`.*

### SMTP Configuration

To deliver the reset emails to users, real SMTP must be configured. The development mock mailer (`quarkus.mailer.mock=true`) will not send real emails.

Configure the following variables to enable real email delivery:

```properties
# Disable the local mock mailer
quarkus.mailer.mock=false

# Sender address
ticket.mailer.from=no-reply@example.com

# Standard SMTP settings
quarkus.mailer.host=smtp.example.com
quarkus.mailer.port=587
quarkus.mailer.username=your-email@example.com
quarkus.mailer.password=your-secure-password
quarkus.mailer.start-tls=REQUIRED
quarkus.mailer.auth-methods=PLAIN LOGIN
```

- **Port 587**: Used for standard submission with STARTTLS (`start-tls=REQUIRED`).
- **Authentication**: Keep credentials secure and inject them via environment variables rather than committing them to source control.
- **Troubleshooting**: The forgot-password request will return an error if real SMTP is configured incorrectly or if the SMTP server rejects the connection.
