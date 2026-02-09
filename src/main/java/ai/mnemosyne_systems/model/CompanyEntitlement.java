/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "company_entitlements")
public class CompanyEntitlement extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "company_entitlement_seq", sequenceName = "company_entitlement_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "company_entitlement_seq")
    public Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    public Company company;

    @ManyToOne(optional = false)
    @JoinColumn(name = "entitlement_id", nullable = false)
    public Entitlement entitlement;

    @ManyToOne(optional = false)
    @JoinColumn(name = "support_level_id", nullable = false)
    public SupportLevel supportLevel;
}
