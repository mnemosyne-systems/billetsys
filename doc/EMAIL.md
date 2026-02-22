# Email integration

This project uses Quarkus Mailer (`quarkus-mailer`) for outgoing notifications and a multipart endpoint for incoming email ingestion.

## Configuration

Set these base properties (or environment variables):

```properties
ticket.mailer.from=${MAIL_FROM:no-reply@billetsys.local}
quarkus.mailer.mock=${MAIL_MOCK:true}
%test.quarkus.mailer.mock=true
```

- `ticket.mailer.from` is the sender address.
- `quarkus.mailer.mock=true` enables the Quarkus mock tester mailbox (default for local/runtime in this project).

### SMTP configuration (real email delivery)

Disable mock mode and configure SMTP transport:

```properties
quarkus.mailer.mock=false
quarkus.mailer.host=smtp.example.com
quarkus.mailer.port=587
quarkus.mailer.username=${MAIL_USERNAME}
quarkus.mailer.password=${MAIL_PASSWORD}
quarkus.mailer.start-tls=REQUIRED
quarkus.mailer.auth-methods=PLAIN LOGIN
quarkus.mailer.from=${MAIL_FROM:no-reply@example.com}
```

Notes:
- Use port `587` with STARTTLS (`start-tls=REQUIRED`) for most providers.
- Use port `465` only when your provider requires implicit TLS (SMTPS).
- Keep credentials in environment variables/secrets, not in committed files.
- If SMTP uses self-signed/internal certs, configure the TLS truststore in Quarkus JVM options.
- Keep `%test.quarkus.mailer.mock=true` so test suite does not require external SMTP.

The settings above are based on Quarkus mailer reference behavior.

## Outgoing notifications

Notifications are sent to all users on the ticket:
- Requester (User)
- TAM users assigned to the ticket
- Support users assigned to the ticket

Emails are sent on:
- Ticket status changes
- New ticket messages (including replies and incoming-email-created messages)

If a message has attachments, the same attachments are included in outgoing emails.

## Incoming email

Incoming messages are accepted at:

`POST /mail/incoming` (multipart/form-data)

Supported fields:
- `from` (email address)
- `subject`
- `body`
- `attachments` (0..N files)

Behavior:
- If subject contains a ticket token in the format `[A-00001]`, the body is appended as a message to that ticket.
- If no token exists in subject, a new ticket is created and the body is saved as the first message.
- Incoming email attachments are saved as message attachments.
- Incoming email is processed only when `From` resolves to a known user.
- If a ticket token is provided, `From` must belong to that ticket/company context; otherwise the mail is ignored.
- If no ticket token is provided, the sender must belong to a company; ticket creation uses that company.
- Unknown sender or sender/ticket mismatch is ignored and logged at warning level.

## Testing

### 1) Start the app in mock mode

Use default config (`quarkus.mailer.mock=true`) and start Quarkus normally.

### 2) Post an incoming email to an existing ticket

Replace `A-00001` with an actual ticket name:

```bash
curl -X POST http://localhost:8080/mail/incoming \
  -F 'from=user@mnemosyne-systems.ai' \
  -F 'subject=[A-00001] Re: update from email' \
  -F 'body=Message from incoming email' \
  -F 'attachments=@/tmp/example.txt;type=text/plain'
```

Expected result:
- HTTP `200`
- message added to that ticket
- attachment visible on the new message

### 3) Post an incoming email without ticket token

```bash
curl -X POST http://localhost:8080/mail/incoming \
  -F 'from=user@mnemosyne-systems.ai' \
  -F 'subject=Need help with my account' \
  -F 'body=Please create a ticket from this email'
```

Expected result:
- HTTP `200`
- a new ticket is created
- body becomes the first message on that ticket

### 4) Validate outgoing notifications

In mock mode, Quarkus logs outgoing mails in the app log.  
After each incoming request above, verify a corresponding `Sending email ...` entry appears.

## Email templates

Templates are Qute files in:

- `src/main/resources/templates/mail/ticket-change-subject.txt`
- `src/main/resources/templates/mail/ticket-change-body.txt`
- `src/main/resources/templates/mail/ticket-change-body.html`

Edit these files to customize email format without changing Java code.

Available data keys:
- `ticket`
- `message` (null for status-only updates)
- `eventType` (`Message` or `Status`)
- `actorName`
- `previousStatus`
- `currentStatus`
