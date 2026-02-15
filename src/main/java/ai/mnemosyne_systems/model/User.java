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
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User extends PanacheEntityBase {

    public static final String TYPE_SUPPORT = "support";
    public static final String TYPE_ADMIN = "admin";
    public static final String TYPE_USER = "user";
    public static final String TYPE_TAM = "tam";

    @Id
    @SequenceGenerator(name = "user_seq", sequenceName = "user_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    public Long id;

    @Column(nullable = false)
    public String name;

    @Column(name = "full_name")
    public String fullName;

    @Column(nullable = false)
    public String email;

    @Column
    public String social;

    @Column(name = "phone_number")
    public String phoneNumber;

    @Column(name = "phone_extension")
    public String phoneExtension;

    @ManyToOne
    @JoinColumn(name = "timezone_id")
    public Timezone timezone;

    @ManyToOne
    @JoinColumn(name = "country_id")
    public Country country;

    @Column(name = "user_type", nullable = false)
    public String type;

    @Column(name = "password_hash", nullable = false)
    public String passwordHash;

    @Column(name = "logo_base64", columnDefinition = "text")
    public String logoBase64;

    /**
     * Returns the formatted phone number with extension if available.
     */
    public String getFormattedPhone() {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }
        if (phoneExtension != null && !phoneExtension.isBlank()) {
            return phoneNumber + " ext. " + phoneExtension;
        }
        return phoneNumber;
    }

    /**
     * Returns the display name (full name if available, otherwise username).
     */
    public String getDisplayName() {
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }
        return name;
    }
}
