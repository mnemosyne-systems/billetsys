/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "custom_fields", uniqueConstraints = @UniqueConstraint(name = "uk_custom_fields_name_key", columnNames = "name_key"))
public class CustomField extends PanacheEntityBase {

    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_NUMBER = "NUMBER";
    public static final String TYPE_DATE = "DATE";
    public static final String TYPE_SELECT = "SELECT";

    @Id
    @SequenceGenerator(name = "custom_field_seq", sequenceName = "custom_field_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "custom_field_seq")
    public Long id;

    @Column(nullable = false)
    public String name;

    @Column(name = "name_key", nullable = false)
    public String nameKey;

    @Column(name = "field_type", nullable = false, length = 16)
    public String fieldType;

    @Column(name = "is_required", nullable = false, columnDefinition = "boolean default false")
    public boolean required;

    @Column(columnDefinition = "text")
    public String description;

    @Column(nullable = false)
    public boolean active = true;

    @Column(name = "sort_order", nullable = false)
    public int sortOrder;

    @OneToMany(mappedBy = "customField", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc, id asc")
    public List<CustomFieldOption> options = new ArrayList<>();
}
