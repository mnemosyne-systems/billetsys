/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.service;

public record TicketImportRow(int rowNumber, String sourceSystem, String sourceKey, String title, String company,
        String entitlement, String status, String initialMessage, String requesterEmail, String category,
        String externalIssueLink, String createdAt) {
}
