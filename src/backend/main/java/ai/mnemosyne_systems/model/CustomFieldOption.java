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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "custom_field_options", uniqueConstraints = @UniqueConstraint(name = "uk_custom_field_option_label", columnNames = {
        "custom_field_id", "label" }))
public class CustomFieldOption extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "custom_field_option_seq", sequenceName = "custom_field_option_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "custom_field_option_seq")
    public Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "custom_field_id", nullable = false)
    public CustomField customField;

    @Column(nullable = false)
    public String label;

    @Column(name = "sort_order", nullable = false)
    public int sortOrder;
}
