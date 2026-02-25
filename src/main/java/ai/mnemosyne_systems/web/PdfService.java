package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.*;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.enterprise.context.ApplicationScoped;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class PdfService {
    private final Color red = new Color(176, 0, 32);
    private final Color lightRedFontColor = new Color(178, 15, 30);
    private final Color lightRed = new Color(244, 235, 236);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd yyyy, h.mma");

    public byte[] generateTicketPdf(Ticket ticket) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();
            Color color = getTicketColor(ticket);
            Font titleFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 16);
            Font normalFont = FontFactory.getFont(FontFactory.COURIER, 12);
            // Pdf title
            Chunk titleChunk = new Chunk("Ticket: " + ticket.name, titleFont);
            titleChunk.setBackground(color);
            Paragraph title = new Paragraph(titleChunk);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            // Reason for SLA
            Message lastMessage = getLastMessage(ticket.messages);
            Long overDue = getOverdue(ticket.companyEntitlement.supportLevel, lastMessage);
            String lastMessageText;
            if (overDue < 0) {
                lastMessageText = String.format("Last message: %s | Expires in: %s minutes",
                        lastMessage.date.format(formatter), overDue * -1);
            } else {
                lastMessageText = String.format("Last message: %s | Expired %s minutes ago",
                        lastMessage.date.format(formatter), overDue);
            }
            Paragraph lastMessageSubTitle = new Paragraph(lastMessageText);
            lastMessageSubTitle.setAlignment(Paragraph.ALIGN_LEFT);
            document.add(lastMessageSubTitle);
            document.add(Chunk.NEWLINE);

            // Ticket details
            PdfPTable ticketTable = getPdfPTable(ticket);
            document.add(ticketTable);
            document.add(Chunk.NEWLINE);
            // users subtitle
            Paragraph usersSubTitle = new Paragraph("Users:", normalFont);
            usersSubTitle.setAlignment(Paragraph.ALIGN_LEFT);
            usersSubTitle.setSpacingAfter(20);
            document.add(usersSubTitle);
            // users table
            PdfPTable usersTable = generateUsersTable(ticket.tamUsers, ticket.supportUsers);
            document.add(usersTable);
            document.add(Chunk.NEWLINE);
            // messages title
            Paragraph messagesSubTitle = new Paragraph("Messages:", normalFont);
            messagesSubTitle.setAlignment(Paragraph.ALIGN_LEFT);
            messagesSubTitle.setSpacingAfter(20);
            document.add(messagesSubTitle);
            // messages and attachments details
            PdfPTable messagesTable = generateMessages(ticket.messages);
            document.add(messagesTable);
            document.add(Chunk.NEWLINE);
            document.close();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private static Color getTicketColor(Ticket ticket) {
        Color color = Color.WHITE;
        if (ticket.companyEntitlement != null && ticket.companyEntitlement.supportLevel != null) {
            switch (ticket.companyEntitlement.supportLevel.color) {
                case "Red":
                    color = Color.RED;
                    break;
                case "Yellow":
                    color = Color.YELLOW;
                    break;
                case "Black":
                    color = Color.lightGray;
                    break;
            }
        }
        return color;
    }

    private PdfPTable getPdfPTable(Ticket ticket) {
        PdfPTable ticketTable = new PdfPTable(2);
        ticketTable.setWidthPercentage(100);
        ticketTable.addCell(createCell("Company", red, Color.WHITE));
        ticketTable.addCell(ticket.company.name);
        ticketTable.addCell(createCell("Category", red, Color.WHITE));
        ticketTable.addCell(ticket.category.name);
        ticketTable.addCell(createCell("Entitlement", red, Color.WHITE));
        ticketTable.addCell(ticket.companyEntitlement.entitlement.name);
        ticketTable.addCell(createCell("Level", red, Color.WHITE));
        ticketTable.addCell(ticket.companyEntitlement != null && ticket.companyEntitlement.supportLevel != null
                ? ticket.companyEntitlement.supportLevel.name
                : "-");
        ticketTable.addCell(createCell("Status", red, Color.WHITE));
        ticketTable.addCell(ticket.status);
        ticketTable.addCell(createCell("External Issue", red, Color.WHITE));
        ticketTable.addCell(ticket.externalIssueLink == null ? "-" : ticket.externalIssueLink);
        ticketTable.addCell(createCell("Affects", red, Color.WHITE));
        ticketTable.addCell(ticket.affectsVersion != null ? ticket.affectsVersion.name : "-");
        ticketTable.addCell(createCell("Resolved", red, Color.WHITE));
        ticketTable.addCell(ticket.resolvedVersion == null ? "-" : ticket.resolvedVersion.name);
        return ticketTable;
    }

    private PdfPTable generateUsersTable(List<User> tams, List<User> supports) {
        PdfPTable usersTable = new PdfPTable(2);
        usersTable.setWidthPercentage(100);
        usersTable.addCell(createCell("Support", red, Color.WHITE));
        usersTable.addCell(createCell("TAM", red, Color.WHITE));
        for (int i = 0, j = 0; i < supports.size() || j < tams.size(); i++, j++) {
            if (i >= supports.size()) {
                usersTable.addCell("-");
            } else {
                usersTable.addCell(supports.get(i).email);
            }
            if (j >= tams.size()) {
                usersTable.addCell("-");
            } else {
                usersTable.addCell(tams.get(j).email);
            }
        }
        return usersTable;
    }

    private PdfPTable generateMessages(List<Message> messages) {
        PdfPTable messageTable = new PdfPTable(2);
        messageTable.setWidthPercentage(100);

        for (Message message : messages) {
            messageTable.addCell(createCell(message.date.format(formatter), red, Color.WHITE));
            messageTable.addCell(createCell(message.author.email, red, Color.WHITE));

            PdfPTable attachmentTable = new PdfPTable(1);
            attachmentTable.setWidthPercentage(100);
            if (message.attachments == null || message.attachments.isEmpty()) {
                attachmentTable.addCell(createCell("-", lightRed, lightRedFontColor));
            } else {
                for (Attachment attachment : message.attachments) {
                    if (attachment.isImage()) {
                        try {
                            Image img = Image.getInstance(attachment.data);
                            attachmentTable.addCell(img);
                        } catch (IOException e) {
                            attachmentTable.addCell("Failed to obtain image");
                        }
                    } else if (attachment.isVideo()) {
                        attachmentTable.addCell(createCell(attachment.name, lightRed, lightRedFontColor));
                    } else {
                        attachmentTable.addCell(createCell(new String(attachment.data, StandardCharsets.UTF_8),
                                lightRed, lightRedFontColor));
                    }
                }
            }
            PdfPCell mergedCell = new PdfPCell();
            mergedCell.setColspan(2);
            mergedCell.addElement(new Paragraph(message.body));
            mergedCell.addElement(new Paragraph(" "));
            mergedCell.addElement(attachmentTable);
            messageTable.addCell(mergedCell);
        }
        return messageTable;
    }

    private PdfPCell createCell(String txt, Color color, Color fontColor) {
        Font normalFont = FontFactory.getFont(FontFactory.COURIER, 12, fontColor);
        PdfPCell cell = new PdfPCell(new Phrase(txt, normalFont));
        cell.setBackgroundColor(color);
        cell.setPadding(5f);
        return cell;
    }

    private Message getLastMessage(List<Message> messages) {
        Message lastMessage = messages.getFirst();
        for (Message message : messages) {
            if (isAfter(message.date, lastMessage.date)) {
                lastMessage = message;
            }
        }
        return lastMessage;
    }

    private boolean isAfter(LocalDateTime time1, LocalDateTime time2) {
        return time1.isAfter(time2);
    }

    private Long getOverdue(Level supportLevel, Message message) {
        long minutes = supportLevel.level;
        LocalDateTime timeToExpire = message.date.plusMinutes(minutes);
        if (timeToExpire.isBefore(LocalDateTime.now())) {
            return java.time.Duration.between(timeToExpire, LocalDateTime.now()).toMinutes();
        }
        return java.time.Duration.between(timeToExpire, LocalDateTime.now()).toMinutes();
    }

}
