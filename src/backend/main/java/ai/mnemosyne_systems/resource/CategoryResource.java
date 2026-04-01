/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.Attachment;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AttachmentHelper;
import ai.mnemosyne_systems.util.AuthHelper;
import io.quarkus.hibernate.orm.panache.Panache;
import io.smallrye.common.annotation.Blocking;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
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
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/categories")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class CategoryResource {

    @GET
    public Response list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireAdmin(auth);
        return Response.seeOther(URI.create("/categories")).build();
    }

    @GET
    @Path("/create")
    public Response createForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireAdmin(auth);
        return Response.seeOther(URI.create("/categories/new")).build();
    }

    @GET
    @Path("/{id}/edit")
    public Response editForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireAdmin(auth);
        if (findCategoryWithAttachments(id) == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/categories/" + id + "/edit")).build();
    }

    @GET
    @Path("/{id}")
    public Response view(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireAdmin(auth);
        if (findCategoryWithAttachments(id) == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/categories/" + id)).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response create(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @HeaderParam("X-Billetsys-Client") String client, MultipartFormDataInput input) {
        requireAdmin(auth);
        String name = AttachmentHelper.readFormValue(input, "name");
        String description = AttachmentHelper.readFormValue(input, "description");
        String isDefault = AttachmentHelper.readFormValue(input, "isDefault");
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
        boolean makeDefault = "true".equalsIgnoreCase(isDefault);
        if (makeDefault) {
            clearDefaults();
        }
        Category category = new Category();
        category.name = name.trim();
        category.description = description == null ? "" : description.trim();
        category.isDefault = makeDefault;
        category.persist();
        List<Attachment> attachments = storeAttachments(category,
                AttachmentHelper.readAttachments(input, "attachments"));
        category.description = resolveInlineAttachmentUrls(category.description, attachments);
        return ReactRedirectSupport.redirect(client, "/categories");
    }

    @POST
    @Path("/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response update(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            @HeaderParam("X-Billetsys-Client") String client, MultipartFormDataInput input) {
        requireAdmin(auth);
        Category category = Category
                .find("select distinct c from Category c left join fetch c.attachments where c.id = ?1", id)
                .firstResult();
        if (category == null) {
            throw new NotFoundException();
        }
        String name = AttachmentHelper.readFormValue(input, "name");
        String description = AttachmentHelper.readFormValue(input, "description");
        String isDefault = AttachmentHelper.readFormValue(input, "isDefault");
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
        boolean makeDefault = "true".equalsIgnoreCase(isDefault);
        if (makeDefault) {
            clearDefaults();
        }
        category.name = name.trim();
        category.description = description == null ? "" : description.trim();
        category.isDefault = makeDefault;
        List<Attachment> attachments = storeAttachments(category,
                AttachmentHelper.readAttachments(input, "attachments"));
        category.description = resolveInlineAttachmentUrls(category.description, attachments);
        return ReactRedirectSupport.redirect(client, "/categories");
    }

    static Category findCategoryWithAttachments(Long id) {
        return Category.find("select distinct c from Category c left join fetch c.attachments where c.id = ?1", id)
                .firstResult();
    }

    static String firstLinePlainText(String description) {
        if (description == null || description.isBlank()) {
            return "";
        }
        String firstLine = description.replace("\r\n", "\n").split("\n", 2)[0].trim();
        firstLine = firstLine.replaceAll("\\[([^\\]]+)]\\(([^)]+)\\)", "$1");
        firstLine = firstLine.replaceAll("^```[a-zA-Z0-9_+\\-]*\\s*", "");
        firstLine = firstLine.replace("```", "");
        firstLine = firstLine.replaceAll("^[>#*\\-\\s]+", "");
        firstLine = firstLine.replace("**", "").replace("__", "").replace("`", "").replace("*", "").replace("_", "");
        return firstLine.replaceAll("\\s+", " ").trim();
    }

    @POST
    @Path("/{id}/delete")
    @Transactional
    public Response delete(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @HeaderParam("X-Billetsys-Client") String client, @PathParam("id") Long id) {
        requireAdmin(auth);
        Category category = Category.findById(id);
        if (category == null) {
            throw new NotFoundException();
        }
        Attachment.delete("category", category);
        category.delete();
        return ReactRedirectSupport.redirect(client, "/categories");
    }

    private List<Attachment> storeAttachments(Category category, List<Attachment> uploaded) {
        for (Attachment upload : uploaded) {
            upload.message = null;
            upload.article = null;
            upload.category = category;
            upload.persist();
            category.attachments.add(upload);
        }
        Panache.getEntityManager().flush();
        return uploaded;
    }

    private String resolveInlineAttachmentUrls(String description, List<Attachment> attachments) {
        if (description == null || description.isBlank() || attachments == null || attachments.isEmpty()) {
            return description;
        }
        String updated = description;
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

    private void clearDefaults() {
        List<Category> defaults = Category.list("isDefault", true);
        for (Category existing : defaults) {
            existing.isDefault = false;
        }
    }

    private User requireAdmin(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isAdmin(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }
}
