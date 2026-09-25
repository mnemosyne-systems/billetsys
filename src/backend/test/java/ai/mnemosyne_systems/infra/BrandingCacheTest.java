/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.infra;

import ai.mnemosyne_systems.model.Installation;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class BrandingCacheTest {

    private static final String PROBE_NAME = "Branding Cache Probe";

    @Inject
    BrandingService brandingService;

    @Test
    void snapshotIsCachedUntilInvalidated() {
        String originalName = sessionCompanyName();
        Assertions.assertNotNull(originalName);

        renameInstallation(PROBE_NAME);
        try {
            // Reads go through HTTP (fresh persistence context per request),
            // so a stale value here can only come from the branding cache.
            Assertions.assertEquals(originalName, sessionCompanyName());

            brandingService.invalidate();

            Assertions.assertEquals(PROBE_NAME, sessionCompanyName());
        } finally {
            renameInstallation(originalName);
            brandingService.invalidate();
        }
        Assertions.assertEquals(originalName, sessionCompanyName());
        RestAssured.given().get("/api/app/session").then().statusCode(200).body("installationCompanyName",
                Matchers.equalTo(originalName));
    }

    String sessionCompanyName() {
        return RestAssured.given().get("/api/app/session").then().statusCode(200).extract()
                .path("installationCompanyName");
    }

    void renameInstallation(String name) {
        QuarkusTransaction.requiringNew().run(() -> {
            Installation installation = Installation.find("singletonKey", "installation").firstResult();
            Assertions.assertNotNull(installation);
            installation.name = name;
        });
    }
}
