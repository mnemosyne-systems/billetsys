/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDateTime;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MessageSortOrderTest extends AccessTestSupport {

    @Test
    @TestSecurity(user = "user", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = "user@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "user") })
    void invalidMessageSortDirectionNormalizesToNull() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);

        RestAssured.given().contentType(ContentType.JSON)
                .body("{\"name\":\"user\",\"email\":\"user@mnemosyne-systems.ai\",\"messageSortDirection\":\"banana\"}")
                .post("/api/profile").then().statusCode(200).body("messageSortDirection", Matchers.nullValue());
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = "user@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "user") })
    void validMessageSortDirectionIsPersisted() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);

        RestAssured.given().contentType(ContentType.JSON)
                .body("{\"name\":\"user\",\"email\":\"user@mnemosyne-systems.ai\",\"messageSortDirection\":\"asc\"}")
                .post("/api/profile").then().statusCode(200).body("messageSortDirection", Matchers.equalTo("asc"));
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = "user@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "user") })
    void exportAscendingOrdersMessagesOldestFirst() throws Exception {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureDefaultCategories();
        Long companyId = ensureCompany("Export Order Asc Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyId);
        ensureTimedMessage(ticket, "OLDER_MESSAGE_MARKER", "support1@mnemosyne-systems.ai",
                LocalDateTime.of(2026, 1, 1, 9, 0));
        ensureTimedMessage(ticket, "NEWER_MESSAGE_MARKER", "support1@mnemosyne-systems.ai",
                LocalDateTime.of(2026, 1, 2, 9, 0));

        byte[] pdf = RestAssured.given().get("/tickets/export/" + ticket.id + "?dir=asc").then().statusCode(200)
                .extract().asByteArray();

        String text = extractText(pdf);
        int olderIndex = text.indexOf("OLDER_MESSAGE_MARKER");
        int newerIndex = text.indexOf("NEWER_MESSAGE_MARKER");
        Assertions.assertTrue(olderIndex >= 0 && newerIndex >= 0, "Both messages should appear in the PDF");
        Assertions.assertTrue(olderIndex < newerIndex, "Oldest message should appear before newest in asc order");
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = "user@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "user") })
    void exportDescendingOrdersMessagesNewestFirst() throws Exception {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureDefaultCategories();
        Long companyId = ensureCompany("Export Order Desc Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyId);
        ensureTimedMessage(ticket, "OLDER_MESSAGE_MARKER", "support1@mnemosyne-systems.ai",
                LocalDateTime.of(2026, 1, 1, 9, 0));
        ensureTimedMessage(ticket, "NEWER_MESSAGE_MARKER", "support1@mnemosyne-systems.ai",
                LocalDateTime.of(2026, 1, 2, 9, 0));

        byte[] pdf = RestAssured.given().get("/tickets/export/" + ticket.id + "?dir=desc").then().statusCode(200)
                .extract().asByteArray();

        String text = extractText(pdf);
        int olderIndex = text.indexOf("OLDER_MESSAGE_MARKER");
        int newerIndex = text.indexOf("NEWER_MESSAGE_MARKER");
        Assertions.assertTrue(olderIndex >= 0 && newerIndex >= 0, "Both messages should appear in the PDF");
        Assertions.assertTrue(newerIndex < olderIndex, "Newest message should appear before oldest in desc order");
    }

    private String extractText(byte[] pdf) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (PdfReader reader = new PdfReader(pdf)) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                sb.append(extractor.getTextFromPage(page));
            }
        }
        return sb.toString();
    }
}
