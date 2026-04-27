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
@Table(name = "ticket_import_records", uniqueConstraints = @UniqueConstraint(name = "uk_ticket_import_batch_source", columnNames = {
        "batch_id", "source_system", "source_key" }))
public class TicketImportRecord extends PanacheEntityBase {

    public static final String RESULT_CREATED = "created";
    public static final String RESULT_SKIPPED = "skipped";
    public static final String RESULT_FAILED = "failed";

    @Id
    @SequenceGenerator(name = "ticket_import_record_seq", sequenceName = "ticket_import_record_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ticket_import_record_seq")
    public Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    public TicketImportBatch batch;

    @Column(name = "source_system", nullable = false)
    public String sourceSystem;

    @Column(name = "source_key", nullable = false)
    public String sourceKey;

    @ManyToOne
    @JoinColumn(name = "ticket_id")
    public Ticket ticket;

    @Column(name = "row_number", nullable = false)
    public int rowNumber;

    @Column(nullable = false)
    public String result;

    @Column(name = "error_message", columnDefinition = "text")
    public String errorMessage;
}
