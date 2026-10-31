/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.model.CustomField;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.TicketCustomFieldValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Central custom-field definition lookup, validation and value persistence.
 * <p>
 * Definitions are installation-wide (no company scope) and cached as detached DTOs only. Every definition write must
 * call {@link #invalidateDefinitions()} so connected ticket forms see the change immediately; the TTL in
 * {@code application.properties} is only a backstop. Values are stored in typed columns so advanced search can query
 * them without a redesign.
 */
@ApplicationScoped
public class TicketCustomFieldService {

    public static final List<String> FIELD_TYPES = List.of(CustomField.TYPE_TEXT, CustomField.TYPE_NUMBER,
            CustomField.TYPE_DATE, CustomField.TYPE_SELECT);

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int NUMBER_SCALE = 10;
    private static final int NUMBER_INTEGER_DIGITS = 28;

    public record CustomFieldDefinition(Long id, String name, String fieldType, boolean required, boolean active,
            String description, int sortOrder, List<String> options) {
    }

    public record CustomFieldView(Long id, String name, String fieldType, boolean required, boolean active,
            String description, int sortOrder, List<String> options, String value) {
    }

    @CacheResult(cacheName = "custom-field-definitions")
    public List<CustomFieldDefinition> activeDefinitions() {
        return CustomField.<CustomField> find(
                "select distinct cf from CustomField cf left join fetch cf.options where cf.active = true order by cf.sortOrder asc, cf.id asc")
                .list().stream().map(this::toDefinition).toList();
    }

    @CacheInvalidateAll(cacheName = "custom-field-definitions")
    public void invalidateDefinitions() {
    }

    public static String normalizeName(String name) {
        return name == null ? null : name.trim();
    }

    public static String normalizeNameKey(String name) {
        String normalized = normalizeName(name);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    public static List<CustomFieldView> toViews(List<CustomFieldDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            return List.of();
        }
        return definitions.stream()
                .map(definition -> new CustomFieldView(definition.id(), definition.name(), definition.fieldType(),
                        definition.required(), definition.active(), definition.description(), definition.sortOrder(),
                        definition.options(), null))
                .toList();
    }

    /**
     * Renders the fields a ticket should show: every active definition plus inactive definitions that already have a
     * value on this ticket. Inactive-without-value fields disappear from the form and from the detail page.
     */
    public List<CustomFieldView> valuesForTicket(Ticket ticket) {
        if (ticket == null) {
            return List.of();
        }
        List<TicketCustomFieldValue> values = TicketCustomFieldValue.find(
                "select distinct v from TicketCustomFieldValue v join fetch v.customField cf left join fetch cf.options where v.ticket = ?1",
                ticket).list();
        Map<Long, TicketCustomFieldValue> byField = new LinkedHashMap<>();
        for (TicketCustomFieldValue value : values) {
            if (value.customField != null && value.customField.id != null) {
                byField.put(value.customField.id, value);
            }
        }
        List<CustomFieldDefinition> definitions = new ArrayList<>(activeDefinitions());
        for (TicketCustomFieldValue value : values) {
            if (value.customField != null && !value.customField.active
                    && definitions.stream().noneMatch(entry -> entry.id().equals(value.customField.id))) {
                definitions.add(toDefinition(value.customField));
            }
        }
        definitions.sort(Comparator.comparingInt(CustomFieldDefinition::sortOrder)
                .thenComparing(CustomFieldDefinition::id, Comparator.nullsLast(Comparator.naturalOrder())));
        return definitions.stream()
                .map(definition -> new CustomFieldView(definition.id(), definition.name(), definition.fieldType(),
                        definition.required(), definition.active(), definition.description(), definition.sortOrder(),
                        definition.options(), displayValue(byField.get(definition.id()))))
                .toList();
    }

    public Map<Long, String> parseSubmission(String customFieldsJson) {
        if (customFieldsJson == null || customFieldsJson.isBlank()) {
            return Map.of();
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(customFieldsJson);
        } catch (JsonProcessingException ex) {
            throw badRequest("Custom fields must be valid JSON");
        }
        if (root == null || !root.isObject()) {
            throw badRequest("Custom fields must be a JSON object");
        }
        Map<Long, String> values = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            Long fieldId;
            try {
                fieldId = Long.valueOf(entry.getKey().trim());
            } catch (NumberFormatException ex) {
                throw badRequest("Invalid custom field id: " + entry.getKey());
            }
            if (fieldId <= 0) {
                throw badRequest("Invalid custom field id: " + entry.getKey());
            }
            JsonNode node = entry.getValue();
            if (node == null || node.isNull()) {
                values.put(fieldId, "");
                continue;
            }
            if (!node.isTextual()) {
                throw badRequest("Custom field " + fieldId + " must be a string");
            }
            values.put(fieldId, node.asText());
        }
        return values;
    }

    /**
     * Validates and persists submitted values. A field that is not present in the JSON is left untouched; an empty
     * string clears a non-required field. Required fields are enforced on create, and an existing required value can
     * never be cleared on update (tickets created before the field existed stay editable).
     */
    public void saveValues(Ticket ticket, String customFieldsJson, boolean creating) {
        saveValues(ticket, customFieldsJson, creating, null);
    }

    public void saveValues(Ticket ticket, String customFieldsJson, boolean creating, String targetStatus) {
        if (ticket == null) {
            return;
        }
        Map<Long, String> submitted = parseSubmission(customFieldsJson);
        Map<Long, CustomField> activeFields = activeFieldsById();
        for (Long fieldId : submitted.keySet()) {
            if (!activeFields.containsKey(fieldId)) {
                throw badRequest("Unknown or inactive custom field: " + fieldId);
            }
        }
        ticket.customFieldValues.size();
        Map<Long, TicketCustomFieldValue> existing = new LinkedHashMap<>();
        for (TicketCustomFieldValue value : ticket.customFieldValues) {
            if (value.customField != null && value.customField.id != null) {
                existing.put(value.customField.id, value);
            }
        }

        if (creating) {
            for (CustomField field : activeFields.values()) {
                if (field.required && isBlank(submitted.get(field.id))) {
                    throw badRequest("Custom field '" + field.name + "' is required");
                }
            }
        } else if (isClosed(ticket) && (targetStatus == null || isClosedStatus(targetStatus))) {
            for (Map.Entry<Long, String> entry : submitted.entrySet()) {
                CustomField field = activeFields.get(entry.getKey());
                if (!matches(field, existing.get(entry.getKey()), entry.getValue())) {
                    throw badRequest("Custom fields cannot be changed on a closed ticket");
                }
            }
            return;
        }

        for (Map.Entry<Long, String> entry : submitted.entrySet()) {
            CustomField field = activeFields.get(entry.getKey());
            String raw = entry.getValue();
            TicketCustomFieldValue value = existing.get(field.id);
            if (isBlank(raw)) {
                if (field.required && value != null && hasValue(value)) {
                    throw badRequest("Custom field '" + field.name + "' is required");
                }
                if (value != null) {
                    ticket.customFieldValues.remove(value);
                }
                continue;
            }
            if (value == null) {
                value = new TicketCustomFieldValue();
                value.ticket = ticket;
                value.customField = field;
                ticket.customFieldValues.add(value);
                value.persist();
            }
            applyValidatedValue(field, value, raw);
        }
    }

    private Map<Long, CustomField> activeFieldsById() {
        List<CustomField> fields = CustomField
                .<CustomField> find(
                        "select distinct cf from CustomField cf left join fetch cf.options where cf.active = true")
                .list();
        Map<Long, CustomField> result = new LinkedHashMap<>();
        for (CustomField field : fields) {
            result.put(field.id, field);
        }
        return result;
    }

    private void applyValidatedValue(CustomField field, TicketCustomFieldValue value, String raw) {
        switch (field.fieldType) {
            case CustomField.TYPE_TEXT -> {
                value.textValue = raw;
                value.numberValue = null;
                value.dateValue = null;
            }
            case CustomField.TYPE_NUMBER -> {
                value.textValue = null;
                value.numberValue = parseNumber(field, raw);
                value.dateValue = null;
            }
            case CustomField.TYPE_DATE -> {
                value.textValue = null;
                value.numberValue = null;
                value.dateValue = parseDate(field, raw);
            }
            case CustomField.TYPE_SELECT -> {
                if (field.options == null || field.options.stream().noneMatch(option -> option.label.equals(raw))) {
                    throw badRequest("Custom field '" + field.name + "' must be one of: "
                            + String.join(", ", optionLabels(field)));
                }
                value.textValue = raw;
                value.numberValue = null;
                value.dateValue = null;
            }
            default -> throw badRequest("Invalid field type for custom field '" + field.name + "'");
        }
    }

    private boolean matches(CustomField field, TicketCustomFieldValue value, String raw) {
        if (value == null) {
            return isBlank(raw);
        }
        if (isBlank(raw)) {
            return false;
        }
        return switch (field.fieldType) {
            case CustomField.TYPE_TEXT, CustomField.TYPE_SELECT -> raw.equals(value.textValue);
            case CustomField.TYPE_NUMBER ->
                    value.numberValue != null && parseNumber(field, raw).compareTo(value.numberValue) == 0;
            case CustomField.TYPE_DATE -> value.dateValue != null && parseDate(field, raw).equals(value.dateValue);
            default -> false;
        };
    }

    private BigDecimal parseNumber(CustomField field, String raw) {
        BigDecimal number;
        try {
            number = new BigDecimal(raw.trim());
        } catch (NumberFormatException ex) {
            throw badRequest("Custom field '" + field.name + "' must be a number");
        }
        if (number.scale() > NUMBER_SCALE) {
            throw badRequest("Custom field '" + field.name + "' must have at most " + NUMBER_SCALE + " decimal places");
        }
        if (number.precision() - number.scale() > NUMBER_INTEGER_DIGITS) {
            throw badRequest("Custom field '" + field.name + "' is too large for a numeric value");
        }
        return number;
    }

    private LocalDate parseDate(CustomField field, String raw) {
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw badRequest("Custom field '" + field.name + "' must be a valid date (YYYY-MM-DD)");
        }
    }

    private String displayValue(TicketCustomFieldValue value) {
        if (value == null) {
            return null;
        }
        return switch (value.customField.fieldType) {
            case CustomField.TYPE_NUMBER ->
                    value.numberValue == null ? null : value.numberValue.stripTrailingZeros().toPlainString();
            case CustomField.TYPE_DATE -> value.dateValue == null ? null : value.dateValue.toString();
            default -> value.textValue;
        };
    }

    private CustomFieldDefinition toDefinition(CustomField field) {
        return new CustomFieldDefinition(field.id, field.name, field.fieldType, field.required, field.active,
                field.description, field.sortOrder, optionLabels(field));
    }

    private List<String> optionLabels(CustomField field) {
        if (field.options == null || field.options.isEmpty()) {
            return List.of();
        }
        return field.options.stream().map(option -> option.label).toList();
    }

    private boolean hasValue(TicketCustomFieldValue value) {
        return value.textValue != null || value.numberValue != null || value.dateValue != null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isClosed(Ticket ticket) {
        return ticket.status != null && isClosedStatus(ticket.status);
    }

    private boolean isClosedStatus(String status) {
        return status != null && "Closed".equalsIgnoreCase(status);
    }

    private static BadRequestException badRequest(String message) {
        return new BadRequestException(
                Response.status(Response.Status.BAD_REQUEST).entity(message).type(MediaType.TEXT_PLAIN_TYPE).build());
    }
}
