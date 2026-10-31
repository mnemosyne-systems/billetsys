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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ticket_custom_field_values", uniqueConstraints = @UniqueConstraint(name = "uk_ticket_custom_field", columnNames = {
        "ticket_id", "custom_field_id" }), indexes = {
                @Index(name = "idx_ticket_custom_field_text", columnList = "custom_field_id, text_value"),
                @Index(name = "idx_ticket_custom_field_number", columnList = "custom_field_id, number_value"),
                @Index(name = "idx_ticket_custom_field_date", columnList = "custom_field_id, date_value") })
public class TicketCustomFieldValue extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "ticket_custom_field_value_seq", sequenceName = "ticket_custom_field_value_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ticket_custom_field_value_seq")
    public Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    public Ticket ticket;

    @ManyToOne(optional = false)
    @JoinColumn(name = "custom_field_id", nullable = false)
    public CustomField customField;

    @Column(name = "text_value", columnDefinition = "text")
    public String textValue;

    @Column(name = "number_value", precision = 38, scale = 10)
    public BigDecimal numberValue;

    @Column(name = "date_value")
    public LocalDate dateValue;
}
