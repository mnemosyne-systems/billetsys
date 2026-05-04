/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.model.Attachment;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.MessageAudienceSupport;
import io.quarkus.mailer.Mail;
import io.quarkus.qute.RawString;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class TicketEmailService {

    @Inject
    Mailer mailer;

    @Inject
    MarkdownService markdownService;

    @Location("mail/ticket-change-subject.txt")
    Template subjectTemplate;

    @Location("mail/ticket-change-body.txt")
    Template bodyTextTemplate;

    @Location("mail/ticket-change-body.html")
    Template bodyHtmlTemplate;

    @ConfigProperty(name = "ticket.mailer.from")
    String fromAddress;

    public void notifyMessageChange(Ticket ticket, Message message, User actor) {
        send(ticket, message, actor, "Message", null);
    }

    public void notifyStatusChange(Ticket ticket, String previousStatus, User actor) {
        send(ticket, null, actor, "Status", normalize(previousStatus));
    }

    private void send(Ticket ticket, Message message, User actor, String eventType, String previousStatus) {
        if (ticket == null || ticket.id == null) {
            return;
        }
        List<User> recipientUsers = recipientUsers(ticket, message);
        if (recipientUsers.isEmpty()) {
            return;
        }
        String actorName = actor == null ? "System" : actor.name;
        String currentStatus = computeEffectiveStatus(ticket, ticket.status);
        String subject = subjectTemplate.data("ticket", ticket).data("eventType", eventType).render();
        String text = renderText(ticket, message, eventType, previousStatus, currentStatus, actorName);
        String html = renderHtml(ticket, message, eventType, previousStatus, currentStatus, actorName);
        for (User recipient : recipientUsers) {
            Mail mail = createMail(recipient, subject, text, html);
            addAttachments(mail, message);
            mailer.send(mail);
        }
    }

    private List<User> recipientUsers(Ticket ticket, Message message) {
        Set<Long> seen = new LinkedHashSet<>();
        List<User> result = new ArrayList<>();
        MessageAudienceSupport.Audience audience = messageAudience(message);
        addUser(seen, result, ticket.requester, audience);
        if (ticket.tamUsers != null) {
            for (User user : ticket.tamUsers) {
                addUser(seen, result, user, audience);
            }
        }
        if (ticket.supportUsers != null) {
            for (User user : ticket.supportUsers) {
                addUser(seen, result, user, audience);
            }
        }
        if (audience == MessageAudienceSupport.Audience.USER_SUPERUSER && ticket.company != null
                && ticket.company.users != null) {
            for (User user : ticket.company.users) {
                addUser(seen, result, user, audience);
            }
        }
        return result;
    }

    private void addUser(Set<Long> seen, List<User> result, User user, MessageAudienceSupport.Audience audience) {
        if (user == null || user.id == null || user.email == null || user.email.isBlank()) {
            return;
        }
        if (audience != null && !MessageAudienceSupport.belongsToAudience(user, audience)) {
            return;
        }
        if (seen.add(user.id)) {
            result.add(user);
        }
    }

    private MessageAudienceSupport.Audience messageAudience(Message message) {
        if (message == null || message.isPublic) {
            return null;
        }
        return MessageAudienceSupport.audienceFor(message.author);
    }

    private String renderText(Ticket ticket, Message message, String eventType, String previousStatus,
            String currentStatus, String actorName) {
        return bodyTextTemplate.data("ticket", ticket).data("message", message).data("eventType", eventType)
                .data("previousStatus", previousStatus).data("currentStatus", currentStatus)
                .data("actorName", actorName).render();
    }

    private String renderHtml(Ticket ticket, Message message, String eventType, String previousStatus,
            String currentStatus, String actorName) {
        return bodyHtmlTemplate.data("ticket", ticket).data("message", message).data("eventType", eventType)
                .data("previousStatus", previousStatus).data("currentStatus", currentStatus)
                .data("actorName", actorName)
                .data("messageHtml",
                        message == null ? new RawString("") : new RawString(markdownService.renderHtml(message.body)))
                .render();
    }

    private Mail createMail(User recipient, String subject, String text, String html) {
        String recipientEmail = recipient.email.trim().toLowerCase();
        if ("text".equalsIgnoreCase(recipient.emailFormat)) {
            return Mail.withText(recipientEmail, subject, text).setFrom(fromAddress);
        }
        if ("html".equalsIgnoreCase(recipient.emailFormat)) {
            return Mail.withHtml(recipientEmail, subject, html).setFrom(fromAddress);
        }
        return Mail.withText(recipientEmail, subject, text).setFrom(fromAddress).setHtml(html);
    }

    private void addAttachments(Mail mail, Message message) {
        if (message == null || message.attachments == null) {
            return;
        }
        for (Attachment attachment : message.attachments) {
            if (attachment != null && attachment.name != null && attachment.data != null) {
                mail.addAttachment(attachment.name, attachment.data,
                        attachment.mimeType == null || attachment.mimeType.isBlank() ? "application/octet-stream"
                                : attachment.mimeType);
            }
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }

    public String computeEffectiveStatus(Ticket ticket, String status) {
        String normalized = status == null || status.isBlank() ? "Open" : status.trim();
        if ("Open".equalsIgnoreCase(normalized) && ticket != null && ticket.supportUsers != null
                && !ticket.supportUsers.isEmpty()) {
            return "Assigned";
        }
        return normalized;
    }
}
