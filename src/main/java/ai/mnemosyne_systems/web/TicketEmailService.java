/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.Attachment;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import io.quarkus.mailer.Mail;
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
        List<String> recipients = recipients(ticket);
        if (recipients.isEmpty()) {
            return;
        }
        String actorName = actor == null ? "System" : actor.name;
        String currentStatus = computeEffectiveStatus(ticket, ticket.status);
        String subject = subjectTemplate.data("ticket", ticket).data("eventType", eventType).render();
        String text = bodyTextTemplate.data("ticket", ticket).data("message", message).data("eventType", eventType)
                .data("previousStatus", previousStatus).data("currentStatus", currentStatus)
                .data("actorName", actorName).render();
        String html = bodyHtmlTemplate.data("ticket", ticket).data("message", message).data("eventType", eventType)
                .data("previousStatus", previousStatus).data("currentStatus", currentStatus)
                .data("actorName", actorName).render();
        Mail mail = Mail.withText(recipients.get(0), subject, text).setFrom(fromAddress).setHtml(html);
        for (int i = 1; i < recipients.size(); i++) {
            mail.addTo(recipients.get(i));
        }
        if (message != null && message.attachments != null) {
            for (Attachment attachment : message.attachments) {
                if (attachment != null && attachment.name != null && attachment.data != null) {
                    mail.addAttachment(attachment.name, attachment.data,
                            attachment.mimeType == null || attachment.mimeType.isBlank() ? "application/octet-stream"
                                    : attachment.mimeType);
                }
            }
        }
        mailer.send(mail);
    }

    private List<String> recipients(Ticket ticket) {
        Set<String> emails = new LinkedHashSet<>();
        addEmail(emails, ticket.requester);
        if (ticket.tamUsers != null) {
            for (User user : ticket.tamUsers) {
                addEmail(emails, user);
            }
        }
        if (ticket.supportUsers != null) {
            for (User user : ticket.supportUsers) {
                addEmail(emails, user);
            }
        }
        return new ArrayList<>(emails);
    }

    private void addEmail(Set<String> emails, User user) {
        if (user == null || user.email == null || user.email.isBlank()) {
            return;
        }
        emails.add(user.email.trim().toLowerCase());
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
