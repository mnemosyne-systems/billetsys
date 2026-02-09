/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Path("/support")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class SupportResource {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d yyyy, h.mma",
            Locale.ENGLISH);

    @Location("support/tickets.html")
    Template ticketsTemplate;

    @Location("support/ticket-form.html")
    Template ticketFormTemplate;

    @Location("support/ticket-detail.html")
    Template ticketDetailTemplate;

    @Location("support/users.html")
    Template supportUsersTemplate;

    @Location("support/user-form.html")
    Template supportUserFormTemplate;

    @GET
    public TemplateInstance listTickets(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireSupport(auth);
        SupportTicketData data = buildTicketData(user);
        return ticketsTemplate.data("tickets", data.assignedTickets).data("pageTitle", "Tickets")
                .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                .data("messageDates", data.messageDates).data("messageDateLabels", data.messageDateLabels)
                .data("slaColors", data.slaColors).data("supportAssignments", data.supportAssignments)
                .data("assignedTicketIds", data.assignedTicketIds).data("ticketsBase", "/support")
                .data("createTicketUrl", "/support/tickets/create").data("showSupportUsers", true)
                .data("currentUser", user);
    }

    @GET
    @Path("/open")
    public TemplateInstance listOpenTickets(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireSupport(auth);
        SupportTicketData data = buildTicketData(user);
        return ticketsTemplate.data("tickets", data.openTickets).data("pageTitle", "Open tickets")
                .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                .data("messageDates", data.messageDates).data("messageDateLabels", data.messageDateLabels)
                .data("slaColors", data.slaColors).data("supportAssignments", data.supportAssignments)
                .data("assignedTicketIds", data.assignedTicketIds).data("ticketsBase", "/support")
                .data("createTicketUrl", "/support/tickets/create").data("showSupportUsers", true)
                .data("currentUser", user);
    }

    @GET
    @Path("/closed")
    public TemplateInstance listClosedTickets(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireSupport(auth);
        SupportTicketData data = buildTicketData(user);
        return ticketsTemplate.data("tickets", data.closedTickets).data("pageTitle", "Closed tickets")
                .data("assignedCount", data.assignedTickets.size()).data("openCount", data.openTickets.size())
                .data("messageDates", data.messageDates).data("messageDateLabels", data.messageDateLabels)
                .data("slaColors", data.slaColors).data("supportAssignments", data.supportAssignments)
                .data("assignedTicketIds", data.assignedTicketIds).data("ticketsBase", "/support")
                .data("createTicketUrl", "/support/tickets/create").data("showSupportUsers", true)
                .data("currentUser", user);
    }

    @GET
    @Path("/users")
    public Response supportUsersRoot(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        requireSupport(auth);
        List<Company> companies = Company.list("order by name");
        if (companies.isEmpty()) {
            throw new NotFoundException();
        }
        Company company = companies.get(0);
        if (company == null || company.id == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/support/users/" + company.id)).build();
    }

    @GET
    @Path("/users/{companyId}")
    public Response listSupportUsers(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("companyId") Long companyId) {
        User currentUser = requireSupport(auth);
        SupportTicketCounts counts = loadTicketCounts(currentUser);
        List<Company> companies = Company.list("order by name");
        Company selectedCompany = null;
        if (companyId != null) {
            selectedCompany = Company.findById(companyId);
            if (selectedCompany == null) {
                throw new NotFoundException();
            }
        }
        if (selectedCompany == null && !companies.isEmpty()) {
            selectedCompany = companies.get(0);
        }
        List<User> users = selectedCompany == null ? List.of() : Company
                .find("select u from Company c join c.users u where c = ?1 order by u.name", selectedCompany).list();
        String createUserUrl = selectedCompany == null ? "/support/users"
                : "/support/users/" + selectedCompany.id + "/create";
        return Response.ok(supportUsersTemplate.data("users", users).data("companies", companies)
                .data("selectedCompanyId", selectedCompany == null ? null : selectedCompany.id)
                .data("selectedCompany", selectedCompany).data("showCompanySelector", true)
                .data("createUserUrl", createUserUrl).data("usersBase", "/support/users")
                .data("assignedCount", counts.assignedCount).data("openCount", counts.openCount)
                .data("ticketsBase", "/support").data("showSupportUsers", true).data("currentUser", currentUser))
                .build();
    }

    @GET
    @Path("/users/{companyId}/create")
    public TemplateInstance createSupportUserForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @PathParam("companyId") Long companyId) {
        User currentUser = requireSupport(auth);
        SupportTicketCounts counts = loadTicketCounts(currentUser);
        List<Company> companies = Company.list("order by name");
        Company selectedCompany = null;
        selectedCompany = Company.findById(companyId);
        if (selectedCompany == null) {
            throw new NotFoundException();
        }
        User newUser = new User();
        newUser.type = User.TYPE_USER;
        return supportUserFormTemplate.data("user", newUser).data("companies", companies)
                .data("selectedCompanyId", selectedCompany == null ? null : selectedCompany.id)
                .data("types", List.of(User.TYPE_USER, User.TYPE_TAM)).data("action", "/support/users")
                .data("title", "New user").data("assignedCount", counts.assignedCount)
                .data("openCount", counts.openCount).data("ticketsBase", "/support").data("showSupportUsers", true)
                .data("currentUser", currentUser);
    }

    @POST
    @Path("/users")
    @Transactional
    public Response createSupportUser(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @FormParam("name") String name,
            @FormParam("email") String email, @FormParam("password") String password, @FormParam("type") String type,
            @FormParam("companyId") Long companyId, @Context HttpServletRequest request) {
        requireSupport(auth);
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Username is required");
        }
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required");
        }
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Password is required");
        }
        if (companyId == null) {
            throw new BadRequestException("Company is required");
        }
        Company company = Company.findById(companyId);
        if (company == null) {
            throw new NotFoundException();
        }
        String normalized = normalizeType(type, Set.of(User.TYPE_USER), "Type must be user");
        User newUser = new User();
        newUser.name = name.trim();
        newUser.email = email.trim();
        newUser.type = normalized;
        newUser.passwordHash = BcryptUtil.bcryptHash(password);
        newUser.persist();
        boolean exists = company.users.stream()
                .anyMatch(existing -> existing.id != null && existing.id.equals(newUser.id));
        if (!exists) {
            company.users.add(newUser);
        }
        return Response.seeOther(URI.create("/support/users/" + company.id)).build();
    }

    @GET
    @Path("/tickets/create")
    public TemplateInstance createForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireSupport(auth);
        SupportTicketCounts counts = loadTicketCounts(user);
        Ticket ticket = new Ticket();
        return ticketFormTemplate.data("ticket", ticket).data("companies", Company.listAll())
                .data("companyEntitlements", java.util.List.of()).data("selectedCompanyEntitlementId", null)
                .data("action", "/support/tickets").data("ticketName", "").data("entitlementsBase", "/support/tickets")
                .data("assignedCount", counts.assignedCount).data("openCount", counts.openCount)
                .data("ticketsBase", "/support").data("showSupportUsers", true).data("currentUser", user);
    }

    @GET
    @Path("/tickets/{id}")
    public TemplateInstance ticketDetail(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @jakarta.ws.rs.PathParam("id") Long id) {
        User user = requireSupport(auth);
        SupportTicketCounts counts = loadTicketCounts(user);
        Ticket ticket = Ticket.findById(id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        assignCompanyTams(ticket);
        String displayStatus = ticket.status;
        if (displayStatus == null || displayStatus.isBlank()) {
            displayStatus = "Open";
        }
        boolean assignedToCurrent = Ticket
                .count("select count(t) from Ticket t join t.supportUsers u where t = ?1 and u = ?2", ticket, user) > 0;
        if (assignedToCurrent && "Open".equalsIgnoreCase(displayStatus)) {
            displayStatus = "Assigned";
        }
        java.util.List<User> supportUsers = User
                .find("select u from Ticket t join t.supportUsers u where t = ?1 order by u.email", ticket).list();
        java.util.List<User> tamUsers = User.find(
                "select distinct u from Company c join c.users u where c = ?1 and lower(u.type) = ?2 order by u.email",
                ticket.company, User.TYPE_TAM).list();
        java.util.List<User> ticketTams = User
                .find("select u from Ticket t join t.tamUsers u where t = ?1 order by u.email", ticket).list();
        if (!ticketTams.isEmpty()) {
            java.util.Set<Long> seenIds = new HashSet<>();
            for (User existing : tamUsers) {
                if (existing.id != null) {
                    seenIds.add(existing.id);
                }
            }
            for (User existing : ticketTams) {
                if (existing.id != null && !seenIds.contains(existing.id)) {
                    tamUsers.add(existing);
                }
            }
        }
        java.util.List<Message> messages = Message.list("ticket = ?1 order by date desc", ticket);
        java.util.Map<Long, String> messageLabels = new java.util.LinkedHashMap<>();
        for (Message message : messages) {
            if (message.date != null) {
                messageLabels.put(message.id, formatDate(message.date));
            }
        }
        java.util.List<CompanyEntitlement> entitlements = CompanyEntitlement.find(
                "select distinct ce from CompanyEntitlement ce join fetch ce.entitlement join fetch ce.supportLevel where ce.company = ?1",
                ticket.company).list();
        return ticketDetailTemplate.data("ticket", ticket).data("displayStatus", displayStatus)
                .data("supportUsers", supportUsers).data("tamUsers", tamUsers).data("messages", messages)
                .data("messageLabels", messageLabels).data("companies", Company.listAll())
                .data("companyEntitlements", entitlements)
                .data("selectedCompanyEntitlementId",
                        ticket.companyEntitlement == null ? null : ticket.companyEntitlement.id)
                .data("action", "/support/tickets/" + id).data("title", "Update")
                .data("assignedCount", counts.assignedCount).data("openCount", counts.openCount)
                .data("ticketsBase", "/support").data("showSupportUsers", true).data("currentUser", user);
    }

    @POST
    @Path("/tickets/{id}/messages")
    @Transactional
    public Response addMessage(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @jakarta.ws.rs.PathParam("id") Long id,
            @FormParam("body") String body) {
        User user = requireSupport(auth);
        if (body == null || body.isBlank()) {
            throw new BadRequestException("Message is required");
        }
        Ticket ticket = Ticket.findById(id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        Message message = new Message();
        message.body = body;
        message.date = LocalDateTime.now();
        message.ticket = ticket;
        message.author = user;
        message.persist();
        return Response.seeOther(URI.create("/support/tickets/" + id)).build();
    }

    @POST
    @Path("/tickets")
    @Transactional
    public Response createTicket(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @FormParam("status") String status,
            @FormParam("message") String messageBody, @FormParam("companyId") Long companyId,
            @FormParam("companyEntitlementId") Long companyEntitlementId) {
        User user = requireSupport(auth);
        if (status == null || status.isBlank()) {
            throw new BadRequestException("Status is required");
        }
        if (!"Open".equalsIgnoreCase(status)) {
            throw new BadRequestException("Status must be Open");
        }
        if (messageBody == null || messageBody.isBlank()) {
            throw new BadRequestException("Message is required");
        }
        if (companyId == null) {
            throw new BadRequestException("Company is required");
        }
        if (companyEntitlementId == null) {
            throw new BadRequestException("Entitlement is required");
        }
        Company company = Company.findById(companyId);
        if (company == null) {
            throw new NotFoundException();
        }
        CompanyEntitlement entitlement = CompanyEntitlement
                .find("company = ?1 and id = ?2", company, companyEntitlementId).firstResult();
        if (entitlement == null) {
            throw new BadRequestException("Entitlement is required");
        }
        Ticket ticket = new Ticket();
        ticket.name = Ticket.nextName(company);
        ticket.status = "Open";
        ticket.company = company;
        ticket.requester = user;
        ticket.companyEntitlement = entitlement;
        ticket.persist();
        assignCompanyTams(ticket);
        Message message = new Message();
        message.body = messageBody;
        message.date = LocalDateTime.now();
        message.ticket = ticket;
        message.author = user;
        message.persist();
        return Response.seeOther(URI.create("/support")).build();
    }

    @POST
    @Path("/tickets/{id}")
    @Transactional
    public Response updateTicket(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @jakarta.ws.rs.PathParam("id") Long id, @FormParam("status") String status,
            @FormParam("companyId") Long companyId, @FormParam("companyEntitlementId") Long companyEntitlementId) {
        User user = requireSupport(auth);
        Ticket ticket = Ticket.findById(id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        if (status == null || status.isBlank()) {
            throw new BadRequestException("Status is required");
        }
        if (companyId == null) {
            throw new BadRequestException("Company is required");
        }
        if (companyEntitlementId == null) {
            throw new BadRequestException("Entitlement is required");
        }
        Company company = Company.findById(companyId);
        if (company == null) {
            throw new NotFoundException();
        }
        CompanyEntitlement entitlement = CompanyEntitlement
                .find("company = ?1 and id = ?2", company, companyEntitlementId).firstResult();
        if (entitlement == null) {
            throw new BadRequestException("Entitlement is required");
        }
        ticket.status = status;
        ticket.company = company;
        ticket.companyEntitlement = entitlement;
        if ("Assigned".equalsIgnoreCase(status)) {
            boolean assigned = ticket.supportUsers.stream()
                    .anyMatch(existing -> existing.id != null && existing.id.equals(user.id));
            if (!assigned) {
                ticket.supportUsers.add(user);
            }
        }
        assignCompanyTams(ticket);
        return Response.seeOther(URI.create("/support/tickets/" + id)).build();
    }

    @POST
    @Path("/tickets/{id}/assign")
    @Transactional
    public Response assignTicket(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @jakarta.ws.rs.PathParam("id") Long id) {
        User user = requireSupport(auth);
        Ticket ticket = Ticket.findById(id);
        if (ticket == null) {
            throw new NotFoundException();
        }
        if (ticket.supportUsers.stream().noneMatch(existing -> existing.id != null && existing.id.equals(user.id))) {
            ticket.supportUsers.add(user);
        }
        if (ticket.status == null || ticket.status.isBlank() || "Open".equalsIgnoreCase(ticket.status)) {
            ticket.status = "Assigned";
        }
        assignCompanyTams(ticket);
        return Response.seeOther(URI.create("/support/tickets/" + id)).build();
    }

    private void assignCompanyTams(Ticket ticket) {
        if (ticket == null || ticket.company == null) {
            return;
        }
        java.util.List<User> tams = User
                .find("select u from Company c join c.users u where c = ?1 and lower(u.type) = ?2", ticket.company,
                        User.TYPE_TAM)
                .list();
        if (tams.isEmpty()) {
            return;
        }
        ticket.tamUsers.size();
        java.util.Set<Long> existingIds = new HashSet<>();
        for (User existing : ticket.tamUsers) {
            if (existing.id != null) {
                existingIds.add(existing.id);
            }
        }
        for (User tam : tams) {
            if (tam.id != null && !existingIds.contains(tam.id)) {
                ticket.tamUsers.add(tam);
            }
        }
    }

    @GET
    @Path("/tickets/company/{id}/entitlements")
    public TemplateInstance companyEntitlements(@CookieParam(AuthHelper.AUTH_COOKIE) String auth,
            @jakarta.ws.rs.PathParam("id") Long id, @QueryParam("message") String message) {
        User user = requireSupport(auth);
        SupportTicketCounts counts = loadTicketCounts(user);
        Company company = Company.findById(id);
        if (company == null) {
            throw new NotFoundException();
        }
        java.util.List<CompanyEntitlement> entitlements = CompanyEntitlement.find(
                "select distinct ce from CompanyEntitlement ce join fetch ce.entitlement join fetch ce.supportLevel where ce.company = ?1",
                company).list();
        Ticket ticket = new Ticket();
        ticket.company = company;
        ticket.name = "";
        String ticketName = Ticket.previewNextName(company);
        return ticketFormTemplate.data("ticket", ticket).data("companies", Company.listAll())
                .data("companyEntitlements", entitlements).data("selectedCompanyEntitlementId", null)
                .data("action", "/support/tickets").data("ticketName", ticketName)
                .data("assignedCount", counts.assignedCount).data("openCount", counts.openCount)
                .data("ticketsBase", "/support").data("showSupportUsers", true)
                .data("entitlementsBase", "/support/tickets").data("message", message).data("currentUser", user);
    }

    private String formatDate(LocalDateTime date) {
        String formatted = DATE_FORMATTER.format(date);
        return formatted.replace("AM", "am").replace("PM", "pm");
    }

    private String resolveSlaColor(ai.mnemosyne_systems.model.SupportLevel level, long minutes) {
        if (level.normal != null && minutes >= level.normal) {
            return level.normalColor;
        }
        if (level.escalate != null && minutes >= level.escalate) {
            return level.escalateColor;
        }
        if (level.critical != null && minutes >= level.critical) {
            return level.criticalColor;
        }
        return null;
    }

    private void sortBySla(List<Ticket> tickets, Map<Long, String> slaColors, Map<Long, LocalDateTime> messageDates) {
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

    private int slaColorRank(String color) {
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

    private SupportTicketData buildTicketData(User user) {
        List<Ticket> tickets = Ticket.listAll();
        Map<Long, LocalDateTime> messageDates = new LinkedHashMap<>();
        Map<Long, String> messageDateLabels = new LinkedHashMap<>();
        List<Message> messages = Message.find("order by date desc").list();
        for (Message message : messages) {
            if (message.ticket != null && !messageDates.containsKey(message.ticket.id)) {
                messageDates.put(message.ticket.id, message.date);
                if (message.date != null) {
                    messageDateLabels.put(message.ticket.id, formatDate(message.date));
                }
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
        for (Ticket ticket : tickets) {
            User assignedSupport = User
                    .find("select u from Ticket t join t.supportUsers u where t = ?1 order by u.id desc", ticket)
                    .firstResult();
            if (assignedSupport != null) {
                supportAssignments.put(ticket.id, assignedSupport.email);
            }
        }
        Set<Long> assignedTicketIds = new HashSet<>();
        List<Ticket> assignedTickets = new java.util.ArrayList<>();
        List<Ticket> openTickets = new java.util.ArrayList<>();
        List<Ticket> closedTickets = new java.util.ArrayList<>();
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
            if (ticket != null && ticket.id != null) {
                slaColors.put(ticket.id, "White");
            }
        }
        sortBySla(assignedTickets, slaColors, messageDates);
        sortBySla(openTickets, slaColors, messageDates);
        sortBySla(closedTickets, slaColors, messageDates);
        SupportTicketData data = new SupportTicketData();
        data.assignedTickets = assignedTickets;
        data.openTickets = openTickets;
        data.closedTickets = closedTickets;
        data.messageDates = messageDates;
        data.messageDateLabels = messageDateLabels;
        data.slaColors = slaColors;
        data.supportAssignments = supportAssignments;
        data.assignedTicketIds = assignedTicketIds;
        return data;
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

    private Ticket normalizeOpenAssigned(Ticket ticket) {
        if (ticket == null || !"Open".equalsIgnoreCase(ticket.status)) {
            return ticket;
        }
        Ticket displayTicket = new Ticket();
        displayTicket.id = ticket.id;
        displayTicket.name = ticket.name;
        displayTicket.status = "Assigned";
        displayTicket.company = ticket.company;
        displayTicket.companyEntitlement = ticket.companyEntitlement;
        return displayTicket;
    }

    private Ticket copyTicketDisplay(Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        Ticket displayTicket = new Ticket();
        displayTicket.id = ticket.id;
        displayTicket.name = ticket.name;
        displayTicket.status = ticket.status;
        displayTicket.company = ticket.company;
        displayTicket.companyEntitlement = ticket.companyEntitlement;
        return displayTicket;
    }

    private String normalizeType(String type, Set<String> allowedTypes, String errorMessage) {
        String normalized = type == null ? "" : type.trim().toLowerCase();
        if (!allowedTypes.contains(normalized)) {
            throw new BadRequestException(errorMessage);
        }
        return normalized;
    }

    static class SupportTicketCounts {
        final int assignedCount;
        final int openCount;

        SupportTicketCounts(int assignedCount, int openCount) {
            this.assignedCount = assignedCount;
            this.openCount = openCount;
        }
    }

    private static class SupportTicketData {
        private List<Ticket> assignedTickets;
        private List<Ticket> openTickets;
        private List<Ticket> closedTickets;
        private Map<Long, LocalDateTime> messageDates;
        private Map<Long, String> messageDateLabels;
        private Map<Long, String> slaColors;
        private Map<Long, String> supportAssignments;
        private Set<Long> assignedTicketIds;
    }

    private User requireSupport(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isSupport(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }
}
