package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.Version;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class SupportTicketViewSupport {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d yyyy, h.mma",
            Locale.ENGLISH);

    private SupportTicketViewSupport() {
    }

    static List<Message> loadMessages(Ticket ticket) {
        List<Message> messages = Message.find(
                "select distinct m from Message m left join fetch m.attachments where m.ticket = ?1 order by m.date desc",
                ticket).list();
        if (messages.isEmpty()) {
            return messages;
        }
        return new ArrayList<>(new LinkedHashSet<>(messages));
    }

    static List<CompanyEntitlement> uniqueEntitlements(List<CompanyEntitlement> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        List<CompanyEntitlement> unique = new ArrayList<>();
        Set<Long> seenEntitlementIds = new LinkedHashSet<>();
        for (CompanyEntitlement entry : entries) {
            if (entry == null || entry.entitlement == null || entry.entitlement.id == null) {
                continue;
            }
            if (seenEntitlementIds.add(entry.entitlement.id)) {
                unique.add(entry);
            }
        }
        return unique;
    }

    static List<Version> availableVersions(CompanyEntitlement companyEntitlement) {
        if (companyEntitlement == null || companyEntitlement.entitlement == null) {
            return List.of();
        }
        return Version.list("entitlement = ?1 order by date asc, id asc", companyEntitlement.entitlement);
    }

    static Version defaultAffectsVersion(CompanyEntitlement companyEntitlement) {
        if (companyEntitlement == null || companyEntitlement.entitlement == null) {
            return null;
        }
        Version version = Version.find("entitlement = ?1 and name = ?2 order by date asc, id asc",
                companyEntitlement.entitlement, "1.0.0").firstResult();
        if (version != null) {
            return version;
        }
        return Version.find("entitlement = ?1 order by date asc, id asc", companyEntitlement.entitlement).firstResult();
    }

    static String formatDate(LocalDateTime date) {
        String formatted = DATE_FORMATTER.format(date);
        return formatted.replace("AM", "am").replace("PM", "pm");
    }

    static SupportTicketCounts loadTicketCounts(User user) {
        if (user == null) {
            return new SupportTicketCounts(0, 0);
        }
        Long assignedCount = Ticket.count(
                "select distinct t from Ticket t join t.supportUsers u where u = ?1 and (t.status is null or lower(t.status) <> 'closed')",
                user);
        Long openCount = Ticket.count("select distinct t from Ticket t where t.supportUsers is empty");
        return new SupportTicketCounts(assignedCount.intValue(), openCount.intValue());
    }

    static boolean isEntitlementExpired(Ticket ticket) {
        if (ticket == null || ticket.companyEntitlement == null || ticket.companyEntitlement.date == null
                || ticket.companyEntitlement.duration == null) {
            return false;
        }
        LocalDate endDate = ticket.companyEntitlement.date;
        if (ticket.companyEntitlement.duration == CompanyEntitlement.DURATION_MONTHLY) {
            endDate = endDate.plusMonths(1);
        } else if (ticket.companyEntitlement.duration == CompanyEntitlement.DURATION_YEARLY) {
            endDate = endDate.plusYears(1);
        } else {
            return false;
        }
        return LocalDate.now().isAfter(endDate);
    }

    static SupportTicketData buildTicketData(User user) {
        List<Ticket> tickets = Ticket.listAll();
        Map<Long, LocalDateTime> messageDates = new LinkedHashMap<>();
        Map<Long, String> messageDateLabels = new LinkedHashMap<>();
        Map<Long, String> messageDirectionArrows = new LinkedHashMap<>();
        List<Message> messages = Message.find("order by date desc").list();
        for (Message message : messages) {
            if (message.ticket != null && !messageDates.containsKey(message.ticket.id)) {
                messageDates.put(message.ticket.id, message.date);
                if (message.date != null) {
                    messageDateLabels.put(message.ticket.id, formatDate(message.date));
                }
                messageDirectionArrows.put(message.ticket.id, messageDirectionArrow(message.author));
            }
        }
        for (Ticket ticket : tickets) {
            if (!messageDateLabels.containsKey(ticket.id)) {
                messageDateLabels.put(ticket.id, "-");
            }
        }
        Map<Long, String> slaColors = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (Ticket ticket : tickets) {
            if (isEntitlementExpired(ticket)) {
                slaColors.put(ticket.id, "Black");
                continue;
            }
            LocalDateTime messageDate = messageDates.get(ticket.id);
            if (messageDate == null || ticket.companyEntitlement == null
                    || ticket.companyEntitlement.supportLevel == null) {
                continue;
            }
            long minutes = Duration.between(messageDate, now).toMinutes();
            if (minutes < 0) {
                minutes = 0;
            }
            String color = resolveSlaColor(ticket.companyEntitlement.supportLevel, minutes);
            if (color != null && !color.isBlank()) {
                slaColors.put(ticket.id, color);
            }
        }
        Map<Long, String> supportAssignments = new LinkedHashMap<>();
        Map<Long, String> supportAssignmentNames = new LinkedHashMap<>();
        Map<Long, Long> supportAssignmentIds = new LinkedHashMap<>();
        Map<Long, User> supportAssignmentUsers = new LinkedHashMap<>();
        for (Ticket ticket : tickets) {
            User assignedSupport = User
                    .find("select u from Ticket t join t.supportUsers u where t = ?1 order by u.id desc", ticket)
                    .firstResult();
            if (assignedSupport != null) {
                supportAssignments.put(ticket.id, assignedSupport.email);
                supportAssignmentNames.put(ticket.id, assignedSupport.name);
                supportAssignmentIds.put(ticket.id, assignedSupport.id);
                supportAssignmentUsers.put(ticket.id, assignedSupport);
            }
        }
        Set<Long> assignedTicketIds = new HashSet<>();
        List<Ticket> assignedTickets = new ArrayList<>();
        List<Ticket> openTickets = new ArrayList<>();
        List<Ticket> closedTickets = new ArrayList<>();
        List<Ticket> assignedToUser = Ticket
                .find("select distinct t from Ticket t join t.supportUsers u where u = ?1", user).list();
        for (Ticket ticket : assignedToUser) {
            if (ticket.id != null) {
                assignedTicketIds.add(ticket.id);
            }
        }
        for (Ticket ticket : tickets) {
            boolean assignedToCurrent = assignedTicketIds.contains(ticket.id);
            boolean hasSupport = supportAssignments.containsKey(ticket.id);
            boolean isClosed = "Closed".equalsIgnoreCase(ticket.status);
            if (assignedToCurrent && !isClosed) {
                assignedTickets.add(normalizeOpenAssigned(ticket));
            }
            if (!hasSupport) {
                openTickets.add(ticket);
            }
            if (assignedToCurrent && isClosed) {
                closedTickets.add(copyTicketDisplay(ticket));
            }
        }
        for (Ticket ticket : closedTickets) {
            if (ticket != null && ticket.id != null && !isEntitlementExpired(ticket)) {
                slaColors.put(ticket.id, "White");
            }
        }
        sortBySla(assignedTickets, slaColors, messageDates);
        sortBySla(openTickets, slaColors, messageDates);
        sortBySla(closedTickets, slaColors, messageDates);
        return new SupportTicketData(assignedTickets, openTickets, closedTickets, messageDates, messageDateLabels,
                messageDirectionArrows, slaColors, supportAssignments, supportAssignmentNames, supportAssignmentIds,
                supportAssignmentUsers, assignedTicketIds);
    }

    private static Ticket normalizeOpenAssigned(Ticket ticket) {
        if (ticket == null || !"Open".equalsIgnoreCase(ticket.status)) {
            return ticket;
        }
        Ticket displayTicket = new Ticket();
        displayTicket.id = ticket.id;
        displayTicket.name = ticket.name;
        displayTicket.status = "Assigned";
        displayTicket.company = ticket.company;
        displayTicket.companyEntitlement = ticket.companyEntitlement;
        displayTicket.affectsVersion = ticket.affectsVersion;
        displayTicket.resolvedVersion = ticket.resolvedVersion;
        displayTicket.category = ticket.category;
        displayTicket.externalIssueLink = ticket.externalIssueLink;
        return displayTicket;
    }

    private static Ticket copyTicketDisplay(Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        Ticket displayTicket = new Ticket();
        displayTicket.id = ticket.id;
        displayTicket.name = ticket.name;
        displayTicket.status = ticket.status;
        displayTicket.company = ticket.company;
        displayTicket.companyEntitlement = ticket.companyEntitlement;
        displayTicket.affectsVersion = ticket.affectsVersion;
        displayTicket.resolvedVersion = ticket.resolvedVersion;
        displayTicket.category = ticket.category;
        displayTicket.externalIssueLink = ticket.externalIssueLink;
        return displayTicket;
    }

    private static String messageDirectionArrow(User author) {
        if (author != null && User.TYPE_SUPPORT.equalsIgnoreCase(author.type)) {
            return "\u2190";
        }
        return "\u2192";
    }

    private static String resolveSlaColor(ai.mnemosyne_systems.model.Level level, long minutes) {
        if (level == null || level.level == null || level.color == null || level.color.isBlank()) {
            return null;
        }
        if (minutes >= level.level.longValue()) {
            return level.color;
        }
        return "White";
    }

    private static void sortBySla(List<Ticket> tickets, Map<Long, String> slaColors,
            Map<Long, LocalDateTime> messageDates) {
        tickets.sort((left, right) -> {
            int leftRank = slaColorRank(slaColors.get(left.id));
            int rightRank = slaColorRank(slaColors.get(right.id));
            if (leftRank != rightRank) {
                return Integer.compare(leftRank, rightRank);
            }
            LocalDateTime leftDate = messageDates.get(left.id);
            LocalDateTime rightDate = messageDates.get(right.id);
            if (leftDate == null && rightDate == null) {
                return 0;
            }
            if (leftDate == null) {
                return 1;
            }
            if (rightDate == null) {
                return -1;
            }
            int dateCompare = rightDate.compareTo(leftDate);
            if (dateCompare != 0) {
                return dateCompare;
            }
            if (left.id == null && right.id == null) {
                return 0;
            }
            if (left.id == null) {
                return 1;
            }
            if (right.id == null) {
                return -1;
            }
            return left.id.compareTo(right.id);
        });
    }

    private static int slaColorRank(String color) {
        if (color == null) {
            return 3;
        }
        String normalized = color.trim().toLowerCase(Locale.ENGLISH);
        if ("red".equals(normalized)) {
            return 0;
        }
        if ("yellow".equals(normalized)) {
            return 1;
        }
        if ("white".equals(normalized)) {
            return 2;
        }
        return 3;
    }

    record SupportTicketCounts(int assignedCount, int openCount) {
    }

    record SupportTicketData(List<Ticket> assignedTickets, List<Ticket> openTickets, List<Ticket> closedTickets,
            Map<Long, LocalDateTime> messageDates, Map<Long, String> messageDateLabels,
            Map<Long, String> messageDirectionArrows, Map<Long, String> slaColors, Map<Long, String> supportAssignments,
            Map<Long, String> supportAssignmentNames, Map<Long, Long> supportAssignmentIds,
            Map<Long, User> supportAssignmentUsers, Set<Long> assignedTicketIds) {
    }
}
