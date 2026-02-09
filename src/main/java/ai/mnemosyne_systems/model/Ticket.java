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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tickets")
public class Ticket extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "ticket_seq", sequenceName = "ticket_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ticket_seq")
    public Long id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    public Company company;

    @ManyToOne
    @JoinColumn(name = "requester_id")
    public User requester;

    @ManyToOne
    @JoinColumn(name = "company_entitlement_id")
    public CompanyEntitlement companyEntitlement;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<Message> messages = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "ticket_supports", joinColumns = @JoinColumn(name = "ticket_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    public List<User> supportUsers = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "ticket_tams", joinColumns = @JoinColumn(name = "ticket_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    public List<User> tamUsers = new ArrayList<>();

    public static String formatName(Company company, long sequence) {
        String base = company == null || company.name == null ? "" : company.name.trim();
        base = base.replaceAll("\\s+", "");
        if (base.isBlank()) {
            base = "COMP";
        }
        if (base.length() > 6) {
            base = base.substring(0, 6);
        }
        return base + "-" + String.format("%05d", sequence);
    }

    public static String previewNextName(Company company) {
        long current = currentSequence(company);
        return formatName(company, current + 1);
    }

    public static String nextName(Company company) {
        long current = currentSequence(company);
        long next = current + 1;
        if (company != null) {
            company.ticketSequence = next;
        }
        return formatName(company, next);
    }

    private static long currentSequence(Company company) {
        if (company == null) {
            return 0;
        }
        if (company.ticketSequence != null) {
            return company.ticketSequence;
        }
        return Ticket.count("company", company);
    }
}
