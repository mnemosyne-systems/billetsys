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
import java.time.LocalDateTime;

@Entity
@Table(name = "cross_references", uniqueConstraints = @UniqueConstraint(name = "uk_cross_ref_msg_type_target", columnNames = {
        "message_id", "target_type", "target_id" }))
public class CrossReference extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "cross_reference_seq", sequenceName = "cross_reference_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cross_reference_seq")
    public Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    public Message sourceMessage;

    @ManyToOne(optional = false)
    @JoinColumn(name = "source_ticket_id", nullable = false)
    public Ticket sourceTicket;

    @Column(name = "target_type", nullable = false)
    public String targetType;

    @Column(name = "target_id", nullable = false)
    public Long targetId;

    @Column(nullable = false)
    public LocalDateTime createdAt;
}
