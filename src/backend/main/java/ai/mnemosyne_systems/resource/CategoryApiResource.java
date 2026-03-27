/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Attachment;
import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import io.smallrye.common.annotation.Blocking;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/categories")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
public class CategoryApiResource {

    @GET
    @Transactional
    public CategoryListResponse list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireAdmin(auth);
        List<CategorySummary> items = Category
                .<Category> list("order by name asc").stream().map(category -> new CategorySummary(category.id,
                        category.name, CategoryResource.firstLinePlainText(category.description), category.isDefault))
                .toList();
        return new CategoryListResponse(true, "/categories/new", items);
    }

    @GET
    @Path("/bootstrap")
    public CategoryBootstrapResponse bootstrap(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireAdmin(auth);
        return new CategoryBootstrapResponse(true);
    }

    @GET
    @Path("/{id}")
    @Transactional
    public CategoryDetailResponse detail(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireAdmin(auth);
        Category category = CategoryResource.findCategoryWithAttachments(id);
        if (category == null) {
            throw new NotFoundException();
        }
        List<CategoryAttachment> attachments = category.attachments.stream().map(this::toAttachmentResponse).toList();
        return new CategoryDetailResponse(category.id, category.name, category.description, category.isDefault,
                "/categories/" + category.id + "/edit", attachments);
    }

    private User requireAdmin(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isAdmin(user)) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        return user;
    }

    private CategoryAttachment toAttachmentResponse(Attachment attachment) {
        return new CategoryAttachment(attachment.id, attachment.name, attachment.mimeType, attachment.sizeLabel(),
                "/attachments/" + attachment.id + "/data");
    }

    public record CategoryListResponse(boolean canCreate, String createPath, List<CategorySummary> items) {
    }

    public record CategoryBootstrapResponse(boolean canEdit) {
    }

    public record CategorySummary(Long id, String name, String descriptionPreview, boolean isDefault) {
    }

    public record CategoryDetailResponse(Long id, String name, String description, boolean isDefault, String editPath,
            List<CategoryAttachment> attachments) {
    }

    public record CategoryAttachment(Long id, String name, String mimeType, String sizeLabel, String downloadPath) {
    }
}
