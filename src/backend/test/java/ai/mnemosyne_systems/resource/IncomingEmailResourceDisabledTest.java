/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(IncomingEmailResourceDisabledTest.IncomingMailDisabledProfile.class)
class IncomingEmailResourceDisabledTest {

    @Test
    void incomingMailEndpointReturnsNotFoundWhenDisabled() {
        RestAssured.given().contentType("multipart/form-data").multiPart("from", "user@mnemosyne-systems.ai")
                .multiPart("subject", "Disabled endpoint").multiPart("body", "Should not be processed")
                .post("/mail/incoming").then().statusCode(404);
    }

    public static class IncomingMailDisabledProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("ticket.mail.incoming.enabled", "false");
        }
    }
}
