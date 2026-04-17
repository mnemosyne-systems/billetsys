package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.infra.NotFoundMapper;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.event.Event;
import ai.mnemosyne_systems.model.event.EventAction;
import ai.mnemosyne_systems.model.event.EventType;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class EventService {
    private final Map<String, EventAction> eventTypeMap = Map.of("Open", EventAction.OPENED, "Assigned",
            EventAction.ASSIGNED, "Closed", EventAction.CLOSED, "Updated", EventAction.UPDATED, "Deleted",
            EventAction.DELETED, "Expired", EventAction.EXPIRED, "Created", EventAction.CREATED, "In Progress",
            EventAction.IN_PROGRESS, "Resolved", EventAction.RESOLVED);

    public void saveEvent(Long entityId, EventType entityType, String action, Company company) {
        Event event = new Event();
        event.entityId = entityId;
        event.entityType = entityType;
        try {
            event.action = eventTypeMap.get(action);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        if (company != null) {
            event.company = company;
        }
        event.persist();
    }

}
