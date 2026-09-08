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

import java.time.LocalDate;

@Entity
@Table(name = "user_availability")
public class UserAvailability extends PanacheEntityBase {

    public static final String SCOPE_PERSONAL = "PERSONAL";
    public static final String SCOPE_COMPANY = "COMPANY";

    @Id
    @SequenceGenerator(name = "user_availability_seq", sequenceName = "user_availability_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_availability_seq")
    public Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    public User user;

    @ManyToOne
    @JoinColumn(name = "company_id")
    public Company company;

    @Column(name = "start_date", nullable = false)
    public LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    public LocalDate endDate;

    @Column(nullable = false)
    public String scope;

    @Column
    public String reason;
}
