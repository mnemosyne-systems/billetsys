package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.event.Event;
import ai.mnemosyne_systems.model.event.EventAction;
import ai.mnemosyne_systems.model.event.EventType;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class EventService {
    private final Map<String, EventAction> eventTypeMap = Map.of("Open", EventAction.OPENED, "Assigned",
            EventAction.ASSIGNED, "Closed", EventAction.CLOSED, "Updated", EventAction.UPDATED, "Deleted",
            EventAction.DELETED, "Expired", EventAction.EXPIRED, "Created", EventAction.CREATED, "In Progress",
            EventAction.IN_PROGRESS, "Resolved", EventAction.RESOLVED, "Unknown", EventAction.UNKNOWN);

    public void saveTicketEvent(Ticket ticket, User createdBy) {
        saveEvent(ticket.id, EventType.TICKET, eventTypeMap.getOrDefault(ticket.status, EventAction.UNKNOWN),
                ticket.company, createdBy);
    }

    public List<Event> getAllChangesToEntity(int entityId) {
        List<Event> events;
        events = Event.find("entityId = :entityId", Sort.by("createdAt").ascending(), Map.of("entityId", entityId))
                .list();
        return events;
    }

    private void saveEvent(Long entityId, EventType entityType, EventAction action, Company company, User user) {
        Event event = new Event();
        event.entityId = entityId;
        event.entityType = entityType;
        event.action = action;
        if (company != null) {
            event.company = company;
        }
        if (user != null) {
            event.user = user;
        }
        event.persist();
    }

}
