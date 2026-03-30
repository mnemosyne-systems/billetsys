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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens", uniqueConstraints = @UniqueConstraint(name = "uk_password_reset_token_token", columnNames = "token"))
public class PasswordResetToken extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "password_reset_token_seq", sequenceName = "password_reset_token_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "password_reset_token_seq")
    public Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Column(nullable = false, unique = true)
    public String token;

    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;

    public static PasswordResetToken issue(User user, long ttlSeconds) {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.user = user;
        resetToken.token = UUID.randomUUID().toString();
        resetToken.expiresAt = Instant.now().plusSeconds(ttlSeconds);
        resetToken.persist();
        return resetToken;
    }
}
