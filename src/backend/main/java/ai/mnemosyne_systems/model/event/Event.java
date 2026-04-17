package ai.mnemosyne_systems.model.event;

import ai.mnemosyne_systems.model.Company;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
public class Event extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public EventType entityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public EventAction action;

    @CreationTimestamp
    public LocalDateTime createdAt;

    @Column(nullable = false)
    public Long entityId;

    @ManyToOne
    @JoinColumn(name = "company_id")
    public Company company;

}
