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
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AttachmentHelper;
import ai.mnemosyne_systems.util.AuthHelper;
import ai.mnemosyne_systems.util.CurrentUser;
import io.quarkus.hibernate.orm.panache.Panache;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
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
import java.util.Locale;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/articles")
@Produces(MediaType.TEXT_HTML)
@Blocking
@RolesAllowed({ "admin", "support", "superuser", "tam", "user" })
public class ArticleResource {
    private static final String SAMPLE_TITLE = "Getting Started Guide";
    private static final String SAMPLE_TAGS = "guide, onboarding";
    private static final String SAMPLE_BODY = "## Welcome to billetsys\n\n- Open a ticket from the Tickets menu\n- Use Markdown in messages\n- Attach files with the attachment picker";
    private static final String LEGACY_SAMPLE_BODY_PREFIX = "# Getting Started";

    @Inject
    CurrentUser currentUser;

    @GET
    @Transactional
    public Response list() {
        currentUser.get();
        ensureSampleArticle();
        return Response.seeOther(URI.create("/articles")).build();
    }

    @GET
    @Path("/create")
    public Response createForm() {
        requireCreateEdit();
        return Response.seeOther(URI.create("/articles/new")).build();
    }

    @GET
    @Path("/{id}/edit")
    public Response editForm(@PathParam("id") Long id) {
        requireCreateEdit();
        if (findArticleWithAttachments(id) == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/articles/" + id + "/edit")).build();
    }

    @GET
    @Path("/{id}")
    public Response view(@PathParam("id") Long id) {
        requireView();
        ensureSampleArticle();
        if (findArticleWithAttachments(id) == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/articles/" + id)).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response create(@HeaderParam("X-Billetsys-Client") String client, MultipartFormDataInput input) {
        requireCreateEdit();
        String title = AttachmentHelper.readFormValue(input, "title");
        String tags = AttachmentHelper.readFormValue(input, "tags");
        String body = AttachmentHelper.readFormValue(input, "body");
        String normalizedTitle = validate(title, body, null);
        Article article = new Article();
        article.title = normalizedTitle;
        article.tags = tags == null ? null : tags.trim();
        article.body = body.trim();
        article.persist();
        List<Attachment> attachments = storeAttachments(article,
                AttachmentHelper.readAttachments(input, "attachments"));
        article.body = resolveInlineAttachmentUrls(article.body, attachments);
        return ReactRedirectSupport.redirect(client, "/articles");
    }

    @POST
    @Path("/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response update(@PathParam("id") Long id, @HeaderParam("X-Billetsys-Client") String client,
            MultipartFormDataInput input) {
        requireCreateEdit();
        Article article = Article
                .find("select distinct a from Article a left join fetch a.attachments where a.id = ?1", id)
                .firstResult();
        if (article == null) {
            throw new NotFoundException();
        }
        String title = AttachmentHelper.readFormValue(input, "title");
        String tags = AttachmentHelper.readFormValue(input, "tags");
        String body = AttachmentHelper.readFormValue(input, "body");
        String normalizedTitle = validate(title, body, article.id);
        article.title = normalizedTitle;
        article.tags = tags == null ? null : tags.trim();
        article.body = body.trim();
        List<Attachment> attachments = storeAttachments(article,
                AttachmentHelper.readAttachments(input, "attachments"));
        article.body = resolveInlineAttachmentUrls(article.body, attachments);
        return ReactRedirectSupport.redirect(client, "/articles/" + id);
    }

    @POST
    @Path("/{id}/delete")
    @Transactional
    @RolesAllowed("admin")
    public Response delete(@HeaderParam("X-Billetsys-Client") String client, @PathParam("id") Long id) {
        Article article = Article.findById(id);
        if (article == null) {
            throw new NotFoundException();
        }
        article.delete();
        return ReactRedirectSupport.redirect(client, "/articles");
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

    private String validate(String title, String body, Long articleId) {
        if (title == null || title.isBlank()) {
            throw badRequest("Title is required");
        }
        String normalizedTitle = title.trim();
        if (titleExists(normalizedTitle, articleId)) {
            throw badRequest("Article title already exists");
        }
        if (body == null || body.isBlank()) {
            throw badRequest("Body is required");
        }
        return normalizedTitle;
    }

    private boolean titleExists(String title, Long articleId) {
        String normalizedTitle = title.toLowerCase(Locale.ROOT);
        long count = articleId == null ? Article.count("lower(title) = ?1", normalizedTitle)
                : Article.count("lower(title) = ?1 and id <> ?2", normalizedTitle, articleId);
        return count > 0;
    }

    private BadRequestException badRequest(String message) {
        return new BadRequestException(
                Response.status(Response.Status.BAD_REQUEST).entity(message).type(MediaType.TEXT_PLAIN_TYPE).build());
    }

    static void ensureSampleArticle() {
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

    static Article findArticleWithAttachments(Long id) {
        return Article.find("select distinct a from Article a left join fetch a.attachments where a.id = ?1", id)
                .firstResult();
    }

    private User requireLoggedIn() {
        User user = currentUser.get();
        if (user == null) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }

    private User requireView() {
        return requireLoggedIn();
    }

    private User requireCreateEdit() {
        User user = currentUser.get();
        if (!canEdit(user)) {
            // TODO return ForbiddenException() ?!
            throw new WebApplicationException(Response.seeOther(URI.create("/articles")).build());
        }
        return user;
    }

    private User requireAdmin() {
        User user = requireLoggedIn();
        if (!AuthHelper.isAdmin(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/articles")).build());
        }
        return user;
    }

    static boolean canEdit(User user) {
        return AuthHelper.isAdmin(user) || AuthHelper.isSupport(user)
                || (user != null && User.TYPE_TAM.equalsIgnoreCase(user.type));
    }

}
