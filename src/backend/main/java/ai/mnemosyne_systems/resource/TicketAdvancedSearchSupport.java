/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.TicketCustomFieldValue;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.event.Event;
import ai.mnemosyne_systems.model.event.EventConstants;
import ai.mnemosyne_systems.service.TicketCustomFieldService;
import ai.mnemosyne_systems.service.TicketCustomFieldService.CustomFieldDefinition;
import ai.mnemosyne_systems.util.AuthHelper;
import io.quarkus.hibernate.orm.panache.Panache;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class TicketAdvancedSearchSupport {

    private TicketAdvancedSearchSupport() {
    }

    record Criteria(String q, Set<String> statuses, Long categoryId, Long companyId, Long companyEntitlementId,
            Long entitlementId, Long levelId, String entitlementName, String levelName, Long affectsVersionId,
            Long resolvedVersionId, Long requesterId, String requester, Long supportUserId, String supportUser,
            Long tamUserId, String tamUser, LocalDate activityFrom, LocalDate activityTo, LocalDate createdFrom,
            LocalDate createdTo, LocalDate resolvedFrom, LocalDate resolvedTo, List<CustomFieldFilter> customFields) {
    }

    private enum CustomFieldOperation {
        TEXT_CONTAINS, SELECT_EXACT, NUMBER_EQUALS, NUMBER_RANGE, DATE_EQUALS, DATE_RANGE
    }

    private record CustomFieldFilter(long fieldId, CustomFieldOperation operation, String text, BigDecimal number,
            BigDecimal upperNumber, LocalDate date, LocalDate upperDate) {
    }

    static Criteria parse(UriInfo uriInfo, User viewer, TicketCustomFieldService customFieldService) {
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        boolean support = AuthHelper.isSupport(viewer);
        boolean superuser = AuthHelper.isSuperuser(viewer);
        boolean tam = AuthHelper.isTam(viewer);

        boolean canUseCompanyFilters = support || superuser || tam;
        boolean canUseTamFilters = support || superuser;
        boolean canUseSupportFilter = support || superuser || tam;

        String q = trimToNull(params.getFirst("q"));
        Set<String> statuses = parseStatuses(params);
        Long categoryId = parseLong(params, "categoryId");
        Long companyId = parseLong(params, "companyId");
        Long companyEntitlementId = parseLong(params, "companyEntitlementId");
        Long entitlementId = parseLong(params, "entitlementId");
        Long levelId = parseLong(params, "levelId");
        String entitlementName = trimToNull(params.getFirst("entitlementName"));
        String levelName = trimToNull(params.getFirst("levelName"));
        Long affectsVersionId = parseLong(params, "affectsVersionId");
        Long resolvedVersionId = parseLong(params, "resolvedVersionId");
        Long requesterId = parseLong(params, "requesterId");
        String requester = trimToNull(params.getFirst("requester"));
        Long supportUserId = parseLong(params, "supportUserId");
        String supportUser = trimToNull(params.getFirst("supportUser"));
        Long tamUserId = parseLong(params, "tamUserId");
        String tamUser = trimToNull(params.getFirst("tamUser"));

        LocalDate activityFrom = parseDate(params, "activityFrom");
        LocalDate activityTo = parseDate(params, "activityTo");
        LocalDate createdFrom = parseDate(params, "createdFrom");
        LocalDate createdTo = parseDate(params, "createdTo");
        LocalDate resolvedFrom = parseDate(params, "resolvedFrom");
        LocalDate resolvedTo = parseDate(params, "resolvedTo");
        validateRange(activityFrom, activityTo, "activity");
        validateRange(createdFrom, createdTo, "created");
        validateRange(resolvedFrom, resolvedTo, "resolved");

        List<CustomFieldFilter> customFields = parseCustomFields(params, customFieldService);

        if (!canUseCompanyFilters) {
            companyId = null;
            companyEntitlementId = null;
            entitlementId = null;
            levelId = null;
            entitlementName = null;
            levelName = null;
        }
        if (!canUseSupportFilter) {
            supportUserId = null;
            supportUser = null;
        }
        if (!canUseTamFilters) {
            tamUserId = null;
            tamUser = null;
        }
        // Requester data is not exposed by any ticket list or detail response today, so it is never applied.
        requesterId = null;
        requester = null;

        return new Criteria(q, statuses, categoryId, companyId, companyEntitlementId, entitlementId, levelId,
                entitlementName, levelName, affectsVersionId, resolvedVersionId, requesterId, requester, supportUserId,
                supportUser, tamUserId, tamUser, activityFrom, activityTo, createdFrom, createdTo, resolvedFrom,
                resolvedTo, customFields);
    }

    static List<Ticket> filter(List<Ticket> scopedTickets, Criteria criteria, User viewer,
            Map<Long, LocalDateTime> activityDates, TicketCustomFieldService customFieldService) {
        List<Ticket> scoped = scopedTickets == null ? List.of() : scopedTickets;
        Set<Long> scopedIds = new LinkedHashSet<>();
        Map<Long, String> displayStatuses = new HashMap<>();
        for (Ticket ticket : scoped) {
            if (ticket != null && ticket.id != null) {
                scopedIds.add(ticket.id);
                displayStatuses.putIfAbsent(ticket.id, ticket.status);
            }
        }
        if (scopedIds.isEmpty()) {
            return List.of();
        }

        List<Ticket> originals = Ticket.list("id in ?1", List.copyOf(scopedIds));
        if (originals.isEmpty()) {
            return List.of();
        }

        List<Message> messages = List.of();
        Map<Long, LocalDateTime> earliestVisibleMessageDates = new HashMap<>();
        Set<Long> messageMatchIds = new LinkedHashSet<>();
        if (criteria.q() != null || criteria.createdFrom() != null || criteria.createdTo() != null) {
            messages = Panache.getEntityManager()
                    .createQuery("select m from Message m where m.ticket.id in :ticketIds", Message.class)
                    .setParameter("ticketIds", List.copyOf(scopedIds)).getResultList();
            String normalizedQuery = criteria.q() == null ? null : criteria.q().toLowerCase(Locale.ENGLISH);
            for (Message message : messages) {
                if (message == null || message.ticket == null || message.ticket.id == null
                        || !MessageVisibilitySupport.canViewMessage(viewer, message)) {
                    continue;
                }
                if (message.date != null) {
                    earliestVisibleMessageDates.merge(message.ticket.id, message.date,
                            (left, right) -> left.isBefore(right) ? left : right);
                }
                if (normalizedQuery != null && containsIgnoreCase(message.body, normalizedQuery)) {
                    messageMatchIds.add(message.ticket.id);
                }
            }
        }

        Map<Long, LocalDateTime> openedDates = new HashMap<>();
        if (criteria.createdFrom() != null || criteria.createdTo() != null) {
            List<Event> openedEvents = Panache.getEntityManager()
                    .createQuery("select e from Event e where e.key in :keys and e.eventType = :eventType", Event.class)
                    .setParameter("keys", List.copyOf(scopedIds))
                    .setParameter("eventType", EventConstants.TICKET_OPENED).getResultList();
            for (Event event : openedEvents) {
                if (event == null || event.key == null || event.createdAt == null) {
                    continue;
                }
                openedDates.merge(event.key, event.createdAt, (left, right) -> left.isBefore(right) ? left : right);
            }
        }

        Map<Long, Map<Long, TicketCustomFieldValue>> customValues = new HashMap<>();
        if (!criteria.customFields().isEmpty()) {
            List<TicketCustomFieldValue> values = Panache.getEntityManager().createQuery(
                    "select v from TicketCustomFieldValue v join fetch v.customField where v.ticket.id in :ticketIds",
                    TicketCustomFieldValue.class).setParameter("ticketIds", List.copyOf(scopedIds)).getResultList();
            for (TicketCustomFieldValue value : values) {
                if (value == null || value.ticket == null || value.ticket.id == null || value.customField == null
                        || value.customField.id == null) {
                    continue;
                }
                customValues.computeIfAbsent(value.ticket.id, ignored -> new HashMap<>()).put(value.customField.id,
                        value);
            }
        }

        String normalizedQuery = criteria.q() == null ? null : criteria.q().toLowerCase(Locale.ENGLISH);
        List<Ticket> result = new ArrayList<>();
        for (Ticket ticket : originals) {
            if (!scopedIds.contains(ticket.id)) {
                continue;
            }
            if (!matchesText(ticket, normalizedQuery, messageMatchIds)) {
                continue;
            }
            if (!criteria.statuses().isEmpty()) {
                String displayStatus = TicketSearchSupport
                        .displayStatus(displayStatuses.getOrDefault(ticket.id, ticket.status), viewer);
                if (!criteria.statuses().contains(displayStatus.trim().toLowerCase(Locale.ENGLISH))) {
                    continue;
                }
            }
            if (!matchesId(criteria.categoryId(), ticket.category == null ? null : ticket.category.id)) {
                continue;
            }
            if (!matchesId(criteria.companyId(), ticket.company == null ? null : ticket.company.id)) {
                continue;
            }
            if (!matchesId(criteria.companyEntitlementId(),
                    ticket.companyEntitlement == null ? null : ticket.companyEntitlement.id)) {
                continue;
            }
            if (!matchesId(criteria.entitlementId(),
                    ticket.companyEntitlement == null || ticket.companyEntitlement.entitlement == null ? null
                            : ticket.companyEntitlement.entitlement.id)) {
                continue;
            }
            if (!matchesId(criteria.levelId(),
                    ticket.companyEntitlement == null || ticket.companyEntitlement.supportLevel == null ? null
                            : ticket.companyEntitlement.supportLevel.id)) {
                continue;
            }
            if (!matchesName(criteria.entitlementName(),
                    ticket.companyEntitlement == null || ticket.companyEntitlement.entitlement == null ? null
                            : ticket.companyEntitlement.entitlement.name)) {
                continue;
            }
            if (!matchesName(criteria.levelName(),
                    ticket.companyEntitlement == null || ticket.companyEntitlement.supportLevel == null ? null
                            : ticket.companyEntitlement.supportLevel.name)) {
                continue;
            }
            if (!matchesId(criteria.affectsVersionId(),
                    ticket.affectsVersion == null ? null : ticket.affectsVersion.id)) {
                continue;
            }
            if (!matchesId(criteria.resolvedVersionId(),
                    ticket.resolvedVersion == null ? null : ticket.resolvedVersion.id)) {
                continue;
            }
            if (!matchesUser(ticket.supportUsers, criteria.supportUserId(), criteria.supportUser())) {
                continue;
            }
            if (!matchesUser(ticket.tamUsers, criteria.tamUserId(), criteria.tamUser())) {
                continue;
            }
            if (!matchesDate(activityDates.get(ticket.id), criteria.activityFrom(), criteria.activityTo())) {
                continue;
            }
            LocalDateTime createdDate = openedDates.get(ticket.id);
            if (createdDate == null) {
                createdDate = earliestVisibleMessageDates.get(ticket.id);
            }
            if (!matchesDate(createdDate, criteria.createdFrom(), criteria.createdTo())) {
                continue;
            }
            if (!matchesDate(ticket.resolvedAt, criteria.resolvedFrom(), criteria.resolvedTo())) {
                continue;
            }
            if (!matchesCustomFields(ticket.id, criteria.customFields(), customValues)) {
                continue;
            }
            result.add(ticket);
        }
        return result.stream().filter(ticket -> ticket != null && scopedIds.contains(ticket.id)).toList();
    }

    static Map<Long, Ticket> indexById(List<Ticket> tickets) {
        Map<Long, Ticket> indexed = new LinkedHashMap<>();
        if (tickets == null) {
            return indexed;
        }
        for (Ticket ticket : tickets) {
            if (ticket != null && ticket.id != null) {
                indexed.putIfAbsent(ticket.id, ticket);
            }
        }
        return indexed;
    }

    private static boolean matchesText(Ticket ticket, String normalizedQuery, Set<Long> messageMatchIds) {
        if (normalizedQuery == null) {
            return true;
        }
        return containsIgnoreCase(ticket.name, normalizedQuery) || containsIgnoreCase(ticket.title, normalizedQuery)
                || (ticket.id != null && messageMatchIds.contains(ticket.id));
    }

    private static boolean matchesId(Long expected, Long actual) {
        return expected == null || (actual != null && expected.equals(actual));
    }

    private static boolean matchesName(String expected, String actual) {
        return expected == null || (actual != null && expected.equalsIgnoreCase(actual.trim()));
    }

    private static boolean matchesUser(List<User> users, Long expectedId, String expectedText) {
        if (expectedId == null && expectedText == null) {
            return true;
        }
        if (users == null || users.isEmpty()) {
            return false;
        }
        String normalizedText = expectedText == null ? null : expectedText.trim().toLowerCase(Locale.ENGLISH);
        for (User user : users) {
            if (user == null) {
                continue;
            }
            if (expectedId != null && expectedId.equals(user.id)) {
                return true;
            }
            if (normalizedText != null && (containsIgnoreCase(user.getDisplayName(), normalizedText)
                    || containsIgnoreCase(user.email, normalizedText))) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesDate(LocalDateTime value, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return true;
        }
        if (value == null) {
            return false;
        }
        LocalDate date = value.toLocalDate();
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    private static boolean matchesCustomFields(long ticketId, List<CustomFieldFilter> filters,
            Map<Long, Map<Long, TicketCustomFieldValue>> values) {
        if (filters.isEmpty()) {
            return true;
        }
        Map<Long, TicketCustomFieldValue> ticketValues = values.get(ticketId);
        for (CustomFieldFilter filter : filters) {
            TicketCustomFieldValue value = ticketValues == null ? null : ticketValues.get(filter.fieldId());
            if (value == null || !matchesCustomField(filter, value)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesCustomField(CustomFieldFilter filter, TicketCustomFieldValue value) {
        return switch (filter.operation()) {
            case TEXT_CONTAINS -> containsIgnoreCase(value.textValue, filter.text().toLowerCase(Locale.ENGLISH));
            case SELECT_EXACT -> filter.text().equals(value.textValue);
            case NUMBER_EQUALS -> value.numberValue != null && value.numberValue.compareTo(filter.number()) == 0;
            case NUMBER_RANGE -> value.numberValue != null
                    && (filter.number() == null || value.numberValue.compareTo(filter.number()) >= 0)
                    && (filter.upperNumber() == null || value.numberValue.compareTo(filter.upperNumber()) <= 0);
            case DATE_EQUALS -> filter.date().equals(value.dateValue);
            case DATE_RANGE ->
                    value.dateValue != null && (filter.date() == null || !value.dateValue.isBefore(filter.date()))
                            && (filter.upperDate() == null || !value.dateValue.isAfter(filter.upperDate()));
        };
    }

    private static boolean containsIgnoreCase(String value, String normalizedNeedle) {
        if (value == null || normalizedNeedle == null) {
            return false;
        }
        return value.toLowerCase(Locale.ENGLISH).contains(normalizedNeedle);
    }

    private static Set<String> parseStatuses(MultivaluedMap<String, String> params) {
        List<String> values = params.get("status");
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> statuses = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isEmpty())
                    .map(item -> item.toLowerCase(Locale.ENGLISH)).forEach(statuses::add);
        }
        return statuses;
    }

    private static Long parseLong(MultivaluedMap<String, String> params, String name) {
        String value = trimToNull(params.getFirst(name));
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw badRequest(name + " must be a number");
        }
    }

    private static LocalDate parseDate(MultivaluedMap<String, String> params, String name) {
        String value = trimToNull(params.getFirst(name));
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw badRequest(name + " must be a valid date (YYYY-MM-DD)");
        }
    }

    private static void validateRange(LocalDate from, LocalDate to, String name) {
        if (from != null && to != null && from.isAfter(to)) {
            throw badRequest(name + "From must not be after " + name + "To");
        }
    }

    private static List<CustomFieldFilter> parseCustomFields(MultivaluedMap<String, String> params,
            TicketCustomFieldService customFieldService) {
        Map<Long, CustomFieldParts> partsByField = new LinkedHashMap<>();
        for (String key : params.keySet()) {
            if (key == null || !key.startsWith("cf.")) {
                continue;
            }
            String suffix = key.substring(3);
            String idPart = suffix;
            String part = "value";
            if (suffix.endsWith(".from")) {
                idPart = suffix.substring(0, suffix.length() - 5);
                part = "from";
            } else if (suffix.endsWith(".to")) {
                idPart = suffix.substring(0, suffix.length() - 3);
                part = "to";
            }
            Long fieldId;
            try {
                fieldId = Long.parseLong(idPart);
            } catch (NumberFormatException exception) {
                throw badRequest("Invalid custom field parameter " + key);
            }
            CustomFieldParts parts = partsByField.computeIfAbsent(fieldId, ignored -> new CustomFieldParts());
            String value = trimToNull(params.getFirst(key));
            switch (part) {
                case "from" -> parts.from = value;
                case "to" -> parts.to = value;
                default -> parts.value = value;
            }
        }
        if (partsByField.isEmpty()) {
            return List.of();
        }

        Map<Long, CustomFieldDefinition> definitions = new HashMap<>();
        for (CustomFieldDefinition definition : customFieldService.activeDefinitions()) {
            definitions.put(definition.id(), definition);
        }

        List<CustomFieldFilter> filters = new ArrayList<>();
        for (Map.Entry<Long, CustomFieldParts> entry : partsByField.entrySet()) {
            Long fieldId = entry.getKey();
            CustomFieldParts parts = entry.getValue();
            CustomFieldDefinition definition = definitions.get(fieldId);
            if (definition == null) {
                throw badRequest("Unknown or inactive custom field: " + fieldId);
            }
            filters.add(toCustomFieldFilter(fieldId, definition, parts));
        }
        return filters;
    }

    private static CustomFieldFilter toCustomFieldFilter(Long fieldId, CustomFieldDefinition definition,
            CustomFieldParts parts) {
        boolean hasValue = parts.value != null;
        boolean hasRange = parts.from != null || parts.to != null;
        if (hasValue && hasRange) {
            throw badRequest("Custom field " + fieldId + " cannot combine a value with a range");
        }
        if (!hasValue && !hasRange) {
            throw badRequest("Custom field " + fieldId + " requires a value or range");
        }

        String type = definition.fieldType();
        return switch (type) {
            case "TEXT" -> {
                if (hasRange) {
                    throw badRequest("Custom field " + fieldId + " does not support ranges");
                }
                yield new CustomFieldFilter(fieldId, CustomFieldOperation.TEXT_CONTAINS, parts.value, null, null, null,
                        null);
            }
            case "SELECT" -> {
                if (hasRange) {
                    throw badRequest("Custom field " + fieldId + " does not support ranges");
                }
                if (definition.options() == null || !definition.options().contains(parts.value)) {
                    throw badRequest("Custom field " + fieldId + " has an invalid option");
                }
                yield new CustomFieldFilter(fieldId, CustomFieldOperation.SELECT_EXACT, parts.value, null, null, null,
                        null);
            }
            case "NUMBER" -> {
                BigDecimal number = parts.value == null ? null : parseNumber(parts.value, fieldId);
                BigDecimal from = parts.from == null ? null : parseNumber(parts.from, fieldId);
                BigDecimal to = parts.to == null ? null : parseNumber(parts.to, fieldId);
                if (from != null && to != null && from.compareTo(to) > 0) {
                    throw badRequest("Custom field " + fieldId + " from must not be after to");
                }
                if (hasValue) {
                    yield new CustomFieldFilter(fieldId, CustomFieldOperation.NUMBER_EQUALS, null, number, null, null,
                            null);
                }
                yield new CustomFieldFilter(fieldId, CustomFieldOperation.NUMBER_RANGE, null, from, to, null, null);
            }
            case "DATE" -> {
                LocalDate date = parts.value == null ? null : parseDateValue(parts.value, fieldId);
                LocalDate from = parts.from == null ? null : parseDateValue(parts.from, fieldId);
                LocalDate to = parts.to == null ? null : parseDateValue(parts.to, fieldId);
                if (from != null && to != null && from.isAfter(to)) {
                    throw badRequest("Custom field " + fieldId + " from must not be after to");
                }
                if (hasValue) {
                    yield new CustomFieldFilter(fieldId, CustomFieldOperation.DATE_EQUALS, null, null, null, date,
                            null);
                }
                yield new CustomFieldFilter(fieldId, CustomFieldOperation.DATE_RANGE, null, null, null, from, to);
            }
            default -> throw badRequest("Unsupported custom field type for " + fieldId);
        };
    }

    private static BigDecimal parseNumber(String value, Long fieldId) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw badRequest("Custom field " + fieldId + " must be a number");
        }
    }

    private static LocalDate parseDateValue(String value, Long fieldId) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw badRequest("Custom field " + fieldId + " must be a valid date (YYYY-MM-DD)");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BadRequestException badRequest(String message) {
        return new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(message)
                .type(jakarta.ws.rs.core.MediaType.TEXT_PLAIN_TYPE).build());
    }

    private static final class CustomFieldParts {
        private String value;
        private String from;
        private String to;
    }
}
