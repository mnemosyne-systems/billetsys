/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Attachment;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Path("/attachments")
@Produces(MediaType.TEXT_HTML)
@Blocking
public class AttachmentResource {

    @Location("attachments/view.html")
    Template viewTemplate;

    @GET
    @Path("/{id}/data")
    @Produces("*/*")
    public Response data(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = AuthHelper.findUser(auth);
        if (user == null) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        Attachment attachment = Attachment.findById(id);
        if (attachment == null) {
            throw new NotFoundException();
        }
        String encoded = URLEncoder.encode(attachment.name, java.nio.charset.StandardCharsets.UTF_8).replace("+",
                "%20");
        return Response.ok(attachment.data, attachment.mimeType)
                .header("Content-Disposition", "inline; filename*=UTF-8''" + encoded).build();
    }

    @GET
    @Path("/{id}")
    public TemplateInstance view(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = AuthHelper.findUser(auth);
        if (user == null) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        Attachment attachment = Attachment.findById(id);
        if (attachment == null) {
            throw new NotFoundException();
        }
        boolean isImage = attachment.isImage();
        String imageData = null;
        List<AttachmentLine> lines = List.of();
        if (isImage) {
            imageData = "data:" + attachment.mimeType + ";base64,"
                    + Base64.getEncoder().encodeToString(attachment.data);
        } else {
            String text = attachment.data == null ? "" : new String(attachment.data, StandardCharsets.UTF_8);
            String[] split = text.split("\\R", -1);
            List<AttachmentLine> entries = new ArrayList<>();
            for (int i = 0; i < split.length; i++) {
                entries.add(new AttachmentLine(i + 1, split[i]));
            }
            lines = entries;
        }
        TicketCounts counts = loadCountsFor(user);
        return viewTemplate.data("attachment", attachment).data("isImage", isImage).data("imageData", imageData)
                .data("lines", lines).data("currentUser", user).data("assignedCount", counts.assignedCount)
                .data("openCount", counts.openCount).data("ticketsBase", counts.ticketsBase)
                .data("usersBase", counts.usersBase).data("showSupportUsers", counts.showSupportUsers);
    }

    public static class AttachmentLine {

        public final int number;
        public final String content;

        AttachmentLine(int number, String content) {
            this.number = number;
            this.content = content;
        }
    }

    private TicketCounts loadCountsFor(User user) {
        if (user == null) {
            return new TicketCounts(0, 0, "/user/tickets", "/user/users", false);
        }
        if (AuthHelper.isSupport(user)) {
            SupportResource.SupportTicketCounts counts = SupportResource.loadTicketCounts(user);
            return new TicketCounts(counts.assignedCount, counts.openCount, "/support", "/support/users", true);
        }
        if (User.TYPE_SUPERUSER.equalsIgnoreCase(user.type)) {
            SupportResource.SupportTicketCounts counts = SuperuserResource.loadTicketCounts(user);
            return new TicketCounts(counts.assignedCount, counts.openCount, "/superuser/tickets", "/superuser/users",
                    true);
        }
        if (User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            List<Ticket> tickets = Ticket.list(
                    "select distinct t from Ticket t left join t.tamUsers tu left join t.company c left join c.users cu where tu = ?1 or cu = ?1",
                    user);
            TicketCounts counts = buildTicketCounts(tickets);
            return new TicketCounts(counts.assignedCount, counts.openCount, "/user/tickets", "/tam/users", true);
        }
        List<Ticket> tickets = Ticket.list("requester = ?1", user);
        TicketCounts counts = buildTicketCounts(tickets);
        return new TicketCounts(counts.assignedCount, counts.openCount, "/user/tickets", "/user/users", false);
    }

    private TicketCounts buildTicketCounts(List<Ticket> tickets) {
        int assigned = 0;
        int open = 0;
        List<Ticket> scopedTickets = tickets == null ? List.of() : tickets;
        for (Ticket ticket : scopedTickets) {
            if ("Closed".equalsIgnoreCase(ticket.status)) {
                continue;
            }
            Long hasSupport = Ticket.count("select count(u) from Ticket t join t.supportUsers u where t = ?1", ticket);
            if (hasSupport != null && hasSupport > 0) {
                assigned++;
            } else {
                open++;
            }
        }
        return new TicketCounts(assigned, open, "/user/tickets", "/user/users", false);
    }

    private static class TicketCounts {
        final int assignedCount;
        final int openCount;
        final String ticketsBase;
        final String usersBase;
        final boolean showSupportUsers;

        TicketCounts(int assignedCount, int openCount, String ticketsBase, String usersBase, boolean showSupportUsers) {
            this.assignedCount = assignedCount;
            this.openCount = openCount;
            this.ticketsBase = ticketsBase;
            this.usersBase = usersBase;
            this.showSupportUsers = showSupportUsers;
        }
    }
}
