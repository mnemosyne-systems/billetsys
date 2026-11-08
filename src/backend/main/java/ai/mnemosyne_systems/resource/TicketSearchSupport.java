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
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import io.quarkus.hibernate.orm.panache.Panache;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class TicketSearchSupport {

    private TicketSearchSupport() {
    }

    static String normalizeSearchTerm(String searchTerm) {
        if (searchTerm == null) {
            return null;
        }
        String normalized = searchTerm.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Status label shown by the ticket lists. Assigned tickets are already normalized by the list data builders; this
     * additionally mirrors the summary normalization for statuses that users and superusers never see verbatim.
     */
    static String displayStatus(String status, User viewer) {
        if (status == null || status.isBlank()) {
            return "Open";
        }
        if (!AuthHelper.isSupport(viewer) && "Waiting for External Feedback".equalsIgnoreCase(status.trim())) {
            return "In Progress";
        }
        return status;
    }

    static List<Ticket> filterTicketsBySearch(List<Ticket> tickets, String searchTerm, User viewer) {
        List<Ticket> scopedTickets = tickets == null ? List.of() : tickets;
        String normalizedSearchTerm = normalizeSearchTerm(searchTerm);
        if (normalizedSearchTerm == null || scopedTickets.isEmpty()) {
            return scopedTickets;
        }
        String normalizedQuery = normalizedSearchTerm.toLowerCase(Locale.ENGLISH);
        Set<Long> messageTicketIds = findMessageMatchTicketIds(scopedTickets, normalizedQuery, viewer);
        return scopedTickets.stream().filter(ticket -> matchesTicketNumber(ticket, normalizedQuery)
                || (ticket != null && ticket.id != null && messageTicketIds.contains(ticket.id))).toList();
    }

    static List<Ticket> suggestTickets(List<Ticket> tickets, String searchTerm, int limit) {
        List<Ticket> scopedTickets = tickets == null ? List.of() : tickets;
        String normalizedSearchTerm = normalizeSearchTerm(searchTerm);
        if (normalizedSearchTerm == null || scopedTickets.isEmpty() || limit <= 0) {
            return List.of();
        }
        String normalizedQuery = normalizedSearchTerm.toLowerCase(Locale.ENGLISH);
        return scopedTickets.stream().filter(
                ticket -> matchesTicketNumber(ticket, normalizedQuery) || matchesTicketTitle(ticket, normalizedQuery))
                .limit(limit).toList();
    }

    @SafeVarargs
    static List<Ticket> combineTickets(List<Ticket>... ticketGroups) {
        if (ticketGroups == null || ticketGroups.length == 0) {
            return List.of();
        }
        List<Ticket> combinedTickets = new ArrayList<>();
        Set<Long> seenIds = new LinkedHashSet<>();
        for (List<Ticket> ticketGroup : ticketGroups) {
            if (ticketGroup == null) {
                continue;
            }
            for (Ticket ticket : ticketGroup) {
                if (ticket == null) {
                    continue;
                }
                if (ticket.id == null) {
                    combinedTickets.add(ticket);
                    continue;
                }
                if (seenIds.add(ticket.id)) {
                    combinedTickets.add(ticket);
                }
            }
        }
        return combinedTickets;
    }

    private static boolean matchesTicketNumber(Ticket ticket, String normalizedQuery) {
        return ticket != null && ticket.name != null
                && ticket.name.toLowerCase(Locale.ENGLISH).contains(normalizedQuery);
    }

    private static boolean matchesTicketTitle(Ticket ticket, String normalizedQuery) {
        return ticket != null && ticket.title != null
                && ticket.title.toLowerCase(Locale.ENGLISH).contains(normalizedQuery);
    }

    private static Set<Long> findMessageMatchTicketIds(List<Ticket> tickets, String normalizedQuery, User viewer) {
        List<Long> ticketIds = tickets.stream().filter(ticket -> ticket != null && ticket.id != null)
                .map(ticket -> ticket.id).toList();
        if (ticketIds.isEmpty()) {
            return Set.of();
        }
        List<Message> matchingMessages = Panache.getEntityManager().createQuery(
                "select distinct m from Message m where m.ticket.id in :ticketIds and lower(coalesce(m.body, '')) like :search",
                Message.class).setParameter("ticketIds", ticketIds).setParameter("search", "%" + normalizedQuery + "%")
                .getResultList();
        Set<Long> matchingIds = new LinkedHashSet<>();
        for (Message message : matchingMessages) {
            if (message == null || message.ticket == null || message.ticket.id == null
                    || !MessageVisibilitySupport.canViewMessage(viewer, message)) {
                continue;
            }
            matchingIds.add(message.ticket.id);
        }
        return matchingIds;
    }
}
