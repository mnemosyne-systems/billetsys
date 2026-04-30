/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.TicketImportBatch;
import ai.mnemosyne_systems.model.TicketImportRecord;
import ai.mnemosyne_systems.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class TicketImportService {

    @Inject
    CsvTicketImportSource csvSource;

    @Inject
    TicketCreationService ticketCreationService;

    @Transactional
    public TicketImportSummary importCsv(byte[] data, String fileName, User actor) {
        return importRows(csvSource, csvSource.parse(data), fileName, actor);
    }

    TicketImportSummary importRows(TicketImportSource source, List<TicketImportRow> rows, String fileName, User actor) {
        TicketImportBatch batch = new TicketImportBatch();
        batch.sourceType = source.sourceType();
        batch.fileName = fileName;
        batch.actor = actor;
        batch.startedAt = LocalDateTime.now();
        batch.status = "Running";
        batch.persist();

        List<TicketImportResult> results = new ArrayList<>();
        for (TicketImportRow row : rows) {
            TicketImportResult result = importRow(batch, row, actor);
            results.add(result);
            if (TicketImportRecord.RESULT_CREATED.equals(result.result())) {
                batch.createdCount++;
            } else if (TicketImportRecord.RESULT_SKIPPED.equals(result.result())) {
                batch.skippedCount++;
            } else {
                batch.failedCount++;
            }
        }
        batch.completedAt = LocalDateTime.now();
        batch.status = batchStatus(batch);
        return new TicketImportSummary(batch.id, batch.sourceType, batch.fileName, batch.status, rows.size(),
                batch.createdCount, batch.skippedCount, batch.failedCount, results);
    }

    private String batchStatus(TicketImportBatch batch) {
        if (batch.failedCount > 0 && batch.createdCount == 0 && batch.skippedCount == 0) {
            return TicketImportBatch.STATUS_FAILED;
        }
        if (batch.failedCount > 0) {
            return TicketImportBatch.STATUS_COMPLETED_WITH_ERRORS;
        }
        return TicketImportBatch.STATUS_COMPLETED;
    }

    private TicketImportResult importRow(TicketImportBatch batch, TicketImportRow row, User actor) {
        String sourceSystem = sourceIdentity(row.sourceSystem(), batch.sourceType);
        String sourceKey = sourceIdentity(row.sourceKey(), "__missing_source_key_row_" + row.rowNumber());
        try {
            sourceKey = normalizeRequired(row.sourceKey(), "Source key is required");
            TicketImportRecord existing = TicketImportRecord
                    .find("sourceSystem = ?1 and sourceKey = ?2 and result = ?3 order by id asc", sourceSystem,
                            sourceKey, TicketImportRecord.RESULT_CREATED)
                    .firstResult();
            if (existing != null && existing.ticket != null) {
                TicketImportRecord record = record(batch, row, sourceSystem, sourceKey,
                        TicketImportRecord.RESULT_SKIPPED, existing.ticket, "Duplicate source ticket");
                return toResult(record);
            }
            String title = normalizeRequired(row.title(), "Title is required");
            String initialMessage = normalizeRequired(row.initialMessage(), "Initial message is required");
            Company company = resolveCompany(row.company());
            CompanyEntitlement entitlement = resolveEntitlement(company, row.entitlement());
            Category category = resolveCategory(row.category());
            User requester = resolveRequester(row.requesterEmail(), actor);
            String status = row.status() == null || row.status().isBlank() ? "Open" : row.status().trim();
            Ticket ticket = ticketCreationService.createTicketWithInitialMessage(
                    new TicketCreationService.TicketCreationRequest(title, status, company, entitlement, category,
                            requester, initialMessage, parseCreatedAt(row.createdAt()), row.externalIssueLink()));
            TicketImportRecord record = record(batch, row, sourceSystem, sourceKey, TicketImportRecord.RESULT_CREATED,
                    ticket, null);
            return toResult(record);
        } catch (RuntimeException ex) {
            TicketImportRecord record = record(batch, row, sourceSystem, sourceKey, TicketImportRecord.RESULT_FAILED,
                    null, cleanError(ex));
            return toResult(record);
        }
    }

    private String sourceIdentity(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private TicketImportRecord record(TicketImportBatch batch, TicketImportRow row, String sourceSystem,
            String sourceKey, String result, Ticket ticket, String errorMessage) {
        TicketImportRecord record = new TicketImportRecord();
        record.batch = batch;
        record.sourceSystem = sourceSystem;
        record.sourceKey = sourceKey;
        record.ticket = ticket;
        record.rowNumber = row.rowNumber();
        record.result = result;
        record.errorMessage = errorMessage;
        record.persist();
        return record;
    }

    private TicketImportResult toResult(TicketImportRecord record) {
        return new TicketImportResult(record.rowNumber, record.sourceSystem, record.sourceKey, record.result,
                record.ticket == null ? null : record.ticket.id, record.ticket == null ? null : record.ticket.name,
                record.errorMessage);
    }

    private Company resolveCompany(String name) {
        String normalized = normalizeRequired(name, "Company is required");
        Company company = Company.find("lower(name) = ?1", normalized.toLowerCase(Locale.ENGLISH)).firstResult();
        if (company == null) {
            throw new IllegalArgumentException("Company not found: " + normalized);
        }
        return company;
    }

    private CompanyEntitlement resolveEntitlement(Company company, String name) {
        String normalized = normalizeRequired(name, "Entitlement is required");
        CompanyEntitlement entitlement = CompanyEntitlement.find(
                "select ce from CompanyEntitlement ce join ce.entitlement e where ce.company = ?1 and lower(e.name) = ?2",
                company, normalized.toLowerCase(Locale.ENGLISH)).firstResult();
        if (entitlement == null) {
            throw new IllegalArgumentException("Entitlement not found for company: " + normalized);
        }
        return entitlement;
    }

    private Category resolveCategory(String name) {
        if (name == null || name.isBlank()) {
            return Category.findDefault();
        }
        Category category = Category.find("lower(name) = ?1", name.trim().toLowerCase(Locale.ENGLISH)).firstResult();
        if (category == null) {
            throw new IllegalArgumentException("Category not found: " + name.trim());
        }
        return category;
    }

    private User resolveRequester(String email, User actor) {
        if (email == null || email.isBlank()) {
            return actor;
        }
        User requester = User.find("lower(email) = ?1", email.trim().toLowerCase(Locale.ENGLISH)).firstResult();
        if (requester == null) {
            throw new IllegalArgumentException("Requester not found: " + email.trim());
        }
        return requester;
    }

    private LocalDateTime parseCreatedAt(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        String trimmed = value.trim();
        try {
            return LocalDateTime.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(trimmed).toLocalDateTime();
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Created at is invalid: " + trimmed);
            }
        }
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String cleanError(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "Unable to import row";
        }
        return message;
    }

    public record TicketImportSummary(Long batchId, String sourceType, String fileName, String status, int rowCount,
            int createdCount, int skippedCount, int failedCount, List<TicketImportResult> results) {
    }

    public record TicketImportResult(int rowNumber, String sourceSystem, String sourceKey, String result, Long ticketId,
            String ticketName, String errorMessage) {
    }
}
