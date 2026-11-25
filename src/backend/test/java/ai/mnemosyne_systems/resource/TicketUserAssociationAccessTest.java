/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TicketUserAssociationAccessTest extends AccessTestSupport {

    private List<Long> participantIds(Long ticketId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Ticket t = Ticket.findById(ticketId);
            return t.userUsers.stream().map(u -> u.id).toList();
        });
    }

    private int externalCount(Long ticketId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Ticket t = Ticket.findById(ticketId);
            return t.externalUsers.size();
        });
    }

    @Test
    @TestSecurity(user = "other-superuser", roles = "superuser")
    @JwtSecurity(claims = { @Claim(key = "email", value = "other-superuser@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "other-superuser") })
    void superuserCannotModifyParticipantsOnOtherCompanyTicket() {
        ensureUser("owner-user", "owner-user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("other-superuser", "other-superuser@mnemosyne-systems.ai", User.TYPE_SUPERUSER);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureDefaultCategories();
        Long ownerCompanyId = ensureCompany("Assoc Owner Co");
        Long otherCompanyId = ensureCompany("Assoc Other Co");
        ensureCompanyUsers(ownerCompanyId, "owner-user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        ensureCompanyUsers(otherCompanyId, "other-superuser@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(ownerCompanyId);

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("email", "intruder@mnemosyne-systems.ai")
                .post("/api/superuser/tickets/" + ticket.id + "/participants/add").then().statusCode(404);

        Assertions.assertTrue(participantIds(ticket.id).isEmpty());
    }

    @Test
    @TestSecurity(user = "other-user", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "email", value = "other-user@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "other-user") })
    void userCannotModifyParticipantsOnAnotherUsersTicket() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("other-user", "other-user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureDefaultCategories();
        Long companyId = ensureCompany("Assoc Same Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "other-user@mnemosyne-systems.ai",
                "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyId);

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("email", "extra@mnemosyne-systems.ai")
                .post("/api/user/tickets/" + ticket.id + "/participants/add").then().statusCode(404);

        Assertions.assertTrue(participantIds(ticket.id).isEmpty());
    }

    @Test
    @TestSecurity(user = "other-tam", roles = "tam")
    @JwtSecurity(claims = { @Claim(key = "email", value = "other-tam@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "other-tam") })
    void tamCannotModifyExternalsOnOtherCompanyTicket() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureUser("other-tam", "other-tam@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureDefaultCategories();
        Long ownerCompanyId = ensureCompany("Ext Owner Co");
        Long otherCompanyId = ensureCompany("Ext Other Co");
        ensureCompanyUsers(ownerCompanyId, "user@mnemosyne-systems.ai", "tam1@mnemosyne-systems.ai");
        ensureCompanyUsers(otherCompanyId, "other-tam@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(ownerCompanyId);

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("email", "ext@mnemosyne-systems.ai").post("/api/tam/tickets/" + ticket.id + "/externals/add")
                .then().statusCode(404);

        Assertions.assertEquals(0, externalCount(ticket.id));
    }

    @Test
    @TestSecurity(user = "superuser1", roles = "superuser")
    @JwtSecurity(claims = { @Claim(key = "email", value = "superuser1@mnemosyne-systems.ai"),
            @Claim(key = "sub", value = "superuser1") })
    void superuserCanAddAndRemoveParticipantOnOwnCompanyTicket() {
        ensureUser("user", "user@mnemosyne-systems.ai", User.TYPE_USER);
        ensureUser("superuser1", "superuser1@mnemosyne-systems.ai", User.TYPE_SUPERUSER);
        ensureUser("support1", "support1@mnemosyne-systems.ai", User.TYPE_SUPPORT);
        ensureUser("tam1", "tam1@mnemosyne-systems.ai", User.TYPE_TAM);
        ensureDefaultCategories();
        Long companyId = ensureCompany("Assoc Allowed Co");
        ensureCompanyUsers(companyId, "user@mnemosyne-systems.ai", "superuser1@mnemosyne-systems.ai",
                "tam1@mnemosyne-systems.ai");
        Ticket ticket = ensureTicket(companyId);

        RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("email", "invited-participant@mnemosyne-systems.ai")
                .post("/api/superuser/tickets/" + ticket.id + "/participants/add").then().statusCode(303);

        List<Long> ids = participantIds(ticket.id);
        Assertions.assertEquals(1, ids.size());
        Long participantId = ids.get(0);

        RestAssured.given().redirects().follow(false)
                .post("/api/superuser/tickets/" + ticket.id + "/participants/" + participantId + "/remove").then()
                .statusCode(303);

        Assertions.assertTrue(participantIds(ticket.id).isEmpty());
    }
}
