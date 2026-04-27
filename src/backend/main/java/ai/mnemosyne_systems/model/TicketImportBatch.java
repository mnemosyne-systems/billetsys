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
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_import_batches")
public class TicketImportBatch extends PanacheEntityBase {

    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_COMPLETED_WITH_ERRORS = "Completed with errors";
    public static final String STATUS_FAILED = "Failed";

    @Id
    @SequenceGenerator(name = "ticket_import_batch_seq", sequenceName = "ticket_import_batch_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ticket_import_batch_seq")
    public Long id;

    @Column(name = "source_type", nullable = false)
    public String sourceType;

    @Column(name = "file_name")
    public String fileName;

    @ManyToOne
    @JoinColumn(name = "actor_id")
    public User actor;

    @Column(name = "started_at", nullable = false)
    public LocalDateTime startedAt;

    @Column(name = "completed_at")
    public LocalDateTime completedAt;

    @Column(nullable = false)
    public String status;

    @Column(name = "created_count", nullable = false)
    public int createdCount;

    @Column(name = "skipped_count", nullable = false)
    public int skippedCount;

    @Column(name = "failed_count", nullable = false)
    public int failedCount;
}
