\newpage

# Events

## Overview

The event system tracks all changes made to an entity throughout its lifecycle.
Every time an entity is created or updated, the system records an event that captures
what changed, who made the change, and when it happened.

> Currently, the event system supports **Tickets**. It is designed to be extended to other entities in the future.

An event is built on two core concepts:

- **Event** — the record of a change that happened to an entity
- **EventType** — the classification of what kind of change occurred

---

## Event Types

Event types are grouped into two categories: `ACTION` and `CATEGORY`.

### ACTION

An action event is recorded when the **status of a ticket changes**.

| Name | Description |
|---|---|
| `OPEN` | Triggered when a ticket is first created |
| `ASSIGNED` | The ticket was assigned to a support user |
| `IN_PROGRESS` | Work on the ticket has started |
| `RESOLVED` | The ticket has been resolved |
| `CLOSED` | The ticket was closed |
| `EXPIRED` | The ticket expired without resolution |
| `UNKNOWN` | The action could not be determined |
| `CREATED` | _(Reserved for future use)_ |
| `UPDATED` | _(Reserved for future use)_ |
| `DELETED` | _(Reserved for future use)_ |

### CATEGORY

A category event is recorded when the **type of a ticket changes**.

| Name | Description |
|---|---|
| `BUG` | The ticket was classified as a bug |
| `FEATURE` | The ticket was classified as a feature request |
| `QUESTION` | The ticket was classified as a question |

---

## How It Works

Events are triggered in two scenarios for a **Ticket**:

**1. Ticket Created**
When a user creates a new ticket, the system records:
- An `OPEN` action event
- A category event matching the ticket's initial category (e.g. `BUG`)

**2. Ticket Updated**
When a support user updates a ticket, the system compares the ticket's current state
against the last recorded event for each type. A new event is only saved if something
has actually changed.

```
Ticket updated
     │
     ├── Has the status changed since the last ACTION event?
     │        └── Yes → Save a new ACTION event
     │
     └── Has the category changed since the last CATEGORY event?
              └── Yes → Save a new CATEGORY event
```

---

## Event Structure

| Field | Type | Description |
|---|---|---|
| `id` | Long | Unique identifier |
| `entityId` | Long | ID of the entity this event belongs to |
| `entityType` | Enum | The type of entity (e.g. `TICKET`) |
| `eventType` | EventType | The classification of the event |
| `company` | Company | The company the ticket belongs to |
| `user` | User | The user who triggered the event |
| `createdAt` | LocalDateTime | Timestamp of when the event was recorded |

---

## Example Lifecycle

Below is an example of events recorded for a ticket from creation to resolution:

```
[2024-01-01 09:00]  ACTION    → OPEN        (ticket created by user)
[2024-01-01 09:00]  CATEGORY  → BUG         (initial category set)
[2024-01-02 10:30]  ACTION    → ASSIGNED   (assigned to support)
[2024-01-03 14:00]  ACTION    → IN_PROGRESS
[2024-01-04 11:00]  CATEGORY  → FEATURE    (category changed from BUG)
[2024-01-05 16:45]  ACTION    → RESOLVED
[2024-01-06 09:00]  ACTION    → CLOSED
```
