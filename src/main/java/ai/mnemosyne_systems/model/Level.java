/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "support_levels")
public class Level extends PanacheEntityBase {
    public enum DayOption {
        MONDAY(1, "Monday"), TUESDAY(2, "Tuesday"), WEDNESDAY(3, "Wednesday"), THURSDAY(4, "Thursday"),
        FRIDAY(5, "Friday"), SATURDAY(6, "Saturday"), SUNDAY(7, "Sunday");

        private final int code;
        private final String label;

        DayOption(int code, String label) {
            this.code = code;
            this.label = label;
        }

        public int getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public static boolean isValid(Integer value) {
            if (value == null) {
                return false;
            }
            for (DayOption option : values()) {
                if (option.code == value) {
                    return true;
                }
            }
            return false;
        }
    }

    public enum HourOption {
        H00(0, "00:00"), H01(1, "01:00"), H02(2, "02:00"), H03(3, "03:00"), H04(4, "04:00"), H05(5, "05:00"),
        H06(6, "06:00"), H07(7, "07:00"), H08(8, "08:00"), H09(9, "09:00"), H10(10, "10:00"), H11(11, "11:00"),
        H12(12, "12:00"), H13(13, "13:00"), H14(14, "14:00"), H15(15, "15:00"), H16(16, "16:00"), H17(17, "17:00"),
        H18(18, "18:00"), H19(19, "19:00"), H20(20, "20:00"), H21(21, "21:00"), H22(22, "22:00"), H23(23, "23:00");

        private final int code;
        private final String label;

        HourOption(int code, String label) {
            this.code = code;
            this.label = label;
        }

        public int getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public static boolean isValid(Integer value) {
            return value != null && value >= 0 && value <= 23;
        }
    }

    @Id
    @SequenceGenerator(name = "support_level_seq", sequenceName = "support_level_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "support_level_seq")
    public Long id;

    public String name;

    public String description;

    public Integer level;
    public String color;

    public Integer fromDay;
    public Integer fromTime;
    public Integer toDay;
    public Integer toTime;

    @ManyToOne
    @JoinColumn(name = "country_id")
    public Country country;

    @ManyToOne
    @JoinColumn(name = "timezone_id")
    public Timezone timezone;
}
