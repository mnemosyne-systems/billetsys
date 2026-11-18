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
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class CsvTicketImportSource implements TicketImportSource {

    public static final int MAX_CSV_BYTES = TicketImportLimits.MAX_IMPORT_BYTES;
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
        List<List<String>> rows = CsvImportRows.parse(new String(data, StandardCharsets.UTF_8));
        if (rows.isEmpty()) {
            throw new BadRequestException("CSV header is required");
        }
        Map<String, Integer> header = CsvImportRows.headerIndexes(rows.get(0), KNOWN_COLUMNS);
        for (String required : REQUIRED_COLUMNS) {
            if (!header.containsKey(required)) {
                throw new BadRequestException("CSV header is missing required column: " + required);
            }
        }
        List<TicketImportRow> result = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (CsvImportRows.isBlankRow(row)) {
                continue;
            }
            int rowNumber = i + 1;
            String sourceSystem = CsvImportRows.value(row, header, "source_system");
            if (sourceSystem == null || sourceSystem.isBlank()) {
                sourceSystem = sourceType();
            }
            result.add(new TicketImportRow(rowNumber, sourceSystem, CsvImportRows.value(row, header, "source_key"),
                    CsvImportRows.value(row, header, "title"), CsvImportRows.value(row, header, "company"),
                    CsvImportRows.value(row, header, "entitlement"), CsvImportRows.value(row, header, "status"),
                    CsvImportRows.value(row, header, "initial_message"),
                    CsvImportRows.value(row, header, "requester_email"), CsvImportRows.value(row, header, "category"),
                    CsvImportRows.value(row, header, "external_issue_link"),
                    CsvImportRows.value(row, header, "created_at")));
        }
        return result;
    }
}
