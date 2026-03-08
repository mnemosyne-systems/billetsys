/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Article;
import ai.mnemosyne_systems.model.Attachment;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AttachmentHelper;
import ai.mnemosyne_systems.util.AuthHelper;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/articles")
@Produces(MediaType.TEXT_HTML)
@Blocking
public class ArticleResource {
    private static final String SAMPLE_TITLE = "Getting Started Guide";
    private static final String SAMPLE_TAGS = "guide, onboarding";
    private static final String SAMPLE_BODY = "## Welcome to billetsys\n\n- Open a ticket from the Tickets menu\n- Use Markdown in messages\n- Attach files with the attachment picker";
    private static final String LEGACY_SAMPLE_BODY_PREFIX = "# Getting Started";

    @Location("article/articles.html")
    Template articlesTemplate;

    @Location("article/articles-admin.html")
    Template adminArticlesTemplate;

    @Location("article/article-form.html")
    Template articleFormTemplate;

    @Location("article/article-view.html")
    Template articleViewTemplate;

    @Location("article/article-view-admin.html")
    Template articleAdminViewTemplate;

    @GET
    @Transactional
    public TemplateInstance list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireLoggedIn(auth);
        ensureSampleArticle();
        List<Article> articles = Article.list("order by id desc");
        if (AuthHelper.isAdmin(user)) {
            return adminArticlesTemplate.data("articles", articles).data("currentUser", user);
        }
        TicketCounts counts = loadCountsFor(user);
        return articlesTemplate.data("articles", articles).data("currentUser", user).data("canCreate", canEdit(user))
                .data("canEdit", canEdit(user)).data("assignedCount", counts.assignedCount)
                .data("openCount", counts.openCount).data("ticketsBase", counts.ticketsBase)
                .data("usersBase", counts.usersBase).data("showSupportUsers", counts.showSupportUsers);
    }

    @GET
    @Path("/create")
    public TemplateInstance createForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireCreateEdit(auth);
        TicketCounts counts = loadCountsFor(user);
        Article article = new Article();
        article.body = "";
        return articleFormTemplate.data("article", article).data("action", "/articles").data("title", "New Article")
                .data("submitLabel", "Create").data("existingAttachments", List.of()).data("currentUser", user)
                .data("assignedCount", counts.assignedCount).data("openCount", counts.openCount)
                .data("ticketsBase", counts.ticketsBase).data("usersBase", counts.usersBase)
                .data("showSupportUsers", counts.showSupportUsers);
    }

    @GET
    @Path("/{id}/edit")
    public TemplateInstance editForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireCreateEdit(auth);
        Article article = Article
                .find("select distinct a from Article a left join fetch a.attachments where a.id = ?1", id)
                .firstResult();
        if (article == null) {
            throw new NotFoundException();
        }
        TicketCounts counts = loadCountsFor(user);
        return articleFormTemplate.data("article", article).data("action", "/articles/" + id)
                .data("title", "Edit Article").data("submitLabel", "Save")
                .data("existingAttachments", article.attachments).data("currentUser", user)
                .data("assignedCount", counts.assignedCount).data("openCount", counts.openCount)
                .data("ticketsBase", counts.ticketsBase).data("usersBase", counts.usersBase)
                .data("showSupportUsers", counts.showSupportUsers);
    }

    @GET
    @Path("/{id}")
    public TemplateInstance view(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireView(auth);
        ensureSampleArticle();
        Article article = Article
                .find("select distinct a from Article a left join fetch a.attachments where a.id = ?1", id)
                .firstResult();
        if (article == null) {
            throw new NotFoundException();
        }
        TicketCounts counts = loadCountsFor(user);
        if (AuthHelper.isAdmin(user)) {
            return articleAdminViewTemplate.data("article", article).data("currentUser", user);
        }
        return articleViewTemplate.data("article", article).data("currentUser", user).data("canEdit", canEdit(user))
                .data("assignedCount", counts.assignedCount).data("openCount", counts.openCount)
                .data("ticketsBase", counts.ticketsBase).data("usersBase", counts.usersBase)
                .data("showSupportUsers", counts.showSupportUsers);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response create(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, MultipartFormDataInput input) {
        requireCreateEdit(auth);
        String title = AttachmentHelper.readFormValue(input, "title");
        String tags = AttachmentHelper.readFormValue(input, "tags");
        String body = AttachmentHelper.readFormValue(input, "body");
        validate(title, body);
        Article article = new Article();
        article.title = title.trim();
        article.tags = tags == null ? null : tags.trim();
        article.body = body.trim();
        article.persist();
        List<Attachment> attachments = storeAttachments(article,
                AttachmentHelper.readAttachments(input, "attachments"));
        article.body = resolveInlineAttachmentUrls(article.body, attachments);
        return Response.seeOther(URI.create("/articles")).build();
    }

    @POST
    @Path("/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response update(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            MultipartFormDataInput input) {
        requireCreateEdit(auth);
        Article article = Article
                .find("select distinct a from Article a left join fetch a.attachments where a.id = ?1", id)
                .firstResult();
        if (article == null) {
            throw new NotFoundException();
        }
        String title = AttachmentHelper.readFormValue(input, "title");
        String tags = AttachmentHelper.readFormValue(input, "tags");
        String body = AttachmentHelper.readFormValue(input, "body");
        validate(title, body);
        article.title = title.trim();
        article.tags = tags == null ? null : tags.trim();
        article.body = body.trim();
        List<Attachment> attachments = storeAttachments(article,
                AttachmentHelper.readAttachments(input, "attachments"));
        article.body = resolveInlineAttachmentUrls(article.body, attachments);
        return Response.seeOther(URI.create("/articles/" + id)).build();
    }

    @POST
    @Path("/{id}/delete")
    @Transactional
    public Response delete(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireAdmin(auth);
        Article article = Article.findById(id);
        if (article == null) {
            throw new NotFoundException();
        }
        article.delete();
        return Response.seeOther(URI.create("/articles")).build();
    }

    private List<Attachment> storeAttachments(Article article, List<Attachment> uploaded) {
        for (Attachment upload : uploaded) {
            upload.message = null;
            upload.article = article;
            upload.persist();
            article.attachments.add(upload);
        }
        Panache.getEntityManager().flush();
        return uploaded;
    }

    private String resolveInlineAttachmentUrls(String body, List<Attachment> attachments) {
        if (body == null || body.isBlank() || attachments == null || attachments.isEmpty()) {
            return body;
        }
        String updated = body;
        for (Attachment attachment : attachments) {
            if (attachment == null || attachment.id == null || attachment.name == null || attachment.name.isBlank()) {
                continue;
            }
            String encodedName = URLEncoder.encode(attachment.name, StandardCharsets.UTF_8).replace("+", "%20");
            String url = "/attachments/" + attachment.id + "/data";
            updated = updated.replace("attachment://" + encodedName, url);
            updated = updated.replace("attachment://" + attachment.name, url);
        }
        return updated;
    }

    private void validate(String title, String body) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Title is required");
        }
        if (body == null || body.isBlank()) {
            throw new BadRequestException("Body is required");
        }
    }

    private void ensureSampleArticle() {
        if (Article.count() > 0) {
            Article sample = Article.find("title", SAMPLE_TITLE).firstResult();
            if (sample != null && sample.body != null && sample.body.startsWith(LEGACY_SAMPLE_BODY_PREFIX)) {
                sample.tags = SAMPLE_TAGS;
                sample.body = SAMPLE_BODY;
            }
            return;
        }
        Article article = new Article();
        article.title = SAMPLE_TITLE;
        article.tags = SAMPLE_TAGS;
        article.body = SAMPLE_BODY;
        article.persist();
    }

    private User requireLoggedIn(String auth) {
        User user = AuthHelper.findUser(auth);
        if (user == null) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }

    private User requireView(String auth) {
        return requireLoggedIn(auth);
    }

    private User requireCreateEdit(String auth) {
        User user = requireLoggedIn(auth);
        if (!canEdit(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/articles")).build());
        }
        return user;
    }

    private User requireAdmin(String auth) {
        User user = requireLoggedIn(auth);
        if (!AuthHelper.isAdmin(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/articles")).build());
        }
        return user;
    }

    private boolean canEdit(User user) {
        return AuthHelper.isSupport(user) || (user != null && User.TYPE_TAM.equalsIgnoreCase(user.type));
    }

    private TicketCounts loadCountsFor(User user) {
        if (user == null || AuthHelper.isAdmin(user)) {
            return new TicketCounts(0, 0, "/user/tickets", "/user/users", false);
        }
        if (AuthHelper.isSupport(user)) {
            SupportResource.SupportTicketCounts counts = SupportResource.loadTicketCounts(user);
            return new TicketCounts(counts.assignedCount, counts.openCount, "/support", "/support/users", true);
        }
        if (User.TYPE_SUPERUSER.equalsIgnoreCase(user.type)) {
            SupportResource.SupportTicketCounts counts = SuperuserResource.loadTicketCounts(user);
            return new TicketCounts(counts.assignedCount, counts.openCount, "/superuser/tickets", "/superuser/users",
                    true);
        }
        if (User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            List<Ticket> tickets = Ticket.find(
                    "select distinct t from Ticket t left join t.tamUsers tu left join t.company c left join c.users cu where tu = ?1 or cu = ?1",
                    user).list();
            return buildTicketCounts(tickets, "/user/tickets", "/tam/users", true);
        }
        List<Ticket> tickets = Ticket.list("requester = ?1", user);
        return buildTicketCounts(tickets, "/user/tickets", "/user/users", false);
    }

    private TicketCounts buildTicketCounts(List<Ticket> tickets, String ticketsBase, String usersBase,
            boolean showSupportUsers) {
        int assigned = 0;
        int open = 0;
        List<Ticket> scopedTickets = tickets == null ? List.of() : tickets;
        for (Ticket ticket : scopedTickets) {
            if ("Closed".equalsIgnoreCase(ticket.status)) {
                continue;
            }
            Long hasSupport = Ticket.count("select count(u) from Ticket t join t.supportUsers u where t = ?1", ticket);
            if (hasSupport != null && hasSupport > 0) {
                assigned++;
            } else {
                open++;
            }
        }
        return new TicketCounts(assigned, open, ticketsBase, usersBase, showSupportUsers);
    }

    private static class TicketCounts {
        final int assignedCount;
        final int openCount;
        final String ticketsBase;
        final String usersBase;
        final boolean showSupportUsers;

        TicketCounts(int assignedCount, int openCount, String ticketsBase, String usersBase, boolean showSupportUsers) {
            this.assignedCount = assignedCount;
            this.openCount = openCount;
            this.ticketsBase = ticketsBase;
            this.usersBase = usersBase;
            this.showSupportUsers = showSupportUsers;
        }
    }
}
