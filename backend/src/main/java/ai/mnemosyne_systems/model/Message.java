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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "messages")
public class Message extends PanacheEntityBase implements Comparable<Message> {

    @Id
    @SequenceGenerator(name = "message_seq", sequenceName = "message_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_seq")
    public Long id;

    @Column(nullable = false, columnDefinition = "text")
    public String body;

    @Column(nullable = false)
    public LocalDateTime date;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    public Ticket ticket;

    @ManyToOne
    @JoinColumn(name = "author_id")
    public User author;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<Attachment> attachments = new ArrayList<>();

    @Override
    public int compareTo(Message other) {
        if (this.date == null && other.date == null)
            return 0;
        if (this.date == null)
            return 1;
        if (other.date == null)
            return -1;
        return this.date.compareTo(other.date);
    }

}
