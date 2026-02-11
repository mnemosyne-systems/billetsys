/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.Attachment;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Entitlement;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.SupportLevel;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.Message;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;

@ApplicationScoped
public class UserSeeder {

    void onStart(@Observes StartupEvent event) {
        seedCountriesAndTimezones();
        seedDefaults();
        seedSupportCatalog();
        seedSampleData();
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
        seedUser("tam", "Technical Account Manager", "tam@mnemosyne-systems.ai", "+1-555-0300", "300",
                "America/Chicago", "US", User.TYPE_TAM, "tam");
        removeUser("user@mnemosyne-systems.ai");
        removeUser("support@mnemosyne-systems.ai");
    }

    @Transactional
    void seedSampleData() {
        User user1 = seedUser("user1", "John Doe", "user1@mnemosyne-systems.ai", "+1-555-0201", null, "Europe/London",
                "GB", User.TYPE_USER, "user1");
        User user2 = seedUser("user2", "Jane Smith", "user2@mnemosyne-systems.ai", "+1-555-0202", null, "Europe/Paris",
                "FR", User.TYPE_USER, "user2");
        User tam = User.find("email", "tam@mnemosyne-systems.ai").firstResult();
        if (tam == null) {
            tam = seedUser("tam", "Technical Account Manager", "tam@mnemosyne-systems.ai", "+1-555-0300", "300",
                    "America/Chicago", "US", User.TYPE_TAM, "tam");
        }

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
        addUserIfMissing(company, user1);
        addUserIfMissing(company, user2);
        addUserIfMissing(company, tam);

        CompanyEntitlement enterpriseHigh = ensureCompanyEntitlement(company, "Enterprise", "High");
        Ticket a1 = seedTicket(Ticket.formatName(company, 1), company, user1, enterpriseHigh);
        Ticket a2 = seedTicket(Ticket.formatName(company, 2), company, user2, enterpriseHigh);
        Ticket a3 = seedTicket(Ticket.formatName(company, 3), company, user1, enterpriseHigh);
        Ticket a4 = seedTicket(Ticket.formatName(company, 4), company, user2, enterpriseHigh);
        company.ticketSequence = 4L;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        seedMessageAt(a1, "Sample ticket created.", now.minusMinutes(150));
        seedMessageAt(a2, "Sample ticket created.", now.minusMinutes(30));
        seedMessageAt(a3, "Sample ticket created.", now.minusMinutes(80));
        seedMessageAt(a4, "Sample ticket created.", now.minusMinutes(100));
        Message attachmentMessage = Message.find("ticket = ?1 and body = ?2", a1, "Sample ticket created.")
                .firstResult();
        seedAttachment(attachmentMessage, "sample.txt", "text/plain",
                "Sample attachment\nLine two".getBytes(StandardCharsets.UTF_8));
        seedMessageAt(a1, "Sample attachments added.", now.minusMinutes(120));
        Message attachmentMessageTwo = Message.find("ticket = ?1 and body = ?2", a1, "Sample attachments added.")
                .firstResult();
        seedAttachment(attachmentMessageTwo, "sample-one.txt", "text/plain",
                "First attachment".getBytes(StandardCharsets.UTF_8));
        seedAttachment(attachmentMessageTwo, "sample-two.txt", "text/plain",
                "Second attachment".getBytes(StandardCharsets.UTF_8));
        assignSupportIfMissing(a1, "support1@mnemosyne-systems.ai");
        assignSupportIfMissing(a4, "support1@mnemosyne-systems.ai");
        if (a4 != null) {
            a4.status = "Closed";
            a4.persist();
        }
    }

    @Transactional
    void seedSupportCatalog() {
        seedEntitlement("Starter", "Email support with 2 business day response", 99);
        seedEntitlement("Business", "Priority support with 1 business day response", 249);
        seedEntitlement("Enterprise", "24/7 support with SLA and dedicated TAM", 499);

        seedSupportLevel("Low", "Standard response window", 60, "Red", 720, "Yellow", 1440, "White");
        seedSupportLevel("Normal", "Default response window", 60, "Red", 120, "Yellow", 720, "White");
        seedSupportLevel("High", "Escalated response window", 60, "Red", 90, "Yellow", 120, "White");
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
            user.name = username;
            user.fullName = fullName;
            user.email = email;
            user.phoneNumber = phoneNumber;
            user.phoneExtension = phoneExtension;
            user.timezone = timezone;
            user.country = country;
            user.type = type;
            user.passwordHash = BcryptUtil.bcryptHash(password);
            user.persist();
            return user;
        }
        if (user.name == null || user.name.isBlank()) {
            user.name = username;
        }
        if (user.fullName == null || user.fullName.isBlank()) {
            user.fullName = fullName;
        }
        if (user.phoneNumber == null || user.phoneNumber.isBlank()) {
            user.phoneNumber = phoneNumber;
        }
        if (user.phoneExtension == null || user.phoneExtension.isBlank()) {
            user.phoneExtension = phoneExtension;
        }
        if (user.timezone == null) {
            user.timezone = timezone;
        }
        if (user.country == null) {
            user.country = country;
        }
        if (user.type == null || user.type.isBlank()) {
            user.type = type;
        }
        if (user.passwordHash == null || user.passwordHash.isBlank()) {
            user.passwordHash = BcryptUtil.bcryptHash(password);
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

    private Ticket seedTicket(String name, Company company, User requester, CompanyEntitlement entitlement) {
        Ticket ticket = Ticket.find("name", name).firstResult();
        if (ticket == null) {
            ticket = new Ticket();
            ticket.name = name;
            ticket.status = "Open";
            ticket.company = company;
            ticket.requester = requester;
            ticket.companyEntitlement = entitlement;
            ticket.persist();
        }
        if (ticket.status == null || ticket.status.isBlank()) {
            ticket.status = "Open";
        }
        ticket.company = company;
        ticket.requester = requester;
        ticket.companyEntitlement = entitlement;
        ticket.persist();
        seedMessage(ticket, "Sample ticket created.");
        return ticket;
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
            ticket.persist();
        }
    }

    private CompanyEntitlement ensureCompanyEntitlement(Company company, String entitlementName, String levelName) {
        Entitlement entitlement = Entitlement.find("name", entitlementName).firstResult();
        SupportLevel level = SupportLevel.find("name", levelName).firstResult();
        if (entitlement == null || level == null) {
            return null;
        }
        CompanyEntitlement entry = CompanyEntitlement.find("company = ?1 and entitlement = ?2", company, entitlement)
                .firstResult();
        if (entry != null) {
            entry.supportLevel = level;
            addEntitlementIfMissing(company, entry);
            return entry;
        }
        entry = new CompanyEntitlement();
        entry.company = company;
        entry.entitlement = entitlement;
        entry.supportLevel = level;
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

    private void seedEntitlement(String name, String description, int price) {
        Entitlement entitlement = Entitlement.find("name", name).firstResult();
        if (entitlement != null) {
            return;
        }
        entitlement = new Entitlement();
        entitlement.name = name;
        entitlement.description = description;
        entitlement.price = price;
        entitlement.persist();
    }

    private void seedSupportLevel(String name, String description, int critical, String criticalColor, int escalate,
            String escalateColor, int normal, String normalColor) {
        SupportLevel level = SupportLevel.find("name", name).firstResult();
        if (level != null) {
            return;
        }
        level = new SupportLevel();
        level.name = name;
        level.description = description;
        level.critical = critical;
        level.criticalColor = criticalColor;
        level.escalate = escalate;
        level.escalateColor = escalateColor;
        level.normal = normal;
        level.normalColor = normalColor;
        level.persist();
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
