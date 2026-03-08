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
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Path("/rss")
@Blocking
public class RssResource {

    private static final DateTimeFormatter RFC_1123 = DateTimeFormatter.RFC_1123_DATE_TIME;

    @GET
    @Path("/support")
    @Produces("application/rss+xml")
    public Response support(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @Context UriInfo uriInfo) {
        User user = AuthHelper.findUser(auth);
        if (user == null) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        if (!AuthHelper.isSupport(user)) {
            throw new ForbiddenException();
        }
        List<Ticket> assigned = Ticket.find(
                "select distinct t from Ticket t join t.supportUsers u where u = ?1 and (t.status is null or lower(t.status) <> 'closed')",
                user).list();
        List<Ticket> open = Ticket.find("select distinct t from Ticket t where t.supportUsers is empty").list();
        LinkedHashMap<Long, Ticket> merged = new LinkedHashMap<>();
        addTickets(merged, assigned);
        addTickets(merged, open);
        String xml = buildFeed("Support tickets feed", "Assigned + Open tickets", merged.values(), uriInfo,
                "/support/tickets/");
        return Response.ok(xml).type("application/rss+xml; charset=UTF-8").build();
    }

    @GET
    @Path("/tam")
    @Produces("application/rss+xml")
    public Response tam(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @Context UriInfo uriInfo) {
        User user = AuthHelper.findUser(auth);
        if (user == null) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        if (!User.TYPE_TAM.equalsIgnoreCase(user.type)) {
            throw new ForbiddenException();
        }
        List<Ticket> scoped = Ticket.list(
                "select distinct t from Ticket t left join t.tamUsers tu left join t.company c left join c.users cu where tu = ?1 or cu = ?1",
                user);
        LinkedHashMap<Long, Ticket> merged = new LinkedHashMap<>();
        for (Ticket ticket : scoped) {
            if (ticket == null || ticket.id == null) {
                continue;
            }
            boolean isClosed = "Closed".equalsIgnoreCase(ticket.status);
            if (isClosed) {
                continue;
            }
            merged.put(ticket.id, ticket);
        }
        String xml = buildFeed("TAM tickets feed", "Active + Open company tickets", merged.values(), uriInfo,
                "/user/tickets/");
        return Response.ok(xml).type("application/rss+xml; charset=UTF-8").build();
    }

    @GET
    @Path("/superuser")
    @Produces("application/rss+xml")
    public Response superuser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @Context UriInfo uriInfo) {
        User user = AuthHelper.findUser(auth);
        if (user == null) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        if (!User.TYPE_SUPERUSER.equalsIgnoreCase(user.type)) {
            throw new ForbiddenException();
        }
        List<Ticket> scoped = SuperuserResource.loadScopedTickets(user);
        LinkedHashMap<Long, Ticket> merged = new LinkedHashMap<>();
        for (Ticket ticket : scoped) {
            if (ticket == null || ticket.id == null || "Closed".equalsIgnoreCase(ticket.status)) {
                continue;
            }
            merged.put(ticket.id, ticket);
        }
        String xml = buildFeed("Superuser tickets feed", "Active + Open company tickets", merged.values(), uriInfo,
                "/superuser/tickets/");
        return Response.ok(xml).type("application/rss+xml; charset=UTF-8").build();
    }

    private String buildFeed(String title, String description, Collection<Ticket> tickets, UriInfo uriInfo,
            String ticketPath) {
        List<Ticket> ticketList = tickets == null ? List.of() : new ArrayList<>(tickets);
        Map<Long, Message> latestMessages = loadLatestMessages(ticketList);
        ticketList.sort((a, b) -> latestMessageDate(latestMessages, b).compareTo(latestMessageDate(latestMessages, a)));
        String channelLink = uriInfo.getBaseUri().toString();
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<rss version=\"2.0\"><channel>");
        xml.append("<title>").append(xmlEscape(title)).append("</title>");
        xml.append("<link>").append(xmlEscape(channelLink)).append("</link>");
        xml.append("<description>").append(xmlEscape(description)).append("</description>");
        xml.append("<language>en-us</language>");
        for (Ticket ticket : ticketList) {
            if (ticket == null || ticket.id == null) {
                continue;
            }
            Message latest = latestMessages.get(ticket.id);
            String link = uriInfo.getBaseUriBuilder().path(ticketPath + ticket.id).build().toString();
            String companyName = ticket.company == null ? "" : ticket.company.name;
            String summary = "Status: " + (ticket.status == null ? "" : ticket.status)
                    + (companyName.isBlank() ? "" : " | Company: " + companyName)
                    + (latest == null || latest.body == null || latest.body.isBlank() ? ""
                            : " | Last message: " + latest.body.replace('\n', ' '));
            xml.append("<item>");
            xml.append("<title>").append(xmlEscape(ticket.name)).append("</title>");
            xml.append("<link>").append(xmlEscape(link)).append("</link>");
            xml.append("<guid>").append(xmlEscape(link)).append("</guid>");
            xml.append("<description>").append(xmlEscape(summary)).append("</description>");
            if (latest != null && latest.date != null) {
                xml.append("<pubDate>").append(xmlEscape(latest.date.atZone(ZoneId.systemDefault()).format(RFC_1123)))
                        .append("</pubDate>");
            }
            xml.append("</item>");
        }
        xml.append("</channel></rss>");
        return xml.toString();
    }

    private Map<Long, Message> loadLatestMessages(List<Ticket> tickets) {
        Set<Long> ticketIds = new LinkedHashSet<>();
        for (Ticket ticket : tickets) {
            if (ticket != null && ticket.id != null) {
                ticketIds.add(ticket.id);
            }
        }
        if (ticketIds.isEmpty()) {
            return Map.of();
        }
        List<Message> messages = Message.find("ticket.id in ?1 order by date desc", ticketIds).list();
        Map<Long, Message> latest = new LinkedHashMap<>();
        for (Message message : messages) {
            if (message == null || message.ticket == null || message.ticket.id == null) {
                continue;
            }
            latest.putIfAbsent(message.ticket.id, message);
        }
        return latest;
    }

    private void addTickets(Map<Long, Ticket> merged, List<Ticket> tickets) {
        if (tickets == null) {
            return;
        }
        for (Ticket ticket : tickets) {
            if (ticket != null && ticket.id != null) {
                merged.putIfAbsent(ticket.id, ticket);
            }
        }
    }

    private java.time.LocalDateTime latestMessageDate(Map<Long, Message> latestMessages, Ticket ticket) {
        if (ticket == null || ticket.id == null) {
            return java.time.LocalDateTime.MIN;
        }
        Message latest = latestMessages.get(ticket.id);
        if (latest == null || latest.date == null) {
            return java.time.LocalDateTime.MIN;
        }
        return latest.date;
    }

    private String xmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
