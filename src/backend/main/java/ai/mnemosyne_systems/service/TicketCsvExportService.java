/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */
package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.resource.SupportTicketApiResource;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.util.List;

@ApplicationScoped
public class TicketCsvExportService {
    private String escapeValue(String value) {
        if (value == null) {
            return "";
        }
        boolean needQuotes = value.contains(",") || value.contains("\"") || value.contains("\n")
                || value.contains("\r");
        if (needQuotes) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public byte[] exportSummaries(List<SupportTicketApiResource.SupportTicketSummary> summaries) {
        List<SupportTicketApiResource.SupportTicketSummary> rows = summaries == null ? List.of() : summaries;
        StringBuilder csv = new StringBuilder();
        csv.append(
                "ticket_name,title,status,date,category,support_user,support_email,company,entitlement,level,affects_version,resolved_version\n");
        for (SupportTicketApiResource.SupportTicketSummary summary : rows) {
            if (summary == null) {
                continue;
            }
            String supportName = "";
            String supportEmail = "";
            if (summary.supportUser() != null) {
                String displayName = summary.supportUser().displayName();
                supportName = (displayName == null || displayName.isBlank()) ? summary.supportUser().username()
                        : displayName;
                supportEmail = summary.supportUser().email() == null ? "" : summary.supportUser().email();
            }

            csv.append(escapeValue(summary.name())).append(",");
            csv.append(escapeValue(summary.title())).append(",");
            csv.append(escapeValue(summary.status())).append(",");
            csv.append(escapeValue(summary.messageDateLabel())).append(",");
            csv.append(escapeValue(summary.categoryName())).append(",");
            csv.append(escapeValue(supportName)).append(",");
            csv.append(escapeValue(supportEmail)).append(",");
            csv.append(escapeValue(summary.companyName())).append(",");
            csv.append(escapeValue(summary.entitlementName())).append(",");
            csv.append(escapeValue(summary.levelName())).append(",");
            csv.append(escapeValue(summary.affectsVersionName())).append(",");
            csv.append(escapeValue(summary.resolvedVersionName())).append("\n");
        }
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        byte[] result = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(body, 0, result, bom.length, body.length);
        return result;
    }
}
