/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.model.Installation;
import ai.mnemosyne_systems.model.Ticket;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TicketAutoCloseService {

    private static final Logger LOGGER = Logger.getLogger(TicketAutoCloseService.class);
    private static final int DEFAULT_AUTO_CLOSE_DAYS = 7;

    @Inject
    TicketEmailService ticketEmailService;

    @Inject
    ReportService reportService;

    // Run once a day at a DST-independent time so auto-close notifications are never skipped or duplicated.
    @Scheduled(cron = "0 0 2 * * ?", timeZone = "${ticket.scheduler.timezone}")
    @Transactional
    public void autoCloseTickets() {
        int autoCloseDays = resolveAutoCloseDays();

        if (autoCloseDays <= 0) {
            LOGGER.info("Ticket auto-close is disabled (ticketAutoCloseDays = 0).");
            return;
        }

        LocalDateTime threshold = LocalDateTime.now().minusDays(autoCloseDays);

        // Find resolved tickets with no rating where resolvedAt exceeds threshold or is missing (legacy rows)
        java.util.List<Ticket> ticketsToClose = Ticket
                .find("lower(status) = 'resolved' and rating is null and " + "(resolvedAt is null or resolvedAt < ?1)",
                        threshold)
                .list();

        for (Ticket ticket : ticketsToClose) {
            LOGGER.infof("Auto-closing ticket %d (%s) as it has been resolved for %d days with no rating.", ticket.id,
                    ticket.name, autoCloseDays);
            String previousStatus = ticketEmailService.computeEffectiveStatus(ticket, ticket.status);
            ticket.rating = -1;
            ticket.status = "Closed";
            ticketEmailService.notifyStatusChange(ticket, previousStatus, null);
        }
        if (!ticketsToClose.isEmpty()) {
            // Accuracy note: there is no staleness gap here. The nightly auto-close bypasses EventService (which is
            // what normally evicts report snapshots), so it evicts the whole report-snapshots cache explicitly on
            // every run that closes tickets — staleness after auto-close is near-zero, not TTL-bound. The eviction
            // is deliberately coarse (whole cache, not per-company) because report keys embed user scopes (see
            // ReportService.computeReport), making targeted invalidation infeasible. Do not "optimize" this into
            // targeted invalidation without rethinking the key design first.
            reportService.invalidateAll();
        }
    }

    private int resolveAutoCloseDays() {
        Installation installation = Installation.find("singletonKey", "installation").firstResult();
        if (installation != null && installation.ticketAutoCloseDays != null) {
            return installation.ticketAutoCloseDays;
        }
        return DEFAULT_AUTO_CLOSE_DAYS;
    }
}
