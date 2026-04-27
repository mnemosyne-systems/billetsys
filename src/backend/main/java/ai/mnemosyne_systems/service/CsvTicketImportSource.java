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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class CsvTicketImportSource implements TicketImportSource {

    public static final int MAX_CSV_BYTES = 5 * 1024 * 1024;
    public static final List<String> REQUIRED_COLUMNS = List.of("source_key", "title", "company", "entitlement",
            "status", "initial_message");
    public static final List<String> OPTIONAL_COLUMNS = List.of("source_system", "requester_email", "category",
            "external_issue_link", "created_at");

    private static final Set<String> KNOWN_COLUMNS = Set.of("source_system", "source_key", "title", "company",
            "entitlement", "status", "initial_message", "requester_email", "category", "external_issue_link",
            "created_at");

    @Override
    public String sourceType() {
        return "csv";
    }

    @Override
    public List<TicketImportRow> parse(byte[] data) {
        if (data == null || data.length == 0) {
            throw new BadRequestException("CSV file is required");
        }
        if (data.length > MAX_CSV_BYTES) {
            throw new BadRequestException("CSV file must be 5 MB or smaller");
        }
        List<List<String>> rows = parseRows(new String(data, StandardCharsets.UTF_8));
        if (rows.isEmpty()) {
            throw new BadRequestException("CSV header is required");
        }
        Map<String, Integer> header = headerIndexes(rows.get(0));
        for (String required : REQUIRED_COLUMNS) {
            if (!header.containsKey(required)) {
                throw new BadRequestException("CSV header is missing required column: " + required);
            }
        }
        List<TicketImportRow> result = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (isBlankRow(row)) {
                continue;
            }
            int rowNumber = i + 1;
            String sourceSystem = value(row, header, "source_system");
            if (sourceSystem == null || sourceSystem.isBlank()) {
                sourceSystem = sourceType();
            }
            result.add(new TicketImportRow(rowNumber, sourceSystem, value(row, header, "source_key"),
                    value(row, header, "title"), value(row, header, "company"), value(row, header, "entitlement"),
                    value(row, header, "status"), value(row, header, "initial_message"),
                    value(row, header, "requester_email"), value(row, header, "category"),
                    value(row, header, "external_issue_link"), value(row, header, "created_at")));
        }
        return result;
    }

    private Map<String, Integer> headerIndexes(List<String> headerRow) {
        Map<String, Integer> indexes = new LinkedHashMap<>();
        for (int i = 0; i < headerRow.size(); i++) {
            String header = normalizeHeader(headerRow.get(i));
            if (header == null || header.isBlank()) {
                continue;
            }
            if (KNOWN_COLUMNS.contains(header)) {
                indexes.putIfAbsent(header, i);
            }
        }
        return indexes;
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.startsWith("\uFEFF")) {
            normalized = normalized.substring(1);
        }
        return normalized.toLowerCase(Locale.ENGLISH);
    }

    private String value(List<String> row, Map<String, Integer> header, String name) {
        Integer index = header.get(name);
        if (index == null || index >= row.size()) {
            return null;
        }
        String value = row.get(index);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private boolean isBlankRow(List<String> row) {
        for (String value : row) {
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private List<List<String>> parseRows(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int i = 0;
        while (i < csv.length()) {
            char ch = csv.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        field.append('"');
                        i += 2;
                        continue;
                    }
                    quoted = false;
                } else {
                    field.append(ch);
                }
                i++;
                continue;
            }
            if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (ch == '\n') {
                row.add(field.toString());
                rows.add(row);
                row = new ArrayList<>();
                field.setLength(0);
            } else if (ch == '\r') {
                if (i + 1 < csv.length() && csv.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(field.toString());
                rows.add(row);
                row = new ArrayList<>();
                field.setLength(0);
            } else {
                field.append(ch);
            }
            i++;
        }
        if (quoted) {
            throw new BadRequestException("CSV contains an unterminated quoted field");
        }
        row.add(field.toString());
        if (!row.isEmpty() && !isBlankRow(row)) {
            rows.add(row);
        }
        return rows;
    }
}
