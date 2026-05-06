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
import ai.mnemosyne_systems.util.AuthHelper;
import io.smallrye.common.annotation.Blocking;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Path("/api/articles")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
public class ArticleApiResource {

    @GET
    @Transactional
    public ArticleListResponse list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireUser(auth);
        ArticleResource.ensureSampleArticle();
        List<ArticleSummary> items = Article.<Article> list("order by lower(title), id").stream()
                .map(article -> new ArticleSummary(article.id, article.title, article.tags)).toList();
        return new ArticleListResponse(ArticleResource.canEdit(user), "/articles/new", items);
    }

    @GET
    @Path("/bootstrap")
    public ArticleBootstrapResponse bootstrap(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireUser(auth);
        return new ArticleBootstrapResponse(ArticleResource.canEdit(user), AuthHelper.isAdmin(user));
    }

    @GET
    @Path("/suggest")
    @Transactional
    public ArticleSuggestionResponse suggest(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @QueryParam("q") @DefaultValue("") String q) {
        requireUser(auth);
        ArticleResource.ensureSampleArticle();
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<Article> all = Article.<Article> list("order by id desc");
        List<ArticleSuggestion> matches = new ArrayList<>();
        for (Article article : all) {
            if (article.id == null) {
                continue;
            }
            if (needle.isEmpty() || String.valueOf(article.id).contains(needle)
                    || (article.title != null && article.title.toLowerCase(Locale.ROOT).contains(needle))) {
                matches.add(new ArticleSuggestion(article.id, String.valueOf(article.id), article.title,
                        "/articles/" + article.id));
            }
            if (matches.size() >= 6) {
                break;
            }
        }
        return new ArticleSuggestionResponse(matches);
    }

    @GET
    @Path("/{id}")
    @Transactional
    public ArticleDetailResponse detail(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireUser(auth);
        ArticleResource.ensureSampleArticle();
        Article article = ArticleResource.findArticleWithAttachments(id);
        if (article == null) {
            throw new NotFoundException();
        }
        List<ArticleAttachment> attachments = article.attachments.stream().map(this::toAttachmentResponse).toList();
        return new ArticleDetailResponse(article.id, article.title, article.tags, article.body,
                ArticleResource.canEdit(user), AuthHelper.isAdmin(user),
                ArticleResource.canEdit(user) ? "/articles/" + article.id + "/edit" : null, attachments);
    }

    private User requireUser(String auth) {
        User user = AuthHelper.findUser(auth);
        if (user == null) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        return user;
    }

    private ArticleAttachment toAttachmentResponse(Attachment attachment) {
        return new ArticleAttachment(attachment.id, attachment.name, attachment.mimeType, attachment.sizeLabel(),
                "/attachments/" + attachment.id + "/data");
    }

    public record ArticleListResponse(boolean canCreate, String createPath, List<ArticleSummary> items) {
    }

    public record ArticleBootstrapResponse(boolean canEdit, boolean canDelete) {
    }

    public record ArticleSummary(Long id, String title, String tags) {
    }

    public record ArticleSuggestionResponse(List<ArticleSuggestion> items) {
    }

    public record ArticleSuggestion(Long id, String name, String title, String detailPath) {
    }

    public record ArticleDetailResponse(Long id, String title, String tags, String body, boolean canEdit,
            boolean canDelete, String editPath, List<ArticleAttachment> attachments) {
    }

    public record ArticleAttachment(Long id, String name, String mimeType, String sizeLabel, String downloadPath) {
    }
}
