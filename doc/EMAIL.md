# Email integration

This project uses Quarkus Mailer (`quarkus-mailer`) for outgoing notifications, a multipart endpoint for incoming email ingestion, and mailbox polling for pulling messages from IMAP/POP3 mailboxes.

## Configuration

Set these base properties (or environment variables):

```properties
ticket.mailer.from=${MAIL_FROM:no-reply@billetsys.local}
quarkus.mailer.mock=${MAIL_MOCK:true}
%test.quarkus.mailer.mock=true
ticket.mail.incoming.enabled=${MAIL_INCOMING_ENABLED:true}
```

- `ticket.mailer.from` is the sender address.
- `quarkus.mailer.mock=true` enables the Quarkus mock tester mailbox (default for local/runtime in this project).
- `ticket.mail.incoming.enabled=true` keeps `POST /mail/incoming` enabled by default.

### SMTP configuration (real email delivery)

To configure real SMTP transport for outbound notifications (including password reset emails), refer to the **Administration and Configuration** section of the **Password Reset** manual chapter (`doc/manual/en/25-password-reset.md`).

### Mailbox polling configuration

To pull messages from a mailbox and turn them into ticket updates, configure:

```properties
ticket.mailbox.enabled=true
ticket.mailbox.poll-interval=5m
ticket.mailbox.protocol=imap
ticket.mailbox.host=mail.example.com
ticket.mailbox.port=993
ticket.mailbox.username=${MAILBOX_USERNAME}
ticket.mailbox.password=${MAILBOX_PASSWORD}
ticket.mailbox.folder=INBOX
ticket.mailbox.ssl=true
ticket.mailbox.starttls=false
ticket.mailbox.unread-only=true
ticket.mailbox.delete-after-process=false
```

Notes:
- `ticket.mailbox.enabled=true` turns on scheduled mailbox polling.
- `ticket.mailbox.protocol` supports standard Jakarta Mail store protocols such as `imap`, `imaps`, `pop3`, or `pop3s`.
- `ticket.mailbox.unread-only=true` means only unseen messages are pulled.
- `ticket.mailbox.delete-after-process=true` can be used if processed messages should be removed from the mailbox after successful ingestion.
- Keep mailbox credentials in environment variables or secrets, not in committed files.

## Outgoing notifications

Notifications are sent to all users on the ticket:
- Requester (User/Superuser)
- TAM users assigned to the ticket
- Support users assigned to the ticket

Emails are sent on:
- Ticket status changes
- New ticket messages (including replies and incoming-email-created messages)

If a message has attachments, the same attachments are included in outgoing emails.

## Incoming email

Incoming messages are accepted at:

`POST /mail/incoming` (multipart/form-data)

To disable this endpoint while leaving mailbox polling or outgoing email enabled, set:

```properties
ticket.mail.incoming.enabled=false
```

When disabled, requests to `POST /mail/incoming` return `404 Not Found`.

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

The same ingestion logic is reused by mailbox polling:
- The mailbox reader pulls messages from the configured folder.
- The first `From:` address is used to resolve the sender and therefore the company context.
- A subject token such as `[A-00005]` is used to find the existing ticket.
- If no subject token exists, the message creates a new ticket for the sender's company.
- Successfully processed mailbox messages are marked seen, and can optionally be deleted after processing.

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
