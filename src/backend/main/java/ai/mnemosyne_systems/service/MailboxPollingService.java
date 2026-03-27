/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.model.Attachment;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.search.FlagTerm;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MailboxPollingService {

    private static final Logger LOGGER = Logger.getLogger(MailboxPollingService.class);

    @Inject
    IncomingEmailService incomingEmailService;

    @ConfigProperty(name = "ticket.mailbox.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "ticket.mailbox.protocol", defaultValue = "imap")
    String protocol;

    @ConfigProperty(name = "ticket.mailbox.host")
    Optional<String> host;

    @ConfigProperty(name = "ticket.mailbox.port", defaultValue = "993")
    int port;

    @ConfigProperty(name = "ticket.mailbox.username")
    Optional<String> username;

    @ConfigProperty(name = "ticket.mailbox.password")
    Optional<String> password;

    @ConfigProperty(name = "ticket.mailbox.folder", defaultValue = "INBOX")
    String folderName;

    @ConfigProperty(name = "ticket.mailbox.ssl", defaultValue = "true")
    boolean ssl;

    @ConfigProperty(name = "ticket.mailbox.starttls", defaultValue = "false")
    boolean startTls;

    @ConfigProperty(name = "ticket.mailbox.unread-only", defaultValue = "true")
    boolean unreadOnly;

    @ConfigProperty(name = "ticket.mailbox.delete-after-process", defaultValue = "false")
    boolean deleteAfterProcess;

    @Scheduled(every = "{ticket.mailbox.poll-interval}")
    void scheduledPoll() {
        if (!enabled) {
            return;
        }
        pollMailbox();
    }

    public int pollMailbox() {
        if (!enabled) {
            return 0;
        }
        String mailboxHost = host.map(String::trim).orElse("");
        String mailboxUsername = username.map(String::trim).orElse("");
        String mailboxPassword = password.orElse("");
        if (mailboxHost.isBlank() || mailboxUsername.isBlank() || mailboxPassword.isBlank()) {
            LOGGER.warn(
                    "Skipping mailbox poll: mailbox is enabled but host/username/password are not fully configured");
            return 0;
        }
        String normalizedProtocol = protocol == null || protocol.isBlank() ? "imap" : protocol.trim().toLowerCase();
        Session session = Session.getInstance(mailProperties(normalizedProtocol, mailboxHost));
        Folder folder = null;
        try (Store store = session.getStore(normalizedProtocol)) {
            store.connect(mailboxHost, port, mailboxUsername, mailboxPassword);
            folder = store.getFolder(folderName.trim().isEmpty() ? "INBOX" : folderName.trim());
            folder.open(Folder.READ_WRITE);
            Message[] messages = unreadOnly ? folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false))
                    : folder.getMessages();
            int processed = 0;
            for (Message message : messages) {
                try {
                    processMailboxMessage(message);
                    message.setFlag(Flags.Flag.SEEN, true);
                    if (deleteAfterProcess) {
                        message.setFlag(Flags.Flag.DELETED, true);
                    }
                    processed++;
                } catch (Exception e) {
                    LOGGER.errorf(e, "Failed to process mailbox message with subject '%s'", safeSubject(message));
                }
            }
            return processed;
        } catch (MessagingException e) {
            LOGGER.error("Failed to poll mailbox", e);
            return 0;
        } finally {
            if (folder != null && folder.isOpen()) {
                try {
                    folder.close(deleteAfterProcess);
                } catch (MessagingException e) {
                    LOGGER.warn("Failed to close mailbox folder", e);
                }
            }
        }
    }

    public void processMailboxMessage(Message message) throws MessagingException, IOException {
        if (message == null) {
            return;
        }
        MailboxMessage mailboxMessage = extractMailboxMessage(message);
        incomingEmailService.processIncomingEmail(mailboxMessage.from(), mailboxMessage.subject(),
                mailboxMessage.body(), mailboxMessage.attachments());
    }

    private Properties mailProperties(String normalizedProtocol, String mailboxHost) {
        Properties properties = new Properties();
        properties.setProperty("mail.store.protocol", normalizedProtocol);
        String prefix = "mail." + normalizedProtocol;
        properties.setProperty(prefix + ".host", mailboxHost);
        properties.setProperty(prefix + ".port", Integer.toString(port));
        properties.setProperty(prefix + ".connectiontimeout", "10000");
        properties.setProperty(prefix + ".timeout", "10000");
        if (ssl) {
            properties.setProperty(prefix + ".ssl.enable", "true");
        }
        if (startTls) {
            properties.setProperty(prefix + ".starttls.enable", "true");
        }
        return properties;
    }

    private MailboxMessage extractMailboxMessage(Message message) throws MessagingException, IOException {
        List<Attachment> attachments = new ArrayList<>();
        MailContent content = extractContent(message, attachments);
        return new MailboxMessage(extractFrom(message), message.getSubject(), content.body(), attachments);
    }

    private MailContent extractContent(Part part, List<Attachment> attachments) throws MessagingException, IOException {
        if (part.isMimeType("text/plain") && !isAttachment(part)) {
            return new MailContent(readText(part.getInputStream()));
        }
        if (part.isMimeType("text/html") && !isAttachment(part)) {
            return new MailContent(stripHtml(readText(part.getInputStream())));
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            String plainText = null;
            String htmlFallback = null;
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (isAttachment(bodyPart)) {
                    Attachment attachment = toAttachment(bodyPart);
                    if (attachment != null) {
                        attachments.add(attachment);
                    }
                    continue;
                }
                MailContent nested = extractContent(bodyPart, attachments);
                if (!nested.body().isBlank()) {
                    if (bodyPart.isMimeType("text/plain")) {
                        if (plainText == null) {
                            plainText = nested.body();
                        }
                        continue;
                    }
                    if (htmlFallback == null) {
                        htmlFallback = nested.body();
                    }
                }
            }
            if (plainText != null) {
                return new MailContent(plainText);
            }
            return new MailContent(htmlFallback == null ? "" : htmlFallback);
        }
        if (part.isMimeType("message/rfc822")) {
            Object content = part.getContent();
            if (content instanceof Part nestedPart) {
                return extractContent(nestedPart, attachments);
            }
        }
        if (isAttachment(part)) {
            Attachment attachment = toAttachment(part);
            if (attachment != null) {
                attachments.add(attachment);
            }
        }
        return new MailContent("");
    }

    private boolean isAttachment(Part part) throws MessagingException {
        String disposition = part.getDisposition();
        return Part.ATTACHMENT.equalsIgnoreCase(disposition) || Part.INLINE.equalsIgnoreCase(disposition)
                || (part.getFileName() != null && !part.getFileName().isBlank());
    }

    private Attachment toAttachment(Part part) throws MessagingException, IOException {
        byte[] data = part.getInputStream().readAllBytes();
        if (data.length == 0) {
            return null;
        }
        Attachment attachment = new Attachment();
        attachment.name = decodeFileName(part.getFileName());
        attachment.mimeType = normalizeContentType(part.getContentType());
        attachment.data = data;
        return attachment;
    }

    private String decodeFileName(String value) {
        if (value == null || value.isBlank()) {
            return "attachment";
        }
        try {
            return MimeUtility.decodeText(value);
        } catch (Exception e) {
            return value;
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        int separator = contentType.indexOf(';');
        return separator >= 0 ? contentType.substring(0, separator).trim() : contentType.trim();
    }

    private String extractFrom(Message message) throws MessagingException {
        if (message == null || message.getFrom() == null || message.getFrom().length == 0) {
            return null;
        }
        if (message.getFrom()[0] instanceof InternetAddress address) {
            return address.getAddress();
        }
        return message.getFrom()[0].toString();
    }

    private String readText(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String stripHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return html.replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("(?i)</p>", "\n\n").replaceAll("(?i)</div>", "\n")
                .replaceAll("<[^>]+>", " ").replace("&nbsp;", " ").replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll(" *\n *", "\n").trim();
    }

    private String safeSubject(Message message) {
        try {
            return message == null ? null : message.getSubject();
        } catch (MessagingException e) {
            return null;
        }
    }

    private record MailboxMessage(String from, String subject, String body, List<Attachment> attachments) {
    }

    private record MailContent(String body) {
    }
}
