/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.CustomField;
import ai.mnemosyne_systems.model.CustomFieldOption;
import ai.mnemosyne_systems.model.TicketCustomFieldValue;
import ai.mnemosyne_systems.model.event.EventConstants;
import ai.mnemosyne_systems.service.EventService;
import ai.mnemosyne_systems.service.TicketCustomFieldService;
import ai.mnemosyne_systems.util.AttachmentHelper;
import ai.mnemosyne_systems.util.CurrentUser;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/custom-fields")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
@RolesAllowed("admin")
public class CustomFieldResource {

    @Inject
    CurrentUser currentUser;

    @Inject
    EventService eventService;

    @Inject
    TicketCustomFieldService ticketCustomFieldService;

    @GET
    public Response list() {
        return Response.seeOther(URI.create("/custom-fields")).build();
    }

    @GET
    @Path("/create")
    public Response createForm() {
        return Response.seeOther(URI.create("/custom-fields/new")).build();
    }

    @GET
    @Path("/{id}/edit")
    public Response editForm(@PathParam("id") Long id) {
        if (findWithOptions(id) == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/custom-fields/" + id + "/edit")).build();
    }

    @GET
    @Path("/{id}")
    public Response view(@PathParam("id") Long id) {
        if (findWithOptions(id) == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/custom-fields/" + id + "/edit")).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response create(@HeaderParam("X-Billetsys-Client") String client, MultipartFormDataInput input) {
        String name = requireName(input, null);
        String fieldType = requireFieldType(AttachmentHelper.readFormValue(input, "fieldType"));
        CustomField field = new CustomField();
        field.name = name;
        field.nameKey = TicketCustomFieldService.normalizeNameKey(name);
        field.fieldType = fieldType;
        field.required = AttachmentHelper.readFormBoolean(input, "required", false);
        field.description = normalizeDescription(AttachmentHelper.readFormValue(input, "description"));
        field.active = AttachmentHelper.readFormBoolean(input, "active", true);
        field.sortOrder = parseSortOrder(AttachmentHelper.readFormValue(input, "sortOrder"));
        if (CustomField.TYPE_SELECT.equals(fieldType)) {
            applyOptions(field, parseOptionLines(AttachmentHelper.readFormValue(input, "options")));
        }
        field.persist();
        eventService.record(field.id, EventConstants.CUSTOM_FIELD_CREATED, null, currentUser.get().id,
                "Custom field created");
        ticketCustomFieldService.invalidateDefinitions();
        return ReactRedirectSupport.redirect(client, "/custom-fields");
    }

    @POST
    @Path("/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response update(@PathParam("id") Long id, @HeaderParam("X-Billetsys-Client") String client,
            MultipartFormDataInput input) {
        CustomField field = findWithOptions(id);
        if (field == null) {
            throw new NotFoundException();
        }
        String fieldType = requireFieldType(AttachmentHelper.readFormValue(input, "fieldType"));
        if (!fieldType.equals(field.fieldType)) {
            throw badRequest("Field type cannot be changed");
        }
        String name = requireName(input, id);
        field.name = name;
        field.nameKey = TicketCustomFieldService.normalizeNameKey(name);
        field.required = AttachmentHelper.readFormBoolean(input, "required", field.required);
        field.description = normalizeDescription(AttachmentHelper.readFormValue(input, "description"));
        field.active = AttachmentHelper.readFormBoolean(input, "active", field.active);
        field.sortOrder = parseSortOrder(AttachmentHelper.readFormValue(input, "sortOrder"));
        applyOptions(field,
                CustomField.TYPE_SELECT.equals(fieldType)
                        ? parseOptionLines(AttachmentHelper.readFormValue(input, "options"))
                        : List.of());
        eventService.record(field.id, EventConstants.CUSTOM_FIELD_UPDATED, null, currentUser.get().id,
                "Custom field updated");
        ticketCustomFieldService.invalidateDefinitions();
        return ReactRedirectSupport.redirect(client, "/custom-fields");
    }

    @POST
    @Path("/{id}/deactivate")
    @Transactional
    public Response deactivate(@PathParam("id") Long id, @HeaderParam("X-Billetsys-Client") String client) {
        CustomField field = findWithOptions(id);
        if (field == null) {
            throw new NotFoundException();
        }
        field.active = false;
        eventService.record(field.id, EventConstants.CUSTOM_FIELD_DEACTIVATED, null, currentUser.get().id,
                "Custom field deactivated");
        ticketCustomFieldService.invalidateDefinitions();
        return ReactRedirectSupport.redirect(client, "/custom-fields");
    }

    @POST
    @Path("/{id}/delete")
    @Transactional
    public Response delete(@PathParam("id") Long id, @HeaderParam("X-Billetsys-Client") String client) {
        CustomField field = findWithOptions(id);
        if (field == null) {
            throw new NotFoundException();
        }
        TicketCustomFieldValue.delete("customField", field);
        eventService.record(field.id, EventConstants.CUSTOM_FIELD_DELETED, null, currentUser.get().id,
                "Custom field deleted");
        field.delete();
        ticketCustomFieldService.invalidateDefinitions();
        return ReactRedirectSupport.redirect(client, "/custom-fields");
    }

    private CustomField findWithOptions(Long id) {
        return CustomField
                .find("select distinct cf from CustomField cf left join fetch cf.options where cf.id = ?1", id)
                .firstResult();
    }

    private String requireName(MultipartFormDataInput input, Long excludeId) {
        String name = TicketCustomFieldService.normalizeName(AttachmentHelper.readFormValue(input, "name"));
        if (name == null || name.isBlank()) {
            throw badRequest("Name is required");
        }
        String nameKey = TicketCustomFieldService.normalizeNameKey(name);
        long duplicates = excludeId == null ? CustomField.count("nameKey", nameKey)
                : CustomField.count("nameKey = ?1 and id <> ?2", nameKey, excludeId);
        if (duplicates > 0) {
            throw badRequest("A custom field with this name already exists");
        }
        return name;
    }

    private String requireFieldType(String raw) {
        String fieldType = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!TicketCustomFieldService.FIELD_TYPES.contains(fieldType)) {
            throw new BadRequestException(
                    "Field type must be one of: " + String.join(", ", TicketCustomFieldService.FIELD_TYPES));
        }
        return fieldType;
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int parseSortOrder(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw badRequest("Sort order must be a number");
        }
    }

    private List<String> parseOptionLines(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        Map<String, String> labels = new LinkedHashMap<>();
        for (String line : raw.split("\\R")) {
            String label = line.trim();
            if (label.isEmpty()) {
                continue;
            }
            if (labels.putIfAbsent(label, label) != null) {
                throw badRequest("Duplicate option: " + label);
            }
        }
        return new ArrayList<>(labels.keySet());
    }

    private void applyOptions(CustomField field, List<String> labels) {
        Map<String, CustomFieldOption> existing = new LinkedHashMap<>();
        for (CustomFieldOption option : field.options) {
            existing.put(option.label, option);
        }
        for (CustomFieldOption option : new ArrayList<>(field.options)) {
            if (!labels.contains(option.label)) {
                long inUse = TicketCustomFieldValue.count("customField = ?1 and textValue = ?2", field, option.label);
                if (inUse > 0) {
                    throw badRequest("Option '" + option.label + "' is in use and cannot be removed or renamed");
                }
                field.options.remove(option);
            }
        }
        for (int index = 0; index < labels.size(); index++) {
            String label = labels.get(index);
            CustomFieldOption option = existing.get(label);
            if (option == null) {
                option = new CustomFieldOption();
                option.customField = field;
                option.label = label;
                field.options.add(option);
            }
            option.sortOrder = index;
        }
    }

    private static BadRequestException badRequest(String message) {
        return new BadRequestException(
                Response.status(Response.Status.BAD_REQUEST).entity(message).type(MediaType.TEXT_PLAIN_TYPE).build());
    }
}
