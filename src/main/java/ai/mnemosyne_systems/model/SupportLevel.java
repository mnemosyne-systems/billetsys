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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "support_levels")
public class SupportLevel extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "support_level_seq", sequenceName = "support_level_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "support_level_seq")
    public Long id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String description;

    @Column(nullable = false)
    public Integer critical;

    @Column(name = "critical_color", nullable = false)
    public String criticalColor;

    @Column(nullable = false)
    public Integer escalate;

    @Column(name = "escalate_color", nullable = false)
    public String escalateColor;

    @Column(nullable = false)
    public Integer normal;

    @Column(name = "normal_color", nullable = false)
    public String normalColor;
}
