package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.model.*;
import ai.mnemosyne_systems.resource.ReportResource;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
            Font subHeaderFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 14, Color.white);
            Font normalFont = FontFactory.getFont(FontFactory.COURIER, 12);

            // Owner Header
            Installation installation = getOwner();
            Chunk ownerHeader = new Chunk(installation.name, subHeaderFont);
            ownerHeader.setBackground(red, 5f, 5f, 400f, 5f);
            Paragraph owner = new Paragraph(ownerHeader);
            owner.setAlignment(Paragraph.ALIGN_LEFT);
            document.add(owner);
            document.add(Chunk.NEWLINE);

            // Pdf title
            Chunk titleChunk = new Chunk("Ticket: " + ticket.name, titleFont);
            titleChunk.setBackground(Color.RED, 5f, 5f, 5f, 5f);
            Paragraph title = new Paragraph(titleChunk);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            // Reason for SLA
            Message lastMessage = getLastMessage(ticket.messages);
            Long overDue = getOverdue(ticket.companyEntitlement.supportLevel, lastMessage);
            String lastMessageText = "minutes ago";
            if (overDue < 0) {
                lastMessageText = String.format("Last message: %s | Expires in: %s minutes",
                        lastMessage.date.format(formatter), overDue * -1);
            } else {
                // more than an hour
                if (overDue > 60) {
                    overDue = overDue / 60;
                    lastMessageText = "hour(s) ago";
                    // more than a day
                    if (overDue > 24) {
                        overDue = overDue / 24;
                        lastMessageText = "day(s) ago";
                        // more than month
                        if (overDue > 30) {
                            lastMessageText = "month(s) ago";
                            overDue = overDue / 30;
                            // more than a year
                            if (overDue > 365) {
                                overDue = overDue / 365;
                                lastMessageText = "year(s) ago";
                            }
                        }
                    }
                }
                lastMessageText = String.format("Last message: %s | Expired %s ", lastMessage.date.format(formatter),
                        overDue) + lastMessageText;
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

    public byte[] generateReportPdf(ReportResource.ReportData data, String companyName, String period,
            boolean showCompanyTable, Map<String, String> chartImages) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 16);
            Font sectionFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 13);
            Font normalFont = FontFactory.getFont(FontFactory.COURIER, 12);
            Font subFont = FontFactory.getFont(FontFactory.COURIER, 10, Color.GRAY);

            // Owner header
            Installation installation = getOwner();
            Font subHeaderFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 14, Color.white);
            Chunk ownerHeader = new Chunk(installation.name, subHeaderFont);
            ownerHeader.setBackground(red, 5f, 5f, 400f, 5f);
            Paragraph owner = new Paragraph(ownerHeader);
            owner.setAlignment(Paragraph.ALIGN_LEFT);
            document.add(owner);
            document.add(Chunk.NEWLINE);

            // Title
            Paragraph title = new Paragraph("Report — " + companyName, titleFont);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            // Summary line
            Paragraph summary = new Paragraph("Total tickets: " + data.totalTickets, subFont);
            summary.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(summary);
            document.add(Chunk.NEWLINE);

            // Tickets by Status
            document.add(new Paragraph("Tickets by Status", sectionFont));
            document.add(Chunk.NEWLINE);
            addChartImage(document, chartImages, "statusChart");
            PdfPTable statusTable = new PdfPTable(2);
            statusTable.setWidthPercentage(100);
            statusTable.addCell(createCell("Status", red, Color.WHITE));
            statusTable.addCell(createCell("Count", red, Color.WHITE));
            for (Map.Entry<String, Long> entry : data.ticketsByStatus.entrySet()) {
                statusTable.addCell(new Phrase(entry.getKey(), normalFont));
                statusTable.addCell(new Phrase(String.valueOf(entry.getValue()), normalFont));
            }
            document.add(statusTable);
            document.add(Chunk.NEWLINE);

            // Tickets by Category
            document.add(new Paragraph("Tickets by Category", sectionFont));
            document.add(Chunk.NEWLINE);
            addChartImage(document, chartImages, "categoryChart");
            PdfPTable categoryTable = new PdfPTable(2);
            categoryTable.setWidthPercentage(100);
            categoryTable.addCell(createCell("Category", red, Color.WHITE));
            categoryTable.addCell(createCell("Count", red, Color.WHITE));
            for (Map.Entry<String, Long> entry : data.ticketsByCategory.entrySet()) {
                categoryTable.addCell(new Phrase(entry.getKey(), normalFont));
                categoryTable.addCell(new Phrase(String.valueOf(entry.getValue()), normalFont));
            }
            document.add(categoryTable);
            document.add(Chunk.NEWLINE);

            // Tickets by Company
            if (showCompanyTable) {
                document.add(new Paragraph("Tickets by Company", sectionFont));
                document.add(Chunk.NEWLINE);
                addChartImage(document, chartImages, "companyChart");
                PdfPTable companyTable = new PdfPTable(2);
                companyTable.setWidthPercentage(100);
                companyTable.addCell(createCell("Company", red, Color.WHITE));
                companyTable.addCell(createCell("Count", red, Color.WHITE));
                for (Map.Entry<String, Long> entry : data.ticketsByCompany.entrySet()) {
                    companyTable.addCell(new Phrase(entry.getKey(), normalFont));
                    companyTable.addCell(new Phrase(String.valueOf(entry.getValue()), normalFont));
                }
                document.add(companyTable);
                document.add(Chunk.NEWLINE);
            }

            // Ticket Volume Over Time
            String periodLabel = "year".equals(period) ? "This year"
                    : "month".equals(period) ? "This month" : "All time";
            document.add(new Paragraph("Ticket Volume Over Time — " + periodLabel, sectionFont));
            document.add(Chunk.NEWLINE);
            addChartImage(document, chartImages, "timeChart");
            if (data.ticketsOverTime.isEmpty()) {
                document.add(new Paragraph("No data available", normalFont));
            } else {
                PdfPTable timeTable = new PdfPTable(2);
                timeTable.setWidthPercentage(100);
                timeTable.addCell(createCell("Period", red, Color.WHITE));
                timeTable.addCell(createCell("Tickets Created", red, Color.WHITE));
                for (Map.Entry<String, Long> entry : data.ticketsOverTime.entrySet()) {
                    timeTable.addCell(new Phrase(entry.getKey(), normalFont));
                    timeTable.addCell(new Phrase(String.valueOf(entry.getValue()), normalFont));
                }
                document.add(timeTable);
            }
            document.add(Chunk.NEWLINE);

            // Avg First Response Time
            document.add(new Paragraph("Avg. First Response Time (hours)", sectionFont));
            document.add(Chunk.NEWLINE);
            addChartImage(document, chartImages, "responseTimeChart");
            if (data.avgFirstResponseTime.isEmpty()) {
                document.add(new Paragraph("No data available", normalFont));
            } else {
                PdfPTable responseTable = new PdfPTable(2);
                responseTable.setWidthPercentage(100);
                responseTable.addCell(createCell("Category", red, Color.WHITE));
                responseTable.addCell(createCell("Avg. Hours", red, Color.WHITE));
                for (Map.Entry<String, Double> entry : data.avgFirstResponseTime.entrySet()) {
                    responseTable.addCell(new Phrase(entry.getKey(), normalFont));
                    responseTable.addCell(new Phrase(String.valueOf(entry.getValue()), normalFont));
                }
                document.add(responseTable);
            }
            document.add(Chunk.NEWLINE);

            // Resolution Histogram
            document.add(new Paragraph("Resolution Time", sectionFont));
            document.add(Chunk.NEWLINE);
            addChartImage(document, chartImages, "histogramChart");
            boolean hasHistogramData = data.resolutionHistogram.values().stream().anyMatch(list -> !list.isEmpty());
            if (!hasHistogramData) {
                document.add(new Paragraph("No data available", normalFont));
                document.add(Chunk.NEWLINE);
            } else {
                PdfPTable histogramTable = new PdfPTable(3);
                histogramTable.setWidthPercentage(100);
                histogramTable.addCell(createCell("Duration", red, Color.WHITE));
                histogramTable.addCell(createCell("Count", red, Color.WHITE));
                histogramTable.addCell(createCell("Tickets", red, Color.WHITE));
                for (Map.Entry<String, List<Ticket>> entry : data.resolutionHistogram.entrySet()) {
                    List<Ticket> tickets = entry.getValue();
                    histogramTable.addCell(new Phrase(entry.getKey(), normalFont));
                    histogramTable.addCell(new Phrase(String.valueOf(tickets.size()), normalFont));
                    if (tickets.isEmpty()) {
                        histogramTable.addCell(new Phrase("-", normalFont));
                    } else {
                        StringBuilder names = new StringBuilder();
                        for (int i = 0; i < tickets.size(); i++) {
                            if (i > 0)
                                names.append(", ");
                            names.append(tickets.get(i).name);
                        }
                        histogramTable.addCell(new Phrase(names.toString(), normalFont));
                    }
                }
                document.add(histogramTable);
                document.add(Chunk.NEWLINE);
            }

            // Avg Resolution Time
            document.add(new Paragraph("Avg. Resolution Time (hours)", sectionFont));
            document.add(Chunk.NEWLINE);
            addChartImage(document, chartImages, "resolutionTimeChart");
            if (data.avgResolutionTime.isEmpty()) {
                document.add(new Paragraph("No data available", normalFont));
            } else {
                PdfPTable resolutionTable = new PdfPTable(2);
                resolutionTable.setWidthPercentage(100);
                resolutionTable.addCell(createCell("Category", red, Color.WHITE));
                resolutionTable.addCell(createCell("Avg. Hours", red, Color.WHITE));
                for (Map.Entry<String, Double> entry : data.avgResolutionTime.entrySet()) {
                    resolutionTable.addCell(new Phrase(entry.getKey(), normalFont));
                    resolutionTable.addCell(new Phrase(String.valueOf(entry.getValue()), normalFont));
                }
                document.add(resolutionTable);
            }
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
        messages.sort(Comparator.reverseOrder());
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

    private Installation getOwner() {
        return Installation.find("SELECT i from Installation i WHERE i.singletonKey = 'installation'").firstResult();
    }

    private void addChartImage(Document document, Map<String, String> chartImages, String key) {
        if (chartImages == null)
            return;
        String base64 = chartImages.get(key);
        if (base64 == null || base64.isBlank())
            return;
        try {
            String data = base64.contains(",") ? base64.substring(base64.indexOf(',') + 1) : base64;
            byte[] imageBytes = java.util.Base64.getDecoder().decode(data);
            Image img = Image.getInstance(imageBytes);
            img.scaleToFit(500, 200);
            img.setAlignment(Image.ALIGN_LEFT);
            document.add(img);
            document.add(Chunk.NEWLINE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to embed chart image: " + key, e);
        }
    }
}
