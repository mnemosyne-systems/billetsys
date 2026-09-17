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
import ai.mnemosyne_systems.util.CurrentUser;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Path("/api/articles")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
@RolesAllowed({ "admin", "support", "superuser", "tam", "user" })
public class ArticleApiResource {

    @Inject
    CurrentUser currentUser;

    @GET
    @Transactional
    public ArticleListResponse list(@QueryParam("page") Integer page, @QueryParam("pageSize") Integer pageSize,
            @QueryParam("sort") String sort, @QueryParam("dir") String dir, @QueryParam("q") String q) {
        User user = currentUser.get();

        ArticleResource.ensureSampleArticle();
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<ArticleSummary> allItems = Article.<Article> list("order by lower(title), id").stream()
                .filter(article -> needle.isEmpty()
                        || (article.title != null && article.title.toLowerCase(Locale.ROOT).contains(needle)))
                .map(article -> new ArticleSummary(article.id, article.title, article.tags)).toList();
        int totalItems = allItems.size();
        java.util.Map<String, PaginationSupport.SortColumn<ArticleSummary>> sortColumns = java.util.Map.of("title",
                PaginationSupport.sortColumn(ArticleSummary::title, String.CASE_INSENSITIVE_ORDER), "tags",
                PaginationSupport.sortColumn(ArticleSummary::tags, String.CASE_INSENSITIVE_ORDER));
        List<ArticleSummary> pageItems = PaginationSupport.sortAndPaginate(allItems, sort, dir, sortColumns, page,
                pageSize);
        PaginationSupport.PaginationMeta meta = PaginationSupport.meta(page, pageSize, totalItems);
        return new ArticleListResponse(ArticleResource.canEdit(user), "/articles/new", pageItems, meta.page(),
                meta.pageSize(), meta.totalItems(), meta.totalPages());
    }

    @GET
    @Path("/bootstrap")
    public ArticleBootstrapResponse bootstrap() {
        User user = currentUser.get();
        return new ArticleBootstrapResponse(ArticleResource.canEdit(user), AuthHelper.isAdmin(user));
    }

    @GET
    @Path("/suggest")
    @Transactional
    public ArticleSuggestionResponse suggest(@QueryParam("q") @DefaultValue("") String q) {
        ArticleResource.ensureSampleArticle();
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<Article> all = Article.<Article> list("order by id desc");
        List<ArticleSuggestion> matches = new ArrayList<>();
        for (Article article : all) {
            if (article.id == null) {
                continue;
            }
            if (needle.isEmpty()
                    || (article.title != null && article.title.toLowerCase(Locale.ROOT).contains(needle))) {
                matches.add(new ArticleSuggestion(article.id, article.title, article.title, "/articles/" + article.id));
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
    public ArticleDetailResponse detail(@PathParam("id") Long id) {
        User user = currentUser.get();
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

    private ArticleAttachment toAttachmentResponse(Attachment attachment) {
        return new ArticleAttachment(attachment.id, attachment.name, attachment.mimeType, attachment.sizeLabel(),
                "/attachments/" + attachment.id + "/data");
    }

    public record ArticleListResponse(boolean canCreate, String createPath, List<ArticleSummary> items, int page,
            int pageSize, int totalItems, int totalPages) {
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
