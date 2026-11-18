/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.service;

import jakarta.ws.rs.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BugzillaTicketImportSourceTest {

    private final BugzillaTicketImportSource source = new BugzillaTicketImportSource();

    @Test
    void mapsRequiredColumnsAndDefaults() {
        List<TicketImportRow> rows = source.parse("""
                bug_id,short_desc,bug_status
                101,Login fails,CONFIRMED
                """.getBytes(StandardCharsets.UTF_8));

        Assertions.assertEquals(1, rows.size());
        TicketImportRow row = rows.get(0);
        Assertions.assertEquals(2, row.rowNumber());
        Assertions.assertEquals("bugzilla", row.sourceSystem());
        Assertions.assertEquals("101", row.sourceKey());
        Assertions.assertEquals("Login fails", row.title());
        Assertions.assertEquals("Open", row.status());
        Assertions.assertEquals("Bug", row.category());
        Assertions.assertNull(row.company());
        Assertions.assertNull(row.entitlement());
        Assertions.assertNull(row.requesterEmail());
        Assertions.assertNull(row.externalIssueLink());
    }

    @Test
    void mapsOptionalFieldsIntoInitialMessageAndLink() {
        List<TicketImportRow> rows = source.parse("""
                bug_id,short_desc,bug_status,product,component,assigned_to,resolution,bug_severity
                101,Add dark mode,RESOLVED,Platform,Theme,dev@example.com,FIXED,enhancement
                """.getBytes(StandardCharsets.UTF_8), ZoneOffset.UTC, "https://bugzilla.example.com/");

        TicketImportRow row = rows.get(0);
        Assertions.assertEquals("Closed", row.status());
        Assertions.assertEquals("Feature", row.category());
        Assertions.assertEquals("https://bugzilla.example.com/show_bug.cgi?id=101", row.externalIssueLink());
        Assertions.assertEquals("""
                Summary: Add dark mode

                Imported from Bugzilla bug #101
                Product: Platform
                Component: Theme
                Assignee: dev@example.com
                Status: RESOLVED
                Resolution: FIXED""", row.initialMessage());
    }

    @Test
    void omitsEmptyResolution() {
        List<TicketImportRow> rows = source.parse("""
                bug_id,short_desc,bug_status,resolution
                102,Crash on save,NEW,---
                """.getBytes(StandardCharsets.UTF_8));

        Assertions.assertFalse(rows.get(0).initialMessage().contains("Resolution:"));
    }

    @Test
    void mapsVerifiedAndClosedToClosed() {
        List<TicketImportRow> rows = source.parse("""
                bug_id,short_desc,bug_status
                1,Verified bug,VERIFIED
                2,Closed bug,CLOSED
                3,New bug,NEW
                """.getBytes(StandardCharsets.UTF_8));

        Assertions.assertEquals("Closed", rows.get(0).status());
        Assertions.assertEquals("Closed", rows.get(1).status());
        Assertions.assertEquals("Open", rows.get(2).status());
    }

    @Test
    void reversesFullwidthBackslash() {
        List<TicketImportRow> rows = source.parse("""
                bug_id,short_desc,bug_status
                103,Path ＼server＼share,NEW
                """.getBytes(StandardCharsets.UTF_8));

        Assertions.assertEquals("Path \\server\\share", rows.get(0).title());
    }

    @Test
    void rejectsHumanReadableHeader() {
        BadRequestException ex = Assertions.assertThrows(BadRequestException.class, () -> source.parse("""
                Bug ID,Summary,Bug Status
                101,Login fails,NEW
                """.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertTrue(ex.getMessage().contains("human=1"));
    }

    @Test
    void rejectsBaseUrlWithoutHttpScheme() {
        BadRequestException ex = Assertions.assertThrows(BadRequestException.class, () -> source.parse("""
                bug_id,short_desc,bug_status
                101,Login fails,NEW
                """.getBytes(StandardCharsets.UTF_8), ZoneOffset.UTC, "javascript:alert(1)"));

        Assertions.assertTrue(ex.getMessage().contains("http://"));
    }

    @Test
    void rejectsMissingRequiredColumn() {
        BadRequestException ex = Assertions.assertThrows(BadRequestException.class, () -> source.parse("""
                bug_id,short_desc
                101,Login fails
                """.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertTrue(ex.getMessage().contains("bug_status"));
    }

    @Test
    void convertsOpenedAtBetweenZones() {
        String value = BugzillaTicketImportSource.convertOpenedAt("2026-04-26 10:15:30", ZoneId.of("Europe/Berlin"),
                ZoneOffset.UTC);

        Assertions.assertEquals("2026-04-26T08:15:30", value);
    }

    @Test
    void convertsIsoOpenedAtBetweenZones() {
        String value = BugzillaTicketImportSource.convertOpenedAt("2026-04-26T10:15:30", ZoneId.of("Europe/Berlin"),
                ZoneOffset.UTC);

        Assertions.assertEquals("2026-04-26T08:15:30", value);
    }

    @Test
    void keepsUnparseableOpenedAt() {
        String value = BugzillaTicketImportSource.convertOpenedAt("not-a-date", ZoneOffset.UTC, ZoneOffset.UTC);

        Assertions.assertEquals("not-a-date", value);
    }
}
