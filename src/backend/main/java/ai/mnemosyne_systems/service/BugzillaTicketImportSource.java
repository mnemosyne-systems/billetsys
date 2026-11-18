/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class BugzillaTicketImportSource implements TicketImportSource {

    public static final List<String> REQUIRED_COLUMNS = List.of("bug_id", "short_desc", "bug_status");
    public static final List<String> OPTIONAL_COLUMNS = List.of("product", "component", "assigned_to", "resolution",
            "opendate", "bug_severity", "changeddate");

    private static final Set<String> KNOWN_COLUMNS = Set.of("bug_id", "product", "component", "assigned_to",
            "bug_status", "resolution", "short_desc", "opendate", "bug_severity", "changeddate");
    private static final DateTimeFormatter BUGZILLA_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss",
            Locale.ENGLISH);

    @Override
    public String sourceType() {
        return "bugzilla";
    }

    @Override
    public List<TicketImportRow> parse(byte[] data) {
        return parse(data, ZoneOffset.UTC, null);
    }

    public List<TicketImportRow> parse(byte[] data, ZoneId timezone, String baseUrl) {
        if (data == null || data.length == 0) {
            throw new BadRequestException("Bugzilla CSV file is required");
        }
        if (data.length > TicketImportLimits.MAX_IMPORT_BYTES) {
            throw new BadRequestException("Bugzilla CSV file must be 5 MB or smaller");
        }
        List<List<String>> rows = CsvImportRows.parse(new String(data, StandardCharsets.UTF_8));
        if (rows.isEmpty()) {
            throw new BadRequestException("CSV header is required");
        }
        Map<String, Integer> header = CsvImportRows.headerIndexes(rows.get(0), KNOWN_COLUMNS);
        for (String required : REQUIRED_COLUMNS) {
            if (!header.containsKey(required)) {
                if (hasHumanLabels(rows.get(0))) {
                    throw new BadRequestException(
                            "Bugzilla export uses labels instead of field names; remove human=1 from the export URL");
                }
                throw new BadRequestException("CSV header is missing required column: " + required);
            }
        }
        ZoneId exportZone = timezone == null ? ZoneOffset.UTC : timezone;
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        List<TicketImportRow> result = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (CsvImportRows.isBlankRow(row)) {
                continue;
            }
            String bugId = clean(CsvImportRows.value(row, header, "bug_id"));
            String summary = clean(CsvImportRows.value(row, header, "short_desc"));
            String product = clean(CsvImportRows.value(row, header, "product"));
            String component = clean(CsvImportRows.value(row, header, "component"));
            String assignee = clean(CsvImportRows.value(row, header, "assigned_to"));
            String rawStatus = clean(CsvImportRows.value(row, header, "bug_status"));
            String resolution = clean(CsvImportRows.value(row, header, "resolution"));
            String severity = clean(CsvImportRows.value(row, header, "bug_severity"));
            String openedAt = clean(CsvImportRows.value(row, header, "opendate"));
            String status = isClosed(rawStatus) ? "Closed" : "Open";
            String category = "enhancement".equalsIgnoreCase(severity) ? "Feature" : "Bug";
            result.add(new TicketImportRow(i + 1, sourceType(), bugId, summary, null, null, status,
                    initialMessage(summary, bugId, product, component, assignee, rawStatus, resolution), null, category,
                    externalIssueLink(normalizedBaseUrl, bugId),
                    convertOpenedAt(openedAt, exportZone, ZoneId.systemDefault())));
        }
        return result;
    }

    static String convertOpenedAt(String value, ZoneId exportZone, ZoneId targetZone) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            LocalDateTime openedAt = parseOpenedAt(trimmed);
            ZoneId sourceZone = exportZone == null ? ZoneOffset.UTC : exportZone;
            ZoneId destinationZone = targetZone == null ? ZoneId.systemDefault() : targetZone;
            return openedAt.atZone(sourceZone).withZoneSameInstant(destinationZone).toLocalDateTime()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ex) {
            return trimmed;
        }
    }

    private static LocalDateTime parseOpenedAt(String value) {
        try {
            return LocalDateTime.parse(value, BUGZILLA_DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            return LocalDateTime.parse(value);
        }
    }

    private static boolean isClosed(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String normalized = status.trim().toUpperCase(Locale.ENGLISH);
        return "RESOLVED".equals(normalized) || "VERIFIED".equals(normalized) || "CLOSED".equals(normalized);
    }

    private static String initialMessage(String summary, String bugId, String product, String component,
            String assignee, String status, String resolution) {
        StringBuilder message = new StringBuilder("Summary: ").append(summary == null ? "" : summary);
        message.append("\n\nImported from Bugzilla bug #").append(bugId == null ? "" : bugId);
        appendField(message, "Product", product);
        appendField(message, "Component", component);
        appendField(message, "Assignee", assignee);
        appendField(message, "Status", status);
        if (!"---".equals(resolution)) {
            appendField(message, "Resolution", resolution);
        }
        return message.toString();
    }

    private static void appendField(StringBuilder message, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        message.append('\n').append(label).append(": ").append(value);
    }

    private static String externalIssueLink(String baseUrl, String bugId) {
        if (baseUrl == null || baseUrl.isBlank() || bugId == null || bugId.isBlank()) {
            return null;
        }
        return baseUrl + "/show_bug.cgi?id=" + bugId;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isBlank() ? null : normalized;
    }

    private static String clean(String value) {
        return value == null ? null : value.replace('\uFF3C', '\\');
    }

    private static boolean hasHumanLabels(List<String> headerRow) {
        for (String value : headerRow) {
            String normalized = CsvImportRows.normalizeHeader(value);
            if ("bug id".equals(normalized) || "summary".equals(normalized) || "bug status".equals(normalized)) {
                return true;
            }
        }
        return false;
    }
}
