/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.CustomField;
import ai.mnemosyne_systems.service.TicketCustomFieldService;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/custom-fields")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
@RolesAllowed("admin")
public class CustomFieldApiResource {

    @GET
    @Transactional
    public CustomFieldListResponse list() {
        List<CustomFieldSummary> items = CustomField.<CustomField> find(
                "select distinct cf from CustomField cf left join fetch cf.options order by cf.sortOrder asc, cf.id asc")
                .list().stream().map(this::toSummary).toList();
        return new CustomFieldListResponse(true, "/custom-fields/new", items);
    }

    @GET
    @Path("/bootstrap")
    public CustomFieldBootstrapResponse bootstrap() {
        return new CustomFieldBootstrapResponse(true, TicketCustomFieldService.FIELD_TYPES);
    }

    @GET
    @Path("/{id}")
    @Transactional
    public CustomFieldDetailResponse detail(@PathParam("id") Long id) {
        CustomField field = CustomField
                .find("select distinct cf from CustomField cf left join fetch cf.options where cf.id = ?1", id)
                .firstResult();
        if (field == null) {
            throw new NotFoundException();
        }
        return new CustomFieldDetailResponse(field.id, field.name, field.fieldType, field.required, field.description,
                field.active, field.sortOrder, options(field), "/custom-fields/" + field.id + "/edit",
                "/custom-fields/" + field.id + "/deactivate");
    }

    private CustomFieldSummary toSummary(CustomField field) {
        return new CustomFieldSummary(field.id, field.name, field.fieldType, field.required, field.active,
                field.sortOrder, field.description, options(field), "/custom-fields/" + field.id + "/edit",
                "/custom-fields/" + field.id + "/deactivate");
    }

    private List<String> options(CustomField field) {
        return field.options == null ? List.of() : field.options.stream().map(option -> option.label).toList();
    }

    public record CustomFieldListResponse(boolean canCreate, String createPath, List<CustomFieldSummary> items) {
    }

    public record CustomFieldBootstrapResponse(boolean canEdit, List<String> fieldTypes) {
    }

    public record CustomFieldSummary(Long id, String name, String fieldType, boolean required, boolean active,
            int sortOrder, String description, List<String> options, String editPath, String deactivatePath) {
    }

    public record CustomFieldDetailResponse(Long id, String name, String fieldType, boolean required,
            String description, boolean active, int sortOrder, List<String> options, String editPath,
            String deactivatePath) {
    }
}
