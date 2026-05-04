\newpage

# Password Reset

The **Password Reset** flow provides a secure, self-service way for users to regain access to their accounts if they forget their credentials.

## Purpose

To maintain security while minimizing support overhead, billetsys allows users to independently reset their passwords. This flow is protected by CAP (CAPTCHA) integration to prevent abuse, such as automated spam or enumeration attacks.

## The Reset Flow

1. **Requesting a Reset**: A user initiates the process from the login page by clicking "Forgot your password?". They are prompted to enter their email address.
2. **CAP Verification**: Before the request is accepted, the user must solve a CAP challenge. This ensures that the request is coming from a human and protects the system from automated bots.
3. **Email Delivery**: If the email matches an active account, billetsys sends an email containing the user's username and a secure, time-limited reset link. The email format follows that user's profile preference when one has been configured.
4. **Setting a New Password**: Clicking the link takes the user to a secure reset page where they can establish a new password and immediately regain access to the application.

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
