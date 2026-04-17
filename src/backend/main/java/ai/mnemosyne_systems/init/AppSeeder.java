/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.init;

import ai.mnemosyne_systems.infra.BrandingProvider;
import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.Attachment;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Entitlement;
import ai.mnemosyne_systems.model.Installation;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Level;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.event.EventType;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;
import ai.mnemosyne_systems.model.Version;

@ApplicationScoped
public class AppSeeder {

    @Inject
    BrandingProvider brandingProvider;

    void onStart(@Observes StartupEvent event) {
        seedCountriesAndTimezones();
        seedDefaults();
        seedSupportCatalog();
        seedSampleData();
        seedEventTypes();
    }

    @Transactional
    void seedCountriesAndTimezones() {
        if (Country.count() > 0) {
            return;
        }

        seedCountry("United States", "US", "America/New_York", "America/Chicago", "America/Denver",
                "America/Los_Angeles", "America/Anchorage", "Pacific/Honolulu");
        seedCountry("United Kingdom", "GB", "Europe/London");
        seedCountry("Canada", "CA", "America/Toronto", "America/Vancouver", "America/Edmonton", "America/Halifax");
        seedCountry("Australia", "AU", "Australia/Sydney", "Australia/Melbourne", "Australia/Brisbane",
                "Australia/Perth");
        seedCountry("Germany", "DE", "Europe/Berlin");
        seedCountry("France", "FR", "Europe/Paris");
        seedCountry("Japan", "JP", "Asia/Tokyo");
        seedCountry("China", "CN", "Asia/Shanghai");
        seedCountry("India", "IN", "Asia/Kolkata");
        seedCountry("Brazil", "BR", "America/Sao_Paulo");
        seedCountry("Egypt", "EG", "Africa/Cairo");
        seedCountry("South Africa", "ZA", "Africa/Johannesburg");
        seedCountry("Mexico", "MX", "America/Mexico_City");
        seedCountry("Spain", "ES", "Europe/Madrid");
        seedCountry("Italy", "IT", "Europe/Rome");
        seedCountry("Netherlands", "NL", "Europe/Amsterdam");
        seedCountry("Sweden", "SE", "Europe/Stockholm");
        seedCountry("Norway", "NO", "Europe/Oslo");
        seedCountry("Denmark", "DK", "Europe/Copenhagen");
        seedCountry("Finland", "FI", "Europe/Helsinki");
        seedCountry("Poland", "PL", "Europe/Warsaw");
        seedCountry("Russia", "RU", "Europe/Moscow");
        seedCountry("South Korea", "KR", "Asia/Seoul");
        seedCountry("Singapore", "SG", "Asia/Singapore");
        seedCountry("United Arab Emirates", "AE", "Asia/Dubai");
        seedCountry("Saudi Arabia", "SA", "Asia/Riyadh");
        seedCountry("Israel", "IL", "Asia/Jerusalem");
        seedCountry("Turkey", "TR", "Europe/Istanbul");
        seedCountry("Argentina", "AR", "America/Buenos_Aires");
        seedCountry("Chile", "CL", "America/Santiago");
        seedCountry("Colombia", "CO", "America/Bogota");
        seedCountry("Peru", "PE", "America/Lima");
        seedCountry("New Zealand", "NZ", "Pacific/Auckland");
        seedCountry("Ireland", "IE", "Europe/Dublin");
        seedCountry("Switzerland", "CH", "Europe/Zurich");
        seedCountry("Austria", "AT", "Europe/Vienna");
        seedCountry("Belgium", "BE", "Europe/Brussels");
        seedCountry("Portugal", "PT", "Europe/Lisbon");
        seedCountry("Greece", "GR", "Europe/Athens");
        seedCountry("Czech Republic", "CZ", "Europe/Prague");
        seedCountry("Romania", "RO", "Europe/Bucharest");
        seedCountry("Hungary", "HU", "Europe/Budapest");
        seedCountry("Ukraine", "UA", "Europe/Kiev");
        seedCountry("Thailand", "TH", "Asia/Bangkok");
        seedCountry("Vietnam", "VN", "Asia/Ho_Chi_Minh");
        seedCountry("Indonesia", "ID", "Asia/Jakarta");
        seedCountry("Malaysia", "MY", "Asia/Kuala_Lumpur");
        seedCountry("Philippines", "PH", "Asia/Manila");
        seedCountry("Pakistan", "PK", "Asia/Karachi");
        seedCountry("Bangladesh", "BD", "Asia/Dhaka");
    }

    private void seedCountry(String name, String code, String... timezoneNames) {
        Country country = Country.find("code", code).firstResult();
        if (country == null) {
            country = new Country();
            country.name = name;
            country.code = code;
            country.persist();
        }
        for (String tzName : timezoneNames) {
            Timezone existing = Timezone.find("name", tzName).firstResult();
            if (existing == null) {
                Timezone tz = new Timezone();
                tz.name = tzName;
                tz.country = country;
                tz.persist();
            }
        }
    }

    @Transactional
    void seedDefaults() {
        seedUser("admin", "System Administrator", "admin@mnemosyne-systems.ai", "+1-555-0100", null, "America/New_York",
                "US", User.TYPE_ADMIN, "admin");
        seedUser("support1", "Sarah Johnson", "support1@mnemosyne-systems.ai", "+1-555-0101", "101", "America/New_York",
                "US", User.TYPE_SUPPORT, "support1");
        seedUser("support2", "Michael Chen", "support2@mnemosyne-systems.ai", "+1-555-0102", "102",
                "America/Los_Angeles", "US", User.TYPE_SUPPORT, "support2");
        seedUser("tam1", "Technical Account Manager", "tam1@mnemosyne-systems.ai", "+1-555-0300", "300",
                "America/Chicago", "US", User.TYPE_TAM, "tam1");
        removeUser("user@mnemosyne-systems.ai");
        removeUser("support@mnemosyne-systems.ai");
    }

    @Transactional
    void seedSampleData() {
        User user1 = seedUser("user1", "John Doe", "user1@mnemosyne-systems.ai", "+1-555-0201", null, "Europe/London",
                "GB", User.TYPE_USER, "user1");
        User user2 = seedUser("user2", "Jane Smith", "user2@mnemosyne-systems.ai", "+1-555-0202", null, "Europe/Paris",
                "FR", User.TYPE_USER, "user2");
        User userB = seedUser("userb", "Bob Brown", "userb@mnemosyne-systems.ai", "+1-555-0203", null,
                "America/New_York", "US", User.TYPE_USER, "userb");
        User tam1 = seedUser("tam1", "Technical Account Manager 1", "tam1@mnemosyne-systems.ai", "+1-555-0300", "300",
                "America/Chicago", "US", User.TYPE_TAM, "tam1");
        User tam2 = seedUser("tam2", "Technical Account Manager 2", "tam2@mnemosyne-systems.ai", "+1-555-0311", "301",
                "America/Chicago", "US", User.TYPE_TAM, "tam2");
        User superuser1 = seedUser("superuser1", "Superuser 1", "superuser1@mnemosyne-systems.ai", "+1-555-0401", "401",
                "America/New_York", "US", User.TYPE_SUPERUSER, "superuser1");
        User superuser2 = seedUser("superuser2", "Superuser 2", "superuser2@mnemosyne-systems.ai", "+1-555-0402", "402",
                "America/New_York", "US", User.TYPE_SUPERUSER, "superuser2");

        Company company = Company
                .find("select distinct c from Company c left join fetch c.users where c.name = ?1", "A").firstResult();
        if (company == null) {
            company = new Company();
            company.name = "A";
            company.country = findCountryByCode("US");
            company.timezone = findTimezoneByName("America/New_York");
            company.persist();
        }
        if (company.country == null) {
            company.country = findCountryByCode("US");
        }
        if (company.timezone == null) {
            company.timezone = findTimezoneByName("America/New_York");
        }
        company.users.removeIf(existing -> User.TYPE_ADMIN.equalsIgnoreCase(existing.type)
                || User.TYPE_SUPPORT.equalsIgnoreCase(existing.type));
        company.users.removeIf(existing -> User.TYPE_TAM.equalsIgnoreCase(existing.type)
                && !"tam1@mnemosyne-systems.ai".equalsIgnoreCase(existing.email));
        company.users.removeIf(existing -> User.TYPE_SUPERUSER.equalsIgnoreCase(existing.type)
                && !"superuser1@mnemosyne-systems.ai".equalsIgnoreCase(existing.email));
        addUserIfMissing(company, user1);
        addUserIfMissing(company, user2);
        addUserIfMissing(company, tam1);
        addUserIfMissing(company, superuser1);

        ensureCompanyEntitlement(company, "Enterprise", "Escalate");
        ensureCompanyEntitlement(company, "Enterprise", "Normal");
        CompanyEntitlement enterpriseCritical = ensureCompanyEntitlement(company, "Enterprise", "Critical");
        CompanyEntitlement enterpriseEscalate = ensureCompanyEntitlement(company, "Enterprise", "Escalate");
        CompanyEntitlement enterpriseNormal = ensureCompanyEntitlement(company, "Enterprise", "Normal");
        Ticket a1 = seedTicket(Ticket.formatName(company, 1), company, user1, enterpriseCritical);
        Ticket a2 = seedTicket(Ticket.formatName(company, 2), company, user2, enterpriseCritical);
        Ticket a3 = seedTicket(Ticket.formatName(company, 3), company, user1, enterpriseCritical);
        Ticket a4 = seedTicket(Ticket.formatName(company, 4), company, user2, enterpriseCritical);
        // Additional tickets for pagination testing (need 25+ total to span 3 pages at pageSize=10)
        Ticket a5 = seedTicket(Ticket.formatName(company, 5), company, user1, enterpriseEscalate);
        Ticket a6 = seedTicket(Ticket.formatName(company, 6), company, user2, enterpriseNormal);
        Ticket a7 = seedTicket(Ticket.formatName(company, 7), company, user1, enterpriseCritical);
        Ticket a8 = seedTicket(Ticket.formatName(company, 8), company, user2, enterpriseEscalate);
        Ticket a9 = seedTicket(Ticket.formatName(company, 9), company, user1, enterpriseNormal);
        Ticket a10 = seedTicket(Ticket.formatName(company, 10), company, user2, enterpriseCritical);
        Ticket a11 = seedTicket(Ticket.formatName(company, 11), company, user1, enterpriseEscalate);
        Ticket a12 = seedTicket(Ticket.formatName(company, 12), company, user2, enterpriseNormal);
        Ticket a13 = seedTicket(Ticket.formatName(company, 13), company, user1, enterpriseCritical);
        Ticket a14 = seedTicket(Ticket.formatName(company, 14), company, user2, enterpriseEscalate);
        Ticket a15 = seedTicket(Ticket.formatName(company, 15), company, user1, enterpriseNormal);
        Ticket a16 = seedTicket(Ticket.formatName(company, 16), company, user2, enterpriseCritical);
        Ticket a17 = seedTicket(Ticket.formatName(company, 17), company, user1, enterpriseEscalate);
        Ticket a18 = seedTicket(Ticket.formatName(company, 18), company, user2, enterpriseNormal);
        Ticket a19 = seedTicket(Ticket.formatName(company, 19), company, user1, enterpriseCritical);
        Ticket a20 = seedTicket(Ticket.formatName(company, 20), company, user2, enterpriseEscalate);
        Ticket a21 = seedTicket(Ticket.formatName(company, 21), company, user1, enterpriseNormal);
        Ticket a22 = seedTicket(Ticket.formatName(company, 22), company, user2, enterpriseCritical);
        Ticket a23 = seedTicket(Ticket.formatName(company, 23), company, user1, enterpriseEscalate);
        Ticket a24 = seedTicket(Ticket.formatName(company, 24), company, user2, enterpriseNormal);
        Ticket a25 = seedTicket(Ticket.formatName(company, 25), company, user1, enterpriseCritical);
        company.ticketSequence = 25L;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        seedMessageAt(a1, "Sample ticket created.", now.minusMonths(8));
        seedMessageAt(a2, "Sample ticket created.", now.minusMonths(3));
        seedMessageAt(a3, "Sample ticket created.", now.minusMonths(1));
        seedMessageAt(a4, "Sample ticket created.", now.minusDays(13));
        // Spread message dates across the year for varied sorting
        seedMessageAt(a5, "Sample ticket created.", now.minusMonths(11));
        seedMessageAt(a6, "Sample ticket created.", now.minusMonths(10));
        seedMessageAt(a7, "Sample ticket created.", now.minusMonths(9));
        seedMessageAt(a8, "Sample ticket created.", now.minusMonths(7));
        seedMessageAt(a9, "Sample ticket created.", now.minusMonths(6));
        seedMessageAt(a10, "Sample ticket created.", now.minusMonths(5));
        seedMessageAt(a11, "Sample ticket created.", now.minusMonths(4));
        seedMessageAt(a12, "Sample ticket created.", now.minusMonths(2));
        seedMessageAt(a13, "Sample ticket created.", now.minusDays(45));
        seedMessageAt(a14, "Sample ticket created.", now.minusDays(30));
        seedMessageAt(a15, "Sample ticket created.", now.minusDays(20));
        seedMessageAt(a16, "Sample ticket created.", now.minusDays(15));
        seedMessageAt(a17, "Sample ticket created.", now.minusDays(10));
        seedMessageAt(a18, "Sample ticket created.", now.minusDays(8));
        seedMessageAt(a19, "Sample ticket created.", now.minusDays(6));
        seedMessageAt(a20, "Sample ticket created.", now.minusDays(5));
        seedMessageAt(a21, "Sample ticket created.", now.minusDays(4));
        seedMessageAt(a22, "Sample ticket created.", now.minusDays(3));
        seedMessageAt(a23, "Sample ticket created.", now.minusDays(2));
        seedMessageAt(a24, "Sample ticket created.", now.minusDays(1));
        seedMessageAt(a25, "Sample ticket created.", now.minusHours(2));
        Message attachmentMessage = Message.find("ticket = ?1 and body = ?2", a1, "Sample ticket created.")
                .firstResult();
        seedAttachment(attachmentMessage, "sample.txt", "text/plain",
                "Sample attachment\nLine two".getBytes(StandardCharsets.UTF_8));
        seedMessageAt(a1, "Sample attachments added.", now.minusMonths(8).plusMinutes(5));
        Message attachmentMessageTwo = Message.find("ticket = ?1 and body = ?2", a1, "Sample attachments added.")
                .firstResult();
        seedAttachment(attachmentMessageTwo, "sample-one.txt", "text/plain",
                "First attachment".getBytes(StandardCharsets.UTF_8));
        seedAttachment(attachmentMessageTwo, "sample-two.txt", "text/plain",
                "Second attachment".getBytes(StandardCharsets.UTF_8));
        assignSupportIfMissing(a1, "support1@mnemosyne-systems.ai");
        assignSupportIfMissing(a4, "support1@mnemosyne-systems.ai");
        assignSupportIfMissing(a7, "support1@mnemosyne-systems.ai");
        assignSupportIfMissing(a10, "support1@mnemosyne-systems.ai");
        assignSupportIfMissing(a13, "support1@mnemosyne-systems.ai");
        assignSupportIfMissing(a16, "support1@mnemosyne-systems.ai");
        assignSupportIfMissing(a19, "support1@mnemosyne-systems.ai");
        assignSupportIfMissing(a22, "support1@mnemosyne-systems.ai");
        assignSupportIfMissing(a25, "support1@mnemosyne-systems.ai");
        assignTamIfMissing(a1, "tam1@mnemosyne-systems.ai");
        assignTamIfMissing(a4, "tam1@mnemosyne-systems.ai");
        // Set varied statuses
        if (a4 != null) {
            a4.status = "Closed";
            a4.persist();
        }
        if (a5 != null) {
            a5.status = "Assigned";
            a5.persist();
        }
        if (a6 != null) {
            a6.status = "In Progress";
            a6.persist();
        }
        if (a8 != null) {
            a8.status = "Resolved";
            a8.persist();
        }
        if (a9 != null) {
            a9.status = "Closed";
            a9.persist();
        }
        if (a10 != null) {
            a10.status = "Assigned";
            a10.persist();
        }
        if (a12 != null) {
            a12.status = "In Progress";
            a12.persist();
        }
        if (a14 != null) {
            a14.status = "Resolved";
            a14.persist();
        }
        if (a16 != null) {
            a16.status = "Closed";
            a16.persist();
        }
        if (a18 != null) {
            a18.status = "Assigned";
            a18.persist();
        }
        if (a20 != null) {
            a20.status = "In Progress";
            a20.persist();
        }
        if (a22 != null) {
            a22.status = "Resolved";
            a22.persist();
        }
        if (a24 != null) {
            a24.status = "Closed";
            a24.persist();
        }

        Category bugCategory = Category.find("name", "Bug").firstResult();
        Category featureCategory = Category.find("name", "Feature").firstResult();
        Category questionCategory = Category.find("name", "Question").firstResult();
        if (a1 != null) {
            a1.externalIssueLink = "https://github.com/mnemosyne-systems/billetsys/issues/6";
            if (bugCategory != null) {
                a1.category = bugCategory;
            }
            a1.persist();
        }
        if (a3 != null && featureCategory != null) {
            a3.category = featureCategory;
            a3.persist();
        }
        // Distribute categories across tickets for varied sort results
        setCategory(a5, featureCategory);
        setCategory(a7, bugCategory);
        setCategory(a9, featureCategory);
        setCategory(a11, questionCategory);
        setCategory(a13, bugCategory);
        setCategory(a15, featureCategory);
        setCategory(a17, questionCategory);
        setCategory(a19, bugCategory);
        setCategory(a21, featureCategory);
        setCategory(a23, questionCategory);
        setCategory(a25, bugCategory);
        // Set descriptive titles for some tickets
        setTitle(a5, "Login page intermittent timeout");
        setTitle(a6, "Add dark mode support");
        setTitle(a7, "PDF export produces blank pages");
        setTitle(a8, "Improve search autocomplete");
        setTitle(a9, "API rate limiting not applied");
        setTitle(a10, "Dashboard graph not updating");
        setTitle(a11, "How to configure SSO?");
        setTitle(a12, "Email notifications delayed");
        setTitle(a13, "Crash on large file upload");
        setTitle(a14, "Add keyboard shortcuts help dialog");
        setTitle(a15, "Request for bulk ticket import");
        setTitle(a16, "Memory leak in report generation");
        setTitle(a17, "Can we export ticket history?");
        setTitle(a18, "Audit log missing user actions");
        setTitle(a19, "XSS vulnerability in comments");
        setTitle(a20, "Add webhook integration");
        setTitle(a21, "Request custom branding options");
        setTitle(a22, "Date picker shows wrong timezone");
        setTitle(a23, "What are the backup procedures?");
        setTitle(a24, "Two-factor authentication setup");
        setTitle(a25, "Connection pool exhaustion under load");

        User support1 = User.find("email", "support1@mnemosyne-systems.ai").firstResult();
        if (support1 != null) {
            seedReplyAt(a1, "Looking into this issue.", support1, now.minusMonths(8).plusMinutes(50));
            seedReplyAt(a4, "This has been resolved.", support1, now.minusDays(13).plusMinutes(20));
        }

        Company companyB = Company
                .find("select distinct c from Company c left join fetch c.users where c.name = ?1", "B").firstResult();
        if (companyB == null) {
            companyB = new Company();
            companyB.name = "B";
            companyB.country = findCountryByCode("US");
            companyB.timezone = findTimezoneByName("America/New_York");
            companyB.persist();
        }
        if (companyB.country == null) {
            companyB.country = findCountryByCode("US");
        }
        if (companyB.timezone == null) {
            companyB.timezone = findTimezoneByName("America/New_York");
        }
        companyB.users.removeIf(existing -> User.TYPE_ADMIN.equalsIgnoreCase(existing.type)
                || User.TYPE_SUPPORT.equalsIgnoreCase(existing.type));
        companyB.users.removeIf(existing -> User.TYPE_TAM.equalsIgnoreCase(existing.type)
                && !"tam2@mnemosyne-systems.ai".equalsIgnoreCase(existing.email));
        companyB.users.removeIf(existing -> User.TYPE_SUPERUSER.equalsIgnoreCase(existing.type)
                && !"superuser2@mnemosyne-systems.ai".equalsIgnoreCase(existing.email));
        addUserIfMissing(companyB, userB);
        addUserIfMissing(companyB, tam2);
        addUserIfMissing(companyB, superuser2);
        CompanyEntitlement starterCritical = ensureCompanyEntitlement(companyB, "Starter", "Critical");
        if (starterCritical != null) {
            starterCritical.duration = CompanyEntitlement.DURATION_MONTHLY;
            starterCritical.date = java.time.LocalDate.now().minusMonths(2);
            starterCritical.persist();
        }
        Ticket b1 = seedTicket(Ticket.formatName(companyB, 1), companyB, userB, starterCritical);
        companyB.ticketSequence = 1L;
        seedMessageAt(b1, "Sample ticket created.", now.minusDays(2));
        if (b1 != null && bugCategory != null) {
            b1.category = bugCategory;
            b1.persist();
        }
        ensureOwnerCompany(null);
    }

    @Transactional
    void seedSupportCatalog() {
        seedEntitlement("Starter", "Email support with 2 business day response");
        seedEntitlement("Business", "Priority support with 1 business day response");
        seedEntitlement("Enterprise", "24/7 support with SLA and dedicated TAM");

        seedLevel("Critical", "Critical response level", 60, "Red");
        seedLevel("Escalate", "Escalation response level", 120, "Yellow");
        seedLevel("Normal", "Normal response level", 1440, "White");
        assignLevelsToEntitlement("Starter", "Critical", "Escalate", "Normal");
        assignLevelsToEntitlement("Business", "Critical", "Escalate", "Normal");
        assignLevelsToEntitlement("Enterprise", "Critical", "Escalate", "Normal");

        seedCategory("Feature", false);
        seedCategory("Bug", false);
        seedCategory("Question", true);
    }

    @Transactional
    void seedEventTypes() {
        // CATEGORY EVENTS
        seedEventType("BUG", "CATEGORY");
        seedEventType("FEATURE", "CATEGORY");
        seedEventType("QUESTION", "CATEGORY");

        // ACTION EVENTS
        seedEventType("CREATED", "ACTION");
        seedEventType("UPDATED", "ACTION");
        seedEventType("DELETED", "ACTION");
        seedEventType("OPEN", "ACTION");
        seedEventType("CLOSED", "ACTION");
        seedEventType("EXPIRED", "ACTION");
        seedEventType("ASSIGNED", "ACTION");
        seedEventType("IN PROGRESS", "ACTION");
        seedEventType("RESOLVED", "ACTION");
        seedEventType("RESOLVED", "ACTION");

        // UNKNOW
        seedEventType("UNKNOWN", "UNKNOWN");

    }

    private EventType seedEventType(String name, String type) {
        EventType eventType;
        eventType = EventType.find("name = ?1 AND type = ?2", name, type).firstResult();
        if (eventType == null) {
            eventType = new EventType();
            eventType.name = name;
            eventType.type = type;
            eventType.persist();
        }
        return eventType;
    }

    private Country findCountryByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Country.find("code", code).firstResult();
    }

    private Timezone findTimezoneByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return Timezone.find("name", name).firstResult();
    }

    private User seedUser(String username, String fullName, String email, String phoneNumber, String phoneExtension,
            String timezoneName, String countryCode, String type, String password) {
        User user = User.find("email", email).firstResult();
        Country country = findCountryByCode(countryCode);
        Timezone timezone = findTimezoneByName(timezoneName);

        if (user == null) {
            user = new User();
        }
        user.name = username;
        user.fullName = fullName;
        user.email = email;
        user.phoneNumber = phoneNumber;
        user.phoneExtension = phoneExtension;
        user.timezone = timezone;
        user.country = country;
        user.type = type;
        if (password != null && !password.isBlank()) {
            user.passwordHash = BcryptUtil.bcryptHash(password);
        }
        if (user.id == null) {
            user.persist();
        }
        return user;
    }

    private void addUserIfMissing(Company company, User user) {
        boolean exists = company.users.stream()
                .anyMatch(existing -> existing.id != null && existing.id.equals(user.id));
        if (!exists) {
            company.users.add(user);
        }
    }

    private void ensureOwnerCompany(User superuser) {
        Company ownerCompany = Company
                .find("select distinct c from Company c left join fetch c.users where lower(c.name) = lower(?1)",
                        "mnemosyne systems")
                .firstResult();
        if (ownerCompany == null) {
            ownerCompany = new Company();
            ownerCompany.name = "mnemosyne systems";
            ownerCompany.country = findCountryByCode("US");
            ownerCompany.timezone = findTimezoneByName("America/New_York");
            ownerCompany.persist();
        }
        if (ownerCompany.country == null) {
            ownerCompany.country = findCountryByCode("US");
        }
        if (ownerCompany.timezone == null) {
            ownerCompany.timezone = findTimezoneByName("America/New_York");
        }
        User adminUser = User.find("email", "admin@mnemosyne-systems.ai").firstResult();
        if (adminUser != null) {
            addUserIfMissing(ownerCompany, adminUser);
        }
        List<User> ownerUsers = User.list("type in ?1 order by name", List.of(User.TYPE_SUPPORT, User.TYPE_TAM));
        for (User ownerUser : ownerUsers) {
            addUserIfMissing(ownerCompany, ownerUser);
        }
        ensureInstallation(ownerCompany, "mnemosyne systems");
    }

    private void ensureInstallation(Company company, String name) {
        if (company == null || name == null || name.isBlank()) {
            return;
        }
        List<Installation> installations = Installation.list("order by id");
        Installation singleton = installations.isEmpty() ? null : installations.get(0);
        if (singleton == null) {
            singleton = new Installation();
            singleton.singletonKey = "installation";
        } else {
            for (int index = 1; index < installations.size(); index++) {
                installations.get(index).delete();
            }
        }
        singleton.company = company;
        singleton.name = name;
        if (singleton.logoBase64 == null || singleton.logoBase64.isBlank()) {
            singleton.logoBase64 = brandingProvider.defaultInstallationLogoBase64();
        }
        String seededColor = BrandingProvider.normalizeInstallationColor(
                firstNonBlank(singleton.headerFooterColor, singleton.headersColor, singleton.buttonsColor));
        if (singleton.headerFooterColor == null || singleton.headerFooterColor.isBlank()) {
            singleton.headerFooterColor = seededColor;
        }
        if (singleton.headersColor == null || singleton.headersColor.isBlank()) {
            singleton.headersColor = seededColor;
        }
        if (singleton.buttonsColor == null || singleton.buttonsColor.isBlank()) {
            singleton.buttonsColor = seededColor;
        }
        if (singleton.use24HourClock == null) {
            singleton.use24HourClock = false;
        }
        singleton.singletonKey = "installation";
        singleton.persist();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Ticket seedTicket(String name, Company company, User requester, CompanyEntitlement entitlement) {
        Ticket ticket = Ticket.find("name", name).firstResult();
        if (ticket == null) {
            ticket = new Ticket();
            ticket.name = name;
            ticket.title = name;
            ticket.status = "Open";
            ticket.company = company;
            ticket.requester = requester;
            ticket.companyEntitlement = entitlement;
            ticket.category = Category.findDefault();
            ticket.persist();
        }
        if (ticket.category == null) {
            ticket.category = Category.findDefault();
        }
        if (ticket.status == null || ticket.status.isBlank()) {
            ticket.status = "Open";
        }
        if (Ticket.normalizeTitle(ticket.title) == null) {
            ticket.title = ticket.name;
        }
        ticket.company = company;
        ticket.requester = requester;
        ticket.companyEntitlement = entitlement;
        if (ticket.affectsVersion == null) {
            ticket.affectsVersion = defaultAffectsVersion(entitlement);
        }
        ticket.persist();
        seedMessage(ticket, "Sample ticket created.");
        return ticket;
    }

    private void setCategory(Ticket ticket, Category category) {
        if (ticket != null && category != null) {
            ticket.category = category;
            ticket.persist();
        }
    }

    private void setTitle(Ticket ticket, String title) {
        if (ticket != null && title != null) {
            ticket.title = title;
            ticket.persist();
        }
    }

    private Version defaultAffectsVersion(CompanyEntitlement entitlement) {
        if (entitlement == null || entitlement.entitlement == null) {
            return null;
        }
        Version version = Version.find("entitlement = ?1 and name = ?2 order by id", entitlement.entitlement, "1.0.0")
                .firstResult();
        if (version != null) {
            return version;
        }
        return Version.find("entitlement = ?1 order by id", entitlement.entitlement).firstResult();
    }

    private void seedMessage(Ticket ticket, String body) {
        if (ai.mnemosyne_systems.model.Message.find("ticket = ?1", ticket).firstResult() != null) {
            return;
        }
        ai.mnemosyne_systems.model.Message message = new ai.mnemosyne_systems.model.Message();
        message.ticket = ticket;
        message.body = body;
        message.date = java.time.LocalDateTime.now();
        message.author = ticket.requester;
        message.persist();
    }

    private void seedMessageAt(Ticket ticket, String body, java.time.LocalDateTime date) {
        if (ticket == null) {
            return;
        }
        ai.mnemosyne_systems.model.Message message = ai.mnemosyne_systems.model.Message
                .find("ticket = ?1 and body = ?2", ticket, body).firstResult();
        if (message != null) {
            message.date = date;
            if (message.author == null) {
                message.author = ticket.requester;
            }
            return;
        }
        message = new ai.mnemosyne_systems.model.Message();
        message.ticket = ticket;
        message.body = body;
        message.date = date;
        message.author = ticket.requester;
        message.persist();
    }

    private void seedReplyAt(Ticket ticket, String body, User author, java.time.LocalDateTime date) {
        if (ticket == null || author == null) {
            return;
        }
        Message message = Message.find("ticket = ?1 and body = ?2", ticket, body).firstResult();
        if (message != null) {
            message.date = date;
            message.author = author;
            return;
        }
        message = new Message();
        message.ticket = ticket;
        message.body = body;
        message.date = date;
        message.author = author;
        message.persist();
    }

    private void seedAttachment(Message message, String name, String mimeType, byte[] data) {
        if (message == null) {
            return;
        }
        Attachment existing = Attachment.find("message = ?1 and name = ?2", message, name).firstResult();
        if (existing != null) {
            return;
        }
        Attachment attachment = new Attachment();
        attachment.message = message;
        attachment.name = name;
        attachment.mimeType = mimeType;
        attachment.data = data;
        attachment.persist();
    }

    private void assignSupportIfMissing(Ticket ticket, String email) {
        if (ticket == null || email == null || email.isBlank()) {
            return;
        }
        User support = User.find("email", email).firstResult();
        if (support == null) {
            return;
        }
        boolean exists = ticket.supportUsers.stream()
                .anyMatch(existing -> existing.id != null && existing.id.equals(support.id));
        if (!exists) {
            ticket.supportUsers.add(support);
            ticket.status = "Assigned";
            ticket.persist();
        } else if (ticket.status.equals("Open")) {
            ticket.status = "Assigned";
            ticket.persist();
        }
    }

    private void assignTamIfMissing(Ticket ticket, String email) {
        if (ticket == null || email == null || email.isBlank()) {
            return;
        }
        User tam = User.find("email", email).firstResult();
        if (tam == null) {
            return;
        }
        boolean exists = ticket.tamUsers.stream()
                .anyMatch(existing -> existing.id != null && existing.id.equals(tam.id));
        if (!exists) {
            ticket.tamUsers.add(tam);
            ticket.persist();
        }
    }

    private CompanyEntitlement ensureCompanyEntitlement(Company company, String entitlementName, String levelName) {
        Entitlement entitlement = Entitlement.find("name", entitlementName).firstResult();
        Level level = Level.find("name", levelName).firstResult();
        if (entitlement == null || level == null) {
            return null;
        }
        CompanyEntitlement entry = CompanyEntitlement
                .find("company = ?1 and entitlement = ?2 and supportLevel = ?3", company, entitlement, level)
                .firstResult();
        if (entry != null) {
            if (entry.date == null) {
                entry.date = java.time.LocalDate.now();
            }
            if (entry.duration == null) {
                entry.duration = CompanyEntitlement.DURATION_YEARLY;
            }
            addEntitlementIfMissing(company, entry);
            return entry;
        }
        entry = new CompanyEntitlement();
        entry.company = company;
        entry.entitlement = entitlement;
        entry.supportLevel = level;
        entry.date = java.time.LocalDate.now();
        entry.duration = CompanyEntitlement.DURATION_YEARLY;
        entry.persist();
        addEntitlementIfMissing(company, entry);
        return entry;
    }

    private void addEntitlementIfMissing(Company company, CompanyEntitlement entry) {
        boolean exists = company.entitlements.stream()
                .anyMatch(existing -> existing.id != null && existing.id.equals(entry.id));
        if (!exists) {
            company.entitlements.add(entry);
        }
    }

    private void seedEntitlement(String name, String description) {
        Entitlement entitlement = Entitlement.find("name", name).firstResult();
        if (entitlement != null) {
            entitlement.description = description;
            ensureEntitlementVersions(entitlement);
            return;
        }
        entitlement = new Entitlement();
        entitlement.name = name;
        entitlement.description = description;
        entitlement.persist();
        ensureEntitlementVersions(entitlement);
    }

    private void ensureEntitlementVersions(Entitlement entitlement) {
        if (entitlement == null) {
            return;
        }
        ensureVersion(entitlement, "1.0.0");
        ensureVersion(entitlement, "1.0.1");
        ensureVersion(entitlement, "1.0.2");
        ensureVersion(entitlement, "1.0.3");
    }

    private void ensureVersion(Entitlement entitlement, String name) {
        Version version = Version.find("entitlement = ?1 and name = ?2", entitlement, name).firstResult();
        if (version != null) {
            if (version.date == null) {
                version.date = java.time.LocalDate.now();
            }
            return;
        }
        version = new Version();
        version.entitlement = entitlement;
        version.name = name;
        version.date = java.time.LocalDate.now();
        version.persist();
    }

    private void assignLevelsToEntitlement(String entitlementName, String... supportLevelNames) {
        Entitlement entitlement = Entitlement.find("name", entitlementName).firstResult();
        if (entitlement == null || supportLevelNames == null || supportLevelNames.length == 0) {
            return;
        }
        java.util.LinkedHashSet<Level> levels = new java.util.LinkedHashSet<>();
        for (String levelName : supportLevelNames) {
            if (levelName == null || levelName.isBlank()) {
                continue;
            }
            Level level = Level.find("name", levelName).firstResult();
            if (level != null) {
                levels.add(level);
            }
        }
        entitlement.supportLevels = new java.util.ArrayList<>(levels);
    }

    private void seedLevel(String name, String description, int levelValue, String color) {
        Level level = Level.find("name", name).firstResult();
        Country country = findCountryByCode("US");
        Timezone timezone = findTimezoneByName("America/New_York");
        if (level != null) {
            level.description = description;
            level.level = levelValue;
            level.color = color;
            if (level.fromDay == null) {
                level.fromDay = Level.DayOption.MONDAY.getCode();
            }
            if (level.fromTime == null) {
                level.fromTime = Level.HourOption.H00.getCode();
            }
            if (level.toDay == null) {
                level.toDay = Level.DayOption.SUNDAY.getCode();
            }
            if (level.toTime == null) {
                level.toTime = Level.HourOption.H23.getCode();
            }
            level.country = country;
            level.timezone = timezone;
            return;
        }
        level = new Level();
        level.name = name;
        level.description = description;
        level.level = levelValue;
        level.color = color;
        level.fromDay = Level.DayOption.MONDAY.getCode();
        level.fromTime = Level.HourOption.H00.getCode();
        level.toDay = Level.DayOption.SUNDAY.getCode();
        level.toTime = Level.HourOption.H23.getCode();
        level.country = country;
        level.timezone = timezone;
        level.persist();
    }

    private void seedCategory(String name, boolean isDefault) {
        Category category = Category.find("name", name).firstResult();
        if (category != null) {
            return;
        }
        category = new Category();
        category.name = name;
        category.isDefault = isDefault;
        category.persist();
    }

    private void removeUser(String email) {
        User user = User.find("email", email).firstResult();
        if (user == null) {
            return;
        }
        List<Company> companies = Company.find("select distinct c from Company c join c.users u where u = ?1", user)
                .list();
        for (Company company : companies) {
            company.users.removeIf(existing -> existing.id != null && existing.id.equals(user.id));
        }
        List<Ticket> supportTickets = Ticket
                .find("select distinct t from Ticket t join t.supportUsers u where u = ?1", user).list();
        for (Ticket ticket : supportTickets) {
            ticket.supportUsers.removeIf(existing -> existing.id != null && existing.id.equals(user.id));
        }
        List<Ticket> tamTickets = Ticket.find("select distinct t from Ticket t join t.tamUsers u where u = ?1", user)
                .list();
        for (Ticket ticket : tamTickets) {
            ticket.tamUsers.removeIf(existing -> existing.id != null && existing.id.equals(user.id));
        }
        List<Ticket> tickets = Ticket.find("select distinct t from Ticket t where t.requester = ?1", user).list();
        for (Ticket ticket : tickets) {
            ticket.requester = null;
        }
        List<Message> messages = Message.find("author = ?1", user).list();
        for (Message message : messages) {
            message.author = null;
        }
        user.delete();
    }
}
