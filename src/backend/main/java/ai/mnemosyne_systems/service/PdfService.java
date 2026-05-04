package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.model.*;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.enterprise.context.ApplicationScoped;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class PdfService {

    @jakarta.inject.Inject
    CrossReferenceService crossReferenceService;

    private final Color red = new Color(176, 0, 32);
    private final Color lightRedFontColor = new Color(178, 15, 30);
    private final Color lightRed = new Color(244, 235, 236);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd yyyy, h.mma");

    public byte[] generateTicketPdf(Ticket ticket, List<Message> messages) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
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
            Message lastMessage = getLastMessage(messages);
            String lastMessageText = buildLastMessageText(ticket, lastMessage);
            Paragraph lastMessageSubTitle = new Paragraph(lastMessageText);
            lastMessageSubTitle.setAlignment(Paragraph.ALIGN_LEFT);
            document.add(lastMessageSubTitle);
            document.add(Chunk.NEWLINE);

            // Ticket details
            PdfPTable ticketTable = getPdfPTable(ticket);
            document.add(ticketTable);
            document.add(Chunk.NEWLINE);
            // Related tickets
            List<Ticket> relatedTickets = getRelatedTickets(ticket);
            if (!relatedTickets.isEmpty()) {
                Paragraph relatedSubTitle = new Paragraph("Related:", normalFont);
                relatedSubTitle.setAlignment(Paragraph.ALIGN_LEFT);
                relatedSubTitle.setSpacingAfter(20);
                document.add(relatedSubTitle);
                PdfPTable relatedTable = new PdfPTable(2);
                relatedTable.setWidthPercentage(100);
                relatedTable.addCell(createCell("Ticket", red, Color.WHITE));
                relatedTable.addCell(createCell("Title", red, Color.WHITE));
                for (Ticket related : relatedTickets) {
                    relatedTable.addCell(new Phrase("#" + related.name, normalFont));
                    relatedTable.addCell(new Phrase(related.displayTitle(), normalFont));
                }
                document.add(relatedTable);
                document.add(Chunk.NEWLINE);
            }
            // Related articles
            List<Article> relatedArticles = getRelatedArticles(ticket);
            if (!relatedArticles.isEmpty()) {
                Paragraph articlesSubTitle = new Paragraph("Articles:", normalFont);
                articlesSubTitle.setAlignment(Paragraph.ALIGN_LEFT);
                articlesSubTitle.setSpacingAfter(20);
                document.add(articlesSubTitle);
                PdfPTable articlesTable = new PdfPTable(1);
                articlesTable.setWidthPercentage(100);
                articlesTable.addCell(createCell("Title", red, Color.WHITE));
                for (Article article : relatedArticles) {
                    articlesTable.addCell(new Phrase(article.title == null ? "" : article.title, normalFont));
                }
                document.add(articlesTable);
                document.add(Chunk.NEWLINE);
            }
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
            PdfPTable messagesTable = generateMessages(messages, writer);
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

    public byte[] generateReportPdf(ReportData data, String companyName, String period, boolean showCompanyTable,
            Map<String, String> chartImages) {
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
        ticketTable.addCell(ticket.company != null && ticket.company.name != null ? ticket.company.name : "-");
        ticketTable.addCell(createCell("Category", red, Color.WHITE));
        ticketTable.addCell(ticket.category != null && ticket.category.name != null ? ticket.category.name : "-");
        ticketTable.addCell(createCell("Entitlement", red, Color.WHITE));
        ticketTable.addCell(ticket.companyEntitlement != null && ticket.companyEntitlement.entitlement != null
                && ticket.companyEntitlement.entitlement.name != null ? ticket.companyEntitlement.entitlement.name
                        : "-");
        ticketTable.addCell(createCell("Level", red, Color.WHITE));
        ticketTable.addCell(ticket.companyEntitlement != null && ticket.companyEntitlement.supportLevel != null
                ? ticket.companyEntitlement.supportLevel.name
                : "-");
        ticketTable.addCell(createCell("Status", red, Color.WHITE));
        ticketTable.addCell(ticket.status == null ? "-" : ticket.status);
        ticketTable.addCell(createCell("External Issue", red, Color.WHITE));
        ticketTable.addCell(ticket.externalIssueLink == null ? "-" : ticket.externalIssueLink);
        ticketTable.addCell(createCell("Affects", red, Color.WHITE));
        ticketTable.addCell(ticket.affectsVersion != null ? ticket.affectsVersion.name : "-");
        ticketTable.addCell(createCell("Resolved", red, Color.WHITE));
        ticketTable.addCell(ticket.resolvedVersion == null ? "-" : ticket.resolvedVersion.name);
        return ticketTable;
    }

    private List<Ticket> getRelatedTickets(Ticket ticket) {
        List<CrossReference> forward = CrossReference.list("sourceTicket = ?1 and targetType = 'ticket'", ticket);
        List<CrossReference> backward = CrossReference.list(
                "select cr from CrossReference cr join fetch cr.sourceTicket where cr.targetType = 'ticket' and cr.targetId = ?1",
                ticket.id);
        Map<Long, Ticket> unique = new LinkedHashMap<>();
        List<Long> forwardIds = new ArrayList<>();
        for (CrossReference ref : forward) {
            forwardIds.add(ref.targetId);
        }
        if (!forwardIds.isEmpty()) {
            List<Ticket> loaded = Ticket.list("id in ?1", forwardIds);
            for (Ticket t : loaded) {
                unique.put(t.id, t);
            }
        }
        for (CrossReference ref : backward) {
            unique.putIfAbsent(ref.sourceTicket.id, ref.sourceTicket);
        }
        List<Ticket> result = new ArrayList<>(unique.values());
        result.sort(Comparator.comparing(t -> t.name));
        return result;
    }

    private List<Article> getRelatedArticles(Ticket ticket) {
        List<CrossReference> refs = CrossReference.list("sourceTicket = ?1 and targetType = 'article'", ticket);
        if (refs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (CrossReference ref : refs) {
            ids.add(ref.targetId);
        }
        List<Article> loaded = Article.list("id in ?1", ids);
        Map<Long, Article> unique = new LinkedHashMap<>();
        for (Article a : loaded) {
            unique.put(a.id, a);
        }
        List<Article> result = new ArrayList<>(unique.values());
        result.sort(Comparator.comparing(a -> a.title == null ? "" : a.title, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private PdfPTable generateUsersTable(List<User> tams, List<User> supports) {
        List<User> safeTams = tams == null ? Collections.emptyList() : tams;
        List<User> safeSupports = supports == null ? Collections.emptyList() : supports;
        PdfPTable usersTable = new PdfPTable(2);
        usersTable.setWidthPercentage(100);
        usersTable.addCell(createCell("Support", red, Color.WHITE));
        usersTable.addCell(createCell("TAM", red, Color.WHITE));
        for (int i = 0, j = 0; i < safeSupports.size() || j < safeTams.size(); i++, j++) {
            if (i >= safeSupports.size()) {
                usersTable.addCell("-");
            } else {
                usersTable.addCell(safeSupports.get(i).email);
            }
            if (j >= safeTams.size()) {
                usersTable.addCell("-");
            } else {
                usersTable.addCell(safeTams.get(j).email);
            }
        }
        return usersTable;
    }

    private PdfPTable generateMessages(List<Message> messages, PdfWriter writer) throws IOException {
        PdfPTable messageTable = new PdfPTable(2);
        messageTable.setWidthPercentage(100);
        List<Message> safeMessages = messages == null ? new ArrayList<>() : new ArrayList<>(messages);
        safeMessages.sort(Comparator.reverseOrder());
        if (safeMessages.isEmpty()) {
            PdfPCell emptyCell = new PdfPCell(new Phrase("No messages"));
            emptyCell.setColspan(2);
            emptyCell.setPadding(8f);
            messageTable.addCell(emptyCell);
            return messageTable;
        }

        Map<Long, Ticket> ticketCache = crossReferenceService
                .preloadReferencedTickets(safeMessages.stream().map(m -> m.body).toList());

        for (Message message : safeMessages) {
            messageTable.addCell(createCell(message.date.format(formatter), red, Color.WHITE));
            messageTable.addCell(createCell(message.author == null ? "-" : message.author.email, red, Color.WHITE));

            PdfPTable attachmentTable = new PdfPTable(1);
            attachmentTable.setWidthPercentage(100);
            if (message.attachments == null || message.attachments.isEmpty()) {
                attachmentTable.addCell(createCell("-", lightRed, lightRedFontColor));
            } else {
                for (Attachment attachment : message.attachments) {
                    addAttachmentContent(attachmentTable, attachment, writer);
                }
            }
            PdfPCell mergedCell = new PdfPCell();
            mergedCell.setColspan(2);
            String transformedBody = crossReferenceService.transformBodyForPdf(message.body, ticketCache);
            mergedCell.addElement(new Paragraph(transformedBody == null ? "" : transformedBody));
            mergedCell.addElement(new Paragraph(" "));
            mergedCell.addElement(attachmentTable);
            messageTable.addCell(mergedCell);
        }
        return messageTable;
    }

    private void addAttachmentContent(PdfPTable attachmentTable, Attachment attachment, PdfWriter writer)
            throws IOException {
        attachmentTable.addCell(createCell(attachment.name == null ? "Attachment" : attachment.name, red, Color.WHITE));

        PdfPCell contentCell = new PdfPCell();
        contentCell.setBackgroundColor(lightRed);
        contentCell.setPadding(8f);

        if (attachment.isImage()) {
            addImageAttachment(contentCell, attachment);
        } else if (isPdfAttachment(attachment)) {
            addPdfAttachment(contentCell, attachment, writer);
        } else if (isTextAttachment(attachment)) {
            addTextAttachment(contentCell, attachment);
        } else {
            contentCell.addElement(new Paragraph("Attachment content could not be rendered in this PDF.",
                    FontFactory.getFont(FontFactory.COURIER, 12, lightRedFontColor)));
        }

        attachmentTable.addCell(contentCell);
    }

    private void addImageAttachment(PdfPCell contentCell, Attachment attachment) {
        try {
            Image img = Image.getInstance(attachment.data);
            img.scaleToFit(460f, 320f);
            img.setAlignment(Image.ALIGN_LEFT);
            contentCell.addElement(img);
        } catch (IOException e) {
            contentCell.addElement(new Paragraph("Failed to obtain image",
                    FontFactory.getFont(FontFactory.COURIER, 12, lightRedFontColor)));
        }
    }

    private void addPdfAttachment(PdfPCell contentCell, Attachment attachment, PdfWriter writer) throws IOException {
        try (PdfReader reader = new PdfReader(attachment.data)) {
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                Image pageImage = Image.getInstance(writer.getImportedPage(reader, page));
                pageImage.scaleToFit(460f, 640f);
                pageImage.setAlignment(Image.ALIGN_LEFT);
                contentCell.addElement(pageImage);
                if (page < reader.getNumberOfPages()) {
                    contentCell.addElement(Chunk.NEWLINE);
                }
            }
        }
    }

    private void addTextAttachment(PdfPCell contentCell, Attachment attachment) {
        Paragraph paragraph = new Paragraph(
                new String(attachment.data == null ? new byte[0] : attachment.data, StandardCharsets.UTF_8),
                FontFactory.getFont(FontFactory.COURIER, 12, lightRedFontColor));
        paragraph.setLeading(14f);
        contentCell.addElement(paragraph);
    }

    private boolean isPdfAttachment(Attachment attachment) {
        if (attachment == null) {
            return false;
        }
        String mimeType = attachment.mimeType == null ? "" : attachment.mimeType.toLowerCase();
        String name = attachment.name == null ? "" : attachment.name.toLowerCase();
        return "application/pdf".equals(mimeType) || name.endsWith(".pdf");
    }

    private boolean isTextAttachment(Attachment attachment) {
        if (attachment == null) {
            return false;
        }
        String mimeType = attachment.mimeType == null ? "" : attachment.mimeType.toLowerCase();
        return mimeType.startsWith("text/");
    }

    private PdfPCell createCell(String txt, Color color, Color fontColor) {
        Font normalFont = FontFactory.getFont(FontFactory.COURIER, 12, fontColor);
        PdfPCell cell = new PdfPCell(new Phrase(txt, normalFont));
        cell.setBackgroundColor(color);
        cell.setPadding(5f);
        return cell;
    }

    private Message getLastMessage(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
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

    private String buildLastMessageText(Ticket ticket, Message lastMessage) {
        if (lastMessage == null || lastMessage.date == null) {
            return "Last message: -";
        }
        if (ticket.companyEntitlement == null || ticket.companyEntitlement.supportLevel == null) {
            return String.format("Last message: %s", lastMessage.date.format(formatter));
        }
        Long overDue = getOverdue(ticket.companyEntitlement.supportLevel, lastMessage);
        String lastMessageText = "minutes ago";
        if (overDue < 0) {
            return String.format("Last message: %s | Expires in: %s minutes", lastMessage.date.format(formatter),
                    overDue * -1);
        }
        if (overDue > 60) {
            overDue = overDue / 60;
            lastMessageText = "hour(s) ago";
            if (overDue > 24) {
                overDue = overDue / 24;
                lastMessageText = "day(s) ago";
                if (overDue > 30) {
                    lastMessageText = "month(s) ago";
                    overDue = overDue / 30;
                    if (overDue > 365) {
                        overDue = overDue / 365;
                        lastMessageText = "year(s) ago";
                    }
                }
            }
        }
        return String.format("Last message: %s | Expired %s ", lastMessage.date.format(formatter), overDue)
                + lastMessageText;
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
