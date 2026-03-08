/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "installations", uniqueConstraints = @UniqueConstraint(name = "uk_installation_singleton", columnNames = "singleton_key"))
public class Installation extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "installation_seq", sequenceName = "installation_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "installation_seq")
    public Long id;

    @Column(nullable = false)
    public String name;

    @Column(name = "singleton_key", nullable = false, unique = true, updatable = false)
    public String singletonKey = "installation";

    @OneToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    public Company company;
}
