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
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "installations", uniqueConstraints = @UniqueConstraint(name = "uk_installation_singleton", columnNames = "singleton_key"))
public class Installation extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "installation_seq", sequenceName = "installation_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "installation_seq")
    public Long id;

    @Column(nullable = false)
    public String name;

    @Column(name = "logo_base64", columnDefinition = "TEXT")
    public String logoBase64;

    @Column(name = "background_base64", columnDefinition = "TEXT")
    public String backgroundBase64;

    @Column(name = "header_footer_color")
    public String headerFooterColor;

    @Column(name = "headers_color")
    public String headersColor;

    @Column(name = "buttons_color")
    public String buttonsColor;

    @Column(name = "use_24_hour_clock")
    public Boolean use24HourClock;

    @Column(name = "ticket_auto_close_days")
    public Integer ticketAutoCloseDays;

    @Column(name = "singleton_key", nullable = false, unique = true, updatable = false)
    public String singletonKey = "installation";

    @Column(name = "admin_role_icon")
    public String adminRoleIcon;

    @Column(name = "support_role_icon")
    public String supportRoleIcon;

    @Column(name = "superuser_role_icon")
    public String superuserRoleIcon;

    @Column(name = "tam_role_icon")
    public String tamRoleIcon;

    @Column(name = "user_role_icon")
    public String userRoleIcon;

    @Column(name = "external_role_icon")
    public String externalRoleIcon;

    @Column(name = "admin_role_color")
    public String adminRoleColor;

    @Column(name = "support_role_color")
    public String supportRoleColor;

    @Column(name = "superuser_role_color")
    public String superuserRoleColor;

    @Column(name = "tam_role_color")
    public String tamRoleColor;

    @Column(name = "user_role_color")
    public String userRoleColor;

    @Column(name = "external_role_color")
    public String externalRoleColor;

    @OneToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    public Company company;
}
