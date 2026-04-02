/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.Article;
import ai.mnemosyne_systems.model.Attachment;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Entitlement;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Level;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.Version;
import ai.mnemosyne_systems.service.MailboxPollingService;
import ai.mnemosyne_systems.service.TicketEmailService;
import ai.mnemosyne_systems.util.AuthHelper;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

abstract class AccessTestSupport {

    @Inject
    MockMailbox mailbox;

    @Inject
    MailboxPollingService mailboxPollingService;

    @Inject
    TicketEmailService ticketEmailService;

    @Transactional
    void ensureUser(String name, String email, String type, String password) {
        User user = User.find("email", email).firstResult();
        if (user == null) {
            user = new User();
            user.name = name;
            user.email = email;
            user.type = type;
            user.passwordHash = BcryptUtil.bcryptHash(password);
            user.persist();
            return;
        }
        user.name = name;
        user.type = type;
        user.passwordHash = BcryptUtil.bcryptHash(password);
    }

    @Transactional
    void ensureCompanyIfMissing(String name) {
        if (ai.mnemosyne_systems.model.Company.find("name", name).firstResult() != null) {
            return;
        }
        ai.mnemosyne_systems.model.Company company = new ai.mnemosyne_systems.model.Company();
        company.name = name;
        company.persist();
    }

    @Transactional
    Long ensureCompany(String name) {
        ai.mnemosyne_systems.model.Company company = ai.mnemosyne_systems.model.Company.find("name", name)
                .firstResult();
        if (company != null) {
            return company.id;
        }
        company = new ai.mnemosyne_systems.model.Company();
        company.name = name;
        company.country = ai.mnemosyne_systems.model.Country.find("code", "US").firstResult();
        company.persist();
        return company.id;
    }

    @Transactional
    ai.mnemosyne_systems.model.Ticket ensureTicket(Long companyId) {
        ai.mnemosyne_systems.model.Company company = ai.mnemosyne_systems.model.Company.findById(companyId);
        ai.mnemosyne_systems.model.User user = ai.mnemosyne_systems.model.User
                .find("email", "user@mnemosyne-systems.ai").firstResult();
        ai.mnemosyne_systems.model.Entitlement entitlement = ai.mnemosyne_systems.model.Entitlement
                .find("name", "Starter").firstResult();
        ai.mnemosyne_systems.model.Level level = ai.mnemosyne_systems.model.Level.find("name", "Normal").firstResult();
        ai.mnemosyne_systems.model.CompanyEntitlement entry = new ai.mnemosyne_systems.model.CompanyEntitlement();
        entry.company = company;
        entry.entitlement = entitlement;
        entry.supportLevel = level;
        entry.persist();
        ai.mnemosyne_systems.model.Ticket ticket = new ai.mnemosyne_systems.model.Ticket();
        ticket.name = ai.mnemosyne_systems.model.Ticket.nextName(company);
        ticket.title = "Support request " + ticket.name;
        ticket.status = "Assigned";
        ticket.company = company;
        ticket.requester = user;
        ticket.companyEntitlement = entry;
        ticket.supportUsers
                .add(ai.mnemosyne_systems.model.User.find("email", "support1@mnemosyne-systems.ai").firstResult());
        ticket.tamUsers.add(ai.mnemosyne_systems.model.User.find("email", "tam1@mnemosyne-systems.ai").firstResult());
        ticket.persist();
        return ticket;
    }

    @Transactional
    ai.mnemosyne_systems.model.Ticket ensureUnassignedOpenTicket(Long companyId) {
        ai.mnemosyne_systems.model.Ticket ticket = ensureTicket(companyId);
        ticket.supportUsers.clear();
        ticket.status = "Open";
        ticket.persist();
        return ticket;
    }

    @Transactional
    void ensureMessage(ai.mnemosyne_systems.model.Ticket ticket, String body) {
        if (ai.mnemosyne_systems.model.Message.find("ticket = ?1", ticket).firstResult() != null) {
            return;
        }
        ai.mnemosyne_systems.model.Message message = new ai.mnemosyne_systems.model.Message();
        message.ticket = ticket;
        message.body = body;
        message.date = java.time.LocalDateTime.now();
        message.author = ai.mnemosyne_systems.model.User.find("email", "support1@mnemosyne-systems.ai").firstResult();
        message.persist();
    }

    @Transactional
    Message ensureMessageWithBody(Ticket ticket, String body) {
        Message message = Message.find("ticket = ?1 and body = ?2", ticket, body).firstResult();
        if (message != null) {
            return message;
        }
        message = new Message();
        message.ticket = ticket;
        message.body = body;
        message.date = java.time.LocalDateTime.now();
        message.author = User.find("email", "support1@mnemosyne-systems.ai").firstResult();
        message.persist();
        return message;
    }

    @Transactional
    Message ensureTimedMessage(Ticket ticket, String body, String authorEmail, java.time.LocalDateTime date) {
        Ticket managedTicket = Ticket.findById(ticket.id);
        Message message = Message.find("ticket = ?1 and body = ?2", managedTicket, body).firstResult();
        if (message == null) {
            message = new Message();
            message.ticket = managedTicket;
            message.body = body;
        }
        message.author = User.find("email", authorEmail).firstResult();
        message.date = date;
        if (message.id == null) {
            message.persist();
        }
        return message;
    }

    @Transactional
    void setTicketSupportLevel(Long ticketId, Long levelId) {
        Ticket ticket = Ticket.findById(ticketId);
        Level level = Level.findById(levelId);
        ticket.companyEntitlement.supportLevel = level;
    }

    @Transactional
    Attachment ensureAttachment(Message message, String name) {
        Attachment attachment = Attachment.find("message = ?1 and name = ?2", message, name).firstResult();
        if (attachment != null) {
            return attachment;
        }
        attachment = new Attachment();
        attachment.message = message;
        attachment.name = name;
        attachment.mimeType = "text/plain";
        attachment.data = "Attachment data".getBytes(StandardCharsets.UTF_8);
        attachment.persist();
        return attachment;
    }

    @Transactional
    void ensureCompanyUsers(Long companyId, String... emails) {
        ai.mnemosyne_systems.model.Company company = ai.mnemosyne_systems.model.Company.findById(companyId);
        if (company == null) {
            return;
        }
        for (String email : emails) {
            ai.mnemosyne_systems.model.User user = ai.mnemosyne_systems.model.User.find("email", email).firstResult();
            if (user == null) {
                continue;
            }
            boolean exists = company.users.stream()
                    .anyMatch(existing -> existing.id != null && existing.id.equals(user.id));
            if (!exists) {
                company.users.add(user);
            }
        }
    }

    @Transactional
    void ensureDefaultCategories() {
        if (Category.count() > 0) {
            return;
        }
        Category feature = new Category();
        feature.name = "Feature";
        feature.description = "Feature requests and improvements";
        feature.isDefault = false;
        feature.persist();
        Category bug = new Category();
        bug.name = "Bug";
        bug.description = "Defects and unexpected behavior reports";
        bug.isDefault = false;
        bug.persist();
        Category question = new Category();
        question.name = "Question";
        question.description = "General product and usage questions";
        question.isDefault = true;
        question.persist();
    }

    String login(String username, String password) {
        return RestAssured.given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("username", username).formParam("password", password).post("/login").then().statusCode(303)
                .extract().cookie(AuthHelper.AUTH_COOKIE);
    }

    @Transactional
    Entitlement ensureEntitlement(String name, String description) {
        Entitlement entitlement = Entitlement.find("name", name).firstResult();
        if (entitlement == null) {
            entitlement = new Entitlement();
            entitlement.name = name;
            entitlement.description = description;
            entitlement.persist();
        }
        ensureVersion(entitlement, "1.0.0");
        ensureVersion(entitlement, "1.0.1");
        return entitlement;
    }

    @Transactional
    Version ensureVersion(Entitlement entitlement, String name) {
        Version version = Version.find("entitlement = ?1 and name = ?2", entitlement, name).firstResult();
        if (version == null) {
            version = new Version();
            version.entitlement = entitlement;
            version.name = name;
            version.date = java.time.LocalDate.now();
            version.persist();
        }
        return version;
    }

    @Transactional
    Version ensureVersion(Entitlement entitlement, String name, java.time.LocalDate date) {
        Version version = ensureVersion(entitlement, name);
        version.date = date;
        return version;
    }

    int countOccurrences(String text, String snippet) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(snippet, index)) >= 0) {
            count++;
            index += snippet.length();
        }
        return count;
    }

    @Transactional
    Level ensureLevel(String name, String description, int levelValue, String color) {
        Level level = Level.find("name", name).firstResult();
        if (level == null) {
            level = new Level();
            level.name = name;
            level.persist();
        }
        level.description = description;
        level.level = levelValue;
        level.color = color;
        return level;
    }

    @Transactional
    CompanyEntitlement ensureCompanyEntitlement(Company company, Entitlement entitlement, Level level) {
        CompanyEntitlement entry = CompanyEntitlement
                .find("company = ?1 and entitlement = ?2 and supportLevel = ?3", company, entitlement, level)
                .firstResult();
        if (entry == null) {
            entry = new CompanyEntitlement();
            entry.company = company;
            entry.entitlement = entitlement;
            entry.supportLevel = level;
            entry.persist();
        }
        return entry;
    }

    @Transactional
    CompanyEntitlement ensureCompanyEntitlement(Long companyId, Entitlement entitlement) {
        Company company = Company.findById(companyId);
        Level level = Level.find("name", "Normal").firstResult();
        return ensureCompanyEntitlement(company, entitlement, level);
    }

    @Transactional
    Article ensureArticle(String title, String tags, String body) {
        Article article = Article.find("title", title).firstResult();
        if (article == null) {
            article = new Article();
            article.title = title;
            article.tags = tags;
            article.body = body;
            article.persist();
            return article;
        }
        article.tags = tags;
        article.body = body;
        return article;
    }

    @Transactional
    Category ensureCategory(String name, String description, boolean isDefault) {
        Category category = Category.find("name", name).firstResult();
        if (category == null) {
            category = new Category();
            category.name = name;
            category.persist();
        }
        category.description = description;
        category.isDefault = isDefault;
        return category;
    }

    @Transactional
    Attachment ensureArticleAttachment(Article article, String name, String body) {
        Attachment attachment = Attachment.find("article = ?1 and name = ?2", article, name).firstResult();
        if (attachment == null) {
            attachment = new Attachment();
            attachment.article = article;
            attachment.name = name;
        }
        attachment.mimeType = "text/plain";
        attachment.data = body.getBytes(StandardCharsets.UTF_8);
        if (attachment.id == null) {
            attachment.persist();
            article.attachments.add(attachment);
        }
        return attachment;
    }

    @Transactional
    Attachment ensureCategoryAttachment(Category category, String name, String body) {
        Attachment attachment = Attachment.find("category = ?1 and name = ?2", category, name).firstResult();
        if (attachment == null) {
            attachment = new Attachment();
            attachment.category = category;
            attachment.name = name;
        }
        attachment.mimeType = "text/plain";
        attachment.data = body.getBytes(StandardCharsets.UTF_8);
        if (attachment.id == null) {
            attachment.persist();
            category.attachments.add(attachment);
        }
        return attachment;
    }

    @Transactional
    Company ownerCompany() {
        Panache.getEntityManager().clear();
        return OwnerResource.findOwnerCompany();
    }

    @Transactional
    boolean companyHasUser(Long companyId, String email) {
        Long count = Company.count("select distinct c from Company c join c.users u where c.id = ?1 and u.email = ?2",
                companyId, email);
        return count != null && count > 0;
    }

    @Transactional
    Entitlement refreshedEntitlement(Long id) {
        Panache.getEntityManager().clear();
        return Entitlement.findById(id);
    }

    @Transactional
    Ticket refreshedTicket(Long id) {
        Panache.getEntityManager().clear();
        return Ticket.findById(id);
    }

    @Transactional
    boolean ticketHasSupportUser(Long ticketId, Long userId) {
        Long count = Ticket.count("select distinct t from Ticket t join t.supportUsers u where t.id = ?1 and u.id = ?2",
                ticketId, userId);
        return count != null && count > 0;
    }

    @Transactional
    boolean ticketHasTamUser(Long ticketId, Long userId) {
        Long count = Ticket.count("select distinct t from Ticket t join t.tamUsers u where t.id = ?1 and u.id = ?2",
                ticketId, userId);
        return count != null && count > 0;
    }

    @Transactional
    Level refreshedLevel(Long id) {
        Panache.getEntityManager().clear();
        return Level.findById(id);
    }

    @Transactional
    Company refreshedCompany(Long id) {
        Panache.getEntityManager().clear();
        return Company.findById(id);
    }

    @Transactional
    User refreshedUser(Long id) {
        Panache.getEntityManager().clear();
        return User.findById(id);
    }

    @Transactional
    Message refreshedMessage(Long id) {
        Panache.getEntityManager().clear();
        return Message.findById(id);
    }

    @Transactional
    Article refreshedArticle(Long id) {
        Panache.getEntityManager().clear();
        return Article.findById(id);
    }

    @Transactional
    Message findMessageByBody(String body) {
        Panache.getEntityManager().clear();
        return Message.find("body", body).firstResult();
    }

    Mail latestMailTo(String recipient) {
        List<Mail> mails = mailbox.getMailsSentTo(recipient);
        Assertions.assertFalse(mails.isEmpty());
        return mails.get(mails.size() - 1);
    }

    void assertTicketCreateMail(Mail mail, Ticket ticket, String actorName, String status, String body) {
        Assertions.assertNotNull(mail);
        Assertions.assertNotNull(ticket);
        Assertions.assertTrue(mail.getSubject().contains("[" + ticket.name + "]"));
        Assertions.assertTrue(mail.getText().contains("Ticket: " + ticket.name));
        Assertions.assertTrue(mail.getText().contains("Type: Message"));
        Assertions.assertTrue(mail.getText().contains("Actor: " + actorName));
        Assertions.assertTrue(mail.getText().contains("Status: " + status));
        Assertions.assertTrue(mail.getText().contains("Previous status:"));
        Assertions.assertTrue(mail.getText().contains(body));
        Assertions.assertTrue(mail.getHtml().contains("<strong>Ticket:</strong> " + ticket.name));
        Assertions.assertTrue(mail.getHtml().contains("<strong>Actor:</strong> " + actorName));
        Assertions.assertTrue(mail.getHtml().contains("<strong>Status:</strong> " + status));
        Assertions.assertTrue(mail.getHtml().contains(body));
    }

    @Transactional
    Long createCompany(String cookie, String name) {
        String normalized = name == null ? "company"
                : name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            normalized = "company";
        }
        String superuserUsername = normalized + "-superuser";
        String superuserEmail = normalized + "@testing.com";
        String location = RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .contentType(ContentType.URLENC).formParam("name", name)
                .formParam("superuserUsername", superuserUsername).formParam("superuserEmail", superuserEmail)
                .formParam("superuserPassword", "pass").post("/companies").then().statusCode(303).extract()
                .header("Location");
        ai.mnemosyne_systems.model.Company company = ai.mnemosyne_systems.model.Company.find("name", name)
                .firstResult();
        return company == null ? null : company.id;
    }

    @Transactional
    void deleteCompany(String cookie, Long companyId) {
        RestAssured.given().redirects().follow(false).cookie(AuthHelper.AUTH_COOKIE, cookie)
                .post("/companies/" + companyId + "/delete").then().statusCode(303);
    }
}
