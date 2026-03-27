/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.infra;

import ai.mnemosyne_systems.model.Company;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@Named("branding")
@ApplicationScoped
public class BrandingProvider {

    public String installationCompanyName() {
        Company company = Company.find("select c from Company c where lower(c.name) = lower(?1)", "mnemosyne systems")
                .firstResult();
        if (company != null && company.name != null && !company.name.isBlank()) {
            return company.name;
        }
        return "billetsys";
    }
}
