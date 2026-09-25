/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.model;

/**
 * Detached ticket summary for the resolution-time histogram buckets in {@link ReportData}.
 * <p>
 * Carries exactly the fields both report consumers need — the JSON API's {@code TicketSummary} (id, name, status,
 * company and category names) and the PDF export (ticket names only) — so the cached report snapshot never holds
 * managed {@link Ticket} entities.
 */
public record HistogramTicket(Long id, String name, String status, String companyName, String categoryName) {
}
