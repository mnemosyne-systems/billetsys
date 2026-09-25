/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */
package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.event.Event;
import ai.mnemosyne_systems.model.event.EventConstants;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class EventService {

    @Inject
    ReportService reportService;

    @Inject
    DirectoryService directoryService;

    @Inject
    TicketBootstrapService bootstrapService;

    public void saveTicketEvent(Ticket ticket, User createdBy) {
        if (ticket == null || ticket.id == null) {
            return;
        }
        if (Event.count("key = ?1 and eventType = ?2", ticket.id, EventConstants.TICKET_OPENED) == 0) {
            record(ticket.id, EventConstants.TICKET_OPENED, ticket.company == null ? null : ticket.company.id,
                    createdBy == null ? null : createdBy.id, "Ticket opened");
        }
        if (ticket.status != null) {
            handleActionEvent(ticket, createdBy);
        }
        if (ticket.category != null && ticket.category.name != null) {
            handleCategoryEvent(ticket, createdBy);
        }
        reportService.invalidateAll();
    }

    public List<Event> getAllChangesToEntity(Long entityId) {
        return getAllChangesToKey(entityId);
    }

    public List<Event> getAllChangesToKey(Long key) {
        return Event.find("key = :key", Sort.by("createdAt").ascending(), Map.of("key", key)).list();
    }

    public void recordMessageCreated(Message message) {
        if (message == null || message.id == null) {
            return;
        }
        Long companyId = message.ticket == null || message.ticket.company == null ? null : message.ticket.company.id;
        record(message.id, EventConstants.MESSAGE_CREATED, companyId, message.author == null ? null : message.author.id,
                "Comment created");
        reportService.invalidateAll();
    }

    public void recordTicketUserAssociation(Ticket ticket, User actor, User member, long eventType) {
        if (ticket == null || ticket.id == null) {
            return;
        }
        String memberName = member == null || member.fullName == null || member.fullName.isBlank() ? "User"
                : member.fullName;
        record(ticket.id, eventType, ticket.company == null ? null : ticket.company.id, actor == null ? null : actor.id,
                memberName);
        reportService.invalidateAll();
    }

    public void record(Long key, Long eventType, Long companyId, Long userId, String eventValue) {
        saveEvent(key, eventType, eventValue, companyId, userId);
        invalidateDirectoryCaches(key, eventType, companyId);
    }

    /**
     * Targeted directory invalidation for user/company lifecycle events.
     * <p>
     * User events invalidate the user's own company set plus every user list they belong to (resolved fresh, so missing
     * {@code companyId} params at individual call sites cannot leak stale entries). Company events invalidate the
     * company's user list plus every member's set.
     */
    private void invalidateDirectoryCaches(Long key, Long eventType, Long companyId) {
        if (eventType == null) {
            return;
        }
        if (eventType == EventConstants.USER_CREATED || eventType == EventConstants.USER_DELETED
                || eventType == EventConstants.USER_ACTIVATED || eventType == EventConstants.USER_DEACTIVATED) {
            directoryService.invalidateUserEverywhere(key, companyId == null ? List.of() : List.of(companyId));
        } else if (eventType == EventConstants.COMPANY_CREATED || eventType == EventConstants.COMPANY_DELETED) {
            if (companyId != null) {
                directoryService.invalidateUsers(companyId);
                directoryService.invalidateCompaniesForMembers(companyId);
            } else {
                directoryService.invalidateAllCompanies();
            }
            bootstrapService.invalidateAllCompanies();
        } else if (eventType == EventConstants.COMPANY_ENTITLEMENT_CREATED
                || eventType == EventConstants.COMPANY_ENTITLEMENT_DELETED) {
            bootstrapService.invalidateEntitlementsForCompany(companyId);
        } else if (eventType == EventConstants.ENTITLEMENT_CREATED || eventType == EventConstants.ENTITLEMENT_DELETED) {
            bootstrapService.invalidateAllEntitlementOptions();
            if (key != null) {
                bootstrapService.invalidateVersions(key);
            }
        } else if (eventType == EventConstants.LEVEL_CREATED || eventType == EventConstants.LEVEL_DELETED) {
            bootstrapService.invalidateAllEntitlementOptions();
        } else if (eventType == EventConstants.CATEGORY_CREATED || eventType == EventConstants.CATEGORY_DELETED) {
            bootstrapService.invalidateAllCategories();
        }
    }

    private void handleActionEvent(Ticket ticket, User createdBy) {
        Event lastActionEvent = Event.find("key = :key AND eventType = :type", Sort.by("createdAt").descending(),
                Map.of("key", ticket.id, "type", EventConstants.TICKET_STATUS_CHANGED)).firstResult();

        boolean hasChanged = lastActionEvent == null || !ticket.status.toUpperCase().equals(lastActionEvent.eventValue);

        if (hasChanged) {
            saveEvent(ticket.id, EventConstants.TICKET_STATUS_CHANGED, ticket.status.toUpperCase(),
                    ticket.company == null ? null : ticket.company.id, createdBy == null ? null : createdBy.id);
            recordTicketStatusEvent(ticket, createdBy);
        }
    }

    private void handleCategoryEvent(Ticket ticket, User createdBy) {
        Event lastCategoryEvent = Event.find("key = :key AND eventType = :type", Sort.by("createdAt").descending(),
                Map.of("key", ticket.id, "type", EventConstants.TICKET_CATEGORY_CHANGED)).firstResult();

        boolean hasChanged = lastCategoryEvent == null
                || !ticket.category.name.toUpperCase().equals(lastCategoryEvent.eventValue);

        if (hasChanged) {
            saveEvent(ticket.id, EventConstants.TICKET_CATEGORY_CHANGED, ticket.category.name.toUpperCase(),
                    ticket.company == null ? null : ticket.company.id, createdBy == null ? null : createdBy.id);
        }
    }

    public void recordTicketDeleted(Ticket ticket, User deletedBy) {
        if (ticket != null) {
            record(ticket.id, EventConstants.TICKET_DELETED, ticket.company == null ? null : ticket.company.id,
                    deletedBy == null ? null : deletedBy.id, "Ticket deleted");
            reportService.invalidateAll();
        }
    }

    private void recordTicketStatusEvent(Ticket ticket, User actor) {
        String status = ticket.status == null ? "" : ticket.status.trim();
        long eventType = switch (status.toLowerCase()) {
            case "assigned" -> EventConstants.TICKET_ASSIGNED;
            case "resolved" -> EventConstants.TICKET_RESOLVED;
            case "closed" -> EventConstants.TICKET_CLOSED;
            default -> -1L;
        };
        if (eventType != -1L) {
            record(ticket.id, eventType, ticket.company == null ? null : ticket.company.id,
                    actor == null ? null : actor.id, "Ticket " + status.toLowerCase());
        }
    }

    private void saveEvent(Long key, Long eventType, String eventValue, Long companyId, Long userId) {
        Event event = new Event();
        event.key = key;
        event.eventType = eventType;
        event.eventValue = eventValue;
        event.companyId = companyId;
        event.userId = userId;
        event.persist();
    }
}
