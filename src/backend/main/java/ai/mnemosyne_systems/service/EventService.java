/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */
package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.event.*;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class EventService {

    public void saveTicketEvent(Ticket ticket, User createdBy) {
        if (ticket.status != null) {
            handleActionEvent(ticket, createdBy);
        }
        if (ticket.category != null && ticket.category.name != null) {
            handleCategoryEvent(ticket, createdBy);
        }
    }

    public List<Event> getAllChangesToEntity(Long entityId) {
        return Event.find("entityId = :entityId", Sort.by("createdAt").ascending(), Map.of("entityId", entityId))
                .list();
    }

    private void handleActionEvent(Ticket ticket, User createdBy) {
        Event lastActionEvent = Event.find("entityId = :entityId AND eventType.type = :type",
                Sort.by("createdAt").descending(), Map.of("entityId", ticket.id, "type", EventTypeEnum.ACTION.name()))
                .firstResult();

        boolean hasChanged = lastActionEvent == null
                || !lastActionEvent.eventType.name.equals(ticket.status.toUpperCase());

        if (hasChanged) {
            EventType eventType = EventType
                    .find("name = :name AND type = :type",
                            Map.of("name", ticket.status.toUpperCase(), "type", EventTypeEnum.ACTION.name()))
                    .firstResult();
            if (eventType != null) {
                saveEvent(ticket.id, EventEntityType.TICKET, eventType, ticket.company, createdBy);
            }
        }
    }

    private void handleCategoryEvent(Ticket ticket, User createdBy) {
        Event lastCategoryEvent = Event.find("entityId = :entityId AND eventType.type = :type",
                Sort.by("createdAt").descending(), Map.of("entityId", ticket.id, "type", EventTypeEnum.CATEGORY.name()))
                .firstResult();

        boolean hasChanged = lastCategoryEvent == null
                || !lastCategoryEvent.eventType.name.equals(ticket.category.name.toUpperCase());

        if (hasChanged) {
            EventType eventType = EventType
                    .find("name = :name AND type = :type",
                            Map.of("name", ticket.category.name.toUpperCase(), "type", EventTypeEnum.CATEGORY.name()))
                    .firstResult();
            if (eventType != null) {
                saveEvent(ticket.id, EventEntityType.TICKET, eventType, ticket.company, createdBy);
            }
        }
    }

    private void saveEvent(Long entityId, EventEntityType entityType, EventType eventType, Company company, User user) {
        Event event = new Event();
        event.entityId = entityId;
        event.entityType = entityType;
        event.eventType = eventType;
        event.company = company;
        event.user = user;
        event.persist();
    }
}
