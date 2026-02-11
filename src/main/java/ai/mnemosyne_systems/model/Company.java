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
import java.util.stream.Collectors;

@Entity
@Table(name = "companies")
public class Company extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "company_seq", sequenceName = "company_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "company_seq")
    public Long id;

    @Column(nullable = false)
    public String name;

    @Column(name = "ticket_sequence")
    public Long ticketSequence;

    public String address1;

    public String address2;

    public String city;

    public String state;

    public String zip;

    @ManyToOne
    @JoinColumn(name = "country_id")
    public Country country;

    @ManyToOne
    @JoinColumn(name = "timezone_id")
    public Timezone timezone;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<Ticket> tickets = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "company_users", joinColumns = @JoinColumn(name = "company_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    public List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<CompanyEntitlement> entitlements = new ArrayList<>();

    public String emailsDisplay() {
        if (users == null || users.isEmpty()) {
            return "";
        }
        return users.stream().map(user -> user.email).filter(email -> email != null && !email.isBlank())
                .map(email -> "<a href=\"mailto:" + email + "\">" + email + "</a>").collect(Collectors.joining(", "));
    }
}
