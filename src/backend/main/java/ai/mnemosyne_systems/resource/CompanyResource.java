/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Entitlement;
import ai.mnemosyne_systems.model.Level;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.util.AuthHelper;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Path("/companies")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
@RolesAllowed("admin")
public class CompanyResource {

    @GET
    public Response listCompanies() {
        return Response.seeOther(URI.create("/companies")).build();
    }

    @GET
    @Path("/create")
    public Response createForm() {
        return Response.seeOther(URI.create("/companies/new")).build();
    }

    @GET
    @Path("{id}/edit")
    public Response editForm(@PathParam("id") Long id) {
        if (Company.findById(id) == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/companies/" + id + "/edit")).build();
    }

    @GET
    @Path("{id}")
    public Response viewCompany(@PathParam("id") Long id) {
        if (Company.findById(id) == null) {
            throw new NotFoundException();
        }
        return Response.seeOther(URI.create("/companies/" + id)).build();
    }

    @POST
    @Path("")
    @Transactional
    public Response createCompany(@HeaderParam("X-Billetsys-Client") String client, @FormParam("name") String name,
            @FormParam("address1") String address1, @FormParam("address2") String address2,
            @FormParam("city") String city, @FormParam("state") String state, @FormParam("zip") String zip,
            @FormParam("countryId") Long countryId, @FormParam("timezoneId") Long timezoneId,
            @FormParam("userIds") java.util.List<Long> userIds, @FormParam("tamIds") java.util.List<Long> tamIds,
            @FormParam("entitlementIds") java.util.List<Long> entitlementIds,
            @FormParam("levelIds") java.util.List<Long> levelIds,
            @FormParam("entitlementDates") java.util.List<String> entitlementDates,
            @FormParam("entitlementDurations") java.util.List<Integer> entitlementDurations,
            @FormParam("phoneNumber") String phoneNumber) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
        Company company = new Company();
        company.name = name;
        company.address1 = address1;
        company.address2 = address2;
        company.city = city;
        company.state = state;
        company.zip = zip;
        company.country = countryId != null ? Country.findById(countryId) : null;
        company.timezone = timezoneId != null ? Timezone.findById(timezoneId) : null;
        company.users = resolveUsers(userIds, tamIds, null);
        company.phoneNumber = phoneNumber;
        company.persist();
        applyEntitlements(company, entitlementIds, levelIds, entitlementDates, entitlementDurations,
                java.util.List.of());
        return ReactRedirectSupport.redirect(client, "/companies");
    }

    @POST
    @Path("{id}")
    @Transactional
    public Response updateCompany(@PathParam("id") Long id, @HeaderParam("X-Billetsys-Client") String client,
            @FormParam("name") String name, @FormParam("address1") String address1,
            @FormParam("address2") String address2, @FormParam("city") String city, @FormParam("state") String state,
            @FormParam("zip") String zip, @FormParam("countryId") Long countryId,
            @FormParam("timezoneId") Long timezoneId, @FormParam("userIds") java.util.List<Long> userIds,
            @FormParam("tamIds") java.util.List<Long> tamIds,
            @FormParam("entitlementIds") java.util.List<Long> entitlementIds,
            @FormParam("levelIds") java.util.List<Long> levelIds,
            @FormParam("entitlementDates") java.util.List<String> entitlementDates,
            @FormParam("entitlementDurations") java.util.List<Integer> entitlementDurations,
            @FormParam("phoneNumber") String phoneNumber, @Context HttpServletRequest request) {
        Company company = Company.findById(id);
        if (company == null) {
            throw new NotFoundException();
        }
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
        company.name = name;
        company.address1 = address1;
        company.address2 = address2;
        company.city = city;
        company.state = state;
        company.zip = zip;
        company.country = countryId != null ? Country.findById(countryId) : null;
        company.timezone = timezoneId != null ? Timezone.findById(timezoneId) : null;
        company.phoneNumber = phoneNumber;
        if (request.getParameterMap().containsKey("userIds") || request.getParameterMap().containsKey("tamIds")) {
            company.users.clear();
            company.users.addAll(resolveUsers(userIds, tamIds, company.id));
        }
        java.util.List<CompanyEntitlement> existingEntitlements = CompanyEntitlement.find("company = ?1", company)
                .list();
        java.util.Set<String> selectedEntitlementPairs = applyEntitlements(company, entitlementIds, levelIds,
                entitlementDates, entitlementDurations, existingEntitlements);
        for (CompanyEntitlement entry : existingEntitlements) {
            if (entry.entitlement == null || entry.supportLevel == null) {
                continue;
            }
            String pairKey = entry.entitlement.id + ":" + entry.supportLevel.id;
            if (selectedEntitlementPairs.contains(pairKey)) {
                continue;
            }
            if (Ticket.count("companyEntitlement", entry) > 0) {
                continue;
            }
            entry.delete();
        }
        return ReactRedirectSupport.redirect(client, "/companies");
    }

    @POST
    @Path("{id}/delete")
    @Transactional
    public Response deleteCompany(@HeaderParam("X-Billetsys-Client") String client, @PathParam("id") Long id) {
        Company company = Company.findById(id);
        if (company == null) {
            throw new NotFoundException();
        }
        company.delete();
        return ReactRedirectSupport.redirect(client, "/companies");
    }

    private java.util.List<User> resolveUsers(java.util.List<Long> userIds, java.util.List<Long> tamIds,
            Long companyId) {
        java.util.LinkedHashSet<Long> ids = new java.util.LinkedHashSet<>();
        if (userIds != null) {
            ids.addAll(userIds);
        }
        if (tamIds != null) {
            ids.addAll(tamIds);
        }
        if (ids.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<User> users = User.list("id in ?1 and type in ?2", ids,
                java.util.List.of(User.TYPE_USER, User.TYPE_TAM, User.TYPE_SUPERUSER));
        for (User user : users) {
            if (user == null || user.id == null) {
                continue;
            }
            if (!User.TYPE_USER.equalsIgnoreCase(user.type) && !User.TYPE_SUPERUSER.equalsIgnoreCase(user.type)) {
                continue;
            }
            long companyCount = companyId == null
                    ? Company.find("select distinct c from Company c join c.users u where u = ?1", user).count()
                    : Company.find("select distinct c from Company c join c.users u where u = ?1 and c.id <> ?2", user,
                            companyId).count();
            if (companyCount > 0) {
                throw new BadRequestException(
                        "Selected users and superusers must not already belong to another company.");
            }
        }
        return users;
    }

    private java.util.Set<String> applyEntitlements(Company company, java.util.List<Long> entitlementIds,
            java.util.List<Long> levelIds, java.util.List<String> entitlementDates,
            java.util.List<Integer> entitlementDurations, java.util.List<CompanyEntitlement> existingEntitlements) {
        java.util.Set<String> selectedEntitlementPairs = new java.util.HashSet<>();
        if (entitlementIds == null || levelIds == null) {
            return selectedEntitlementPairs;
        }
        java.util.Map<String, CompanyEntitlement> byEntitlementPair = new java.util.HashMap<>();
        for (CompanyEntitlement entry : existingEntitlements) {
            if (entry.entitlement != null && entry.supportLevel != null) {
                byEntitlementPair.put(entry.entitlement.id + ":" + entry.supportLevel.id, entry);
            }
        }
        int count = Math.min(entitlementIds.size(), levelIds.size());
        for (int index = 0; index < count; index++) {
            Long entitlementId = entitlementIds.get(index);
            Long supportLevelId = levelIds.get(index);
            if (entitlementId == null || supportLevelId == null) {
                continue;
            }
            Entitlement entitlement = Entitlement.findById(entitlementId);
            Level supportLevel = Level.findById(supportLevelId);
            if (entitlement == null || supportLevel == null) {
                continue;
            }
            LocalDate entitlementDate = parseEntitlementDate(entitlementDates, index);
            Integer entitlementDuration = parseEntitlementDuration(entitlementDurations, index);
            for (Level levelToApply : levelsToApply(supportLevel)) {
                String pairKey = entitlement.id + ":" + levelToApply.id;
                if (selectedEntitlementPairs.contains(pairKey)) {
                    continue;
                }
                selectedEntitlementPairs.add(pairKey);
                CompanyEntitlement entry = byEntitlementPair.get(pairKey);
                if (entry == null) {
                    entry = new CompanyEntitlement();
                    entry.company = company;
                    byEntitlementPair.put(pairKey, entry);
                }
                entry.entitlement = entitlement;
                entry.supportLevel = levelToApply;
                entry.date = entitlementDate;
                entry.duration = entitlementDuration;
                entry.persist();
            }
        }
        return selectedEntitlementPairs;
    }

    private java.util.List<Level> levelsToApply(Level selectedLevel) {
        if (selectedLevel == null || selectedLevel.id == null) {
            return java.util.List.of();
        }
        java.util.List<Level> levels = new java.util.ArrayList<>();
        levels.add(selectedLevel);
        if (selectedLevel.level != null) {
            levels.addAll(Level.list("level > ?1 order by level", selectedLevel.level));
        }
        return levels;
    }

    private LocalDate parseEntitlementDate(java.util.List<String> entitlementDates, int index) {
        if (entitlementDates == null || index >= entitlementDates.size()) {
            return LocalDate.now();
        }
        String value = entitlementDates.get(index);
        if (value == null || value.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (RuntimeException ex) {
            throw new BadRequestException("Date is invalid");
        }
    }

    private Integer parseEntitlementDuration(java.util.List<Integer> entitlementDurations, int index) {
        Integer value = CompanyEntitlement.DURATION_YEARLY;
        if (entitlementDurations != null && index < entitlementDurations.size()) {
            value = entitlementDurations.get(index);
        }
        if (value == null
                || (value != CompanyEntitlement.DURATION_MONTHLY && value != CompanyEntitlement.DURATION_YEARLY)) {
            throw new BadRequestException("Duration is invalid");
        }
        return value;
    }

    private boolean isEntitlementExpired(CompanyEntitlement entitlement) {
        if (entitlement == null || entitlement.date == null || entitlement.duration == null) {
            return false;
        }
        LocalDate endDate = entitlement.date;
        if (entitlement.duration == CompanyEntitlement.DURATION_MONTHLY) {
            endDate = endDate.plusMonths(1);
        } else if (entitlement.duration == CompanyEntitlement.DURATION_YEARLY) {
            endDate = endDate.plusYears(1);
        } else {
            return false;
        }
        return LocalDate.now().isAfter(endDate);
    }

}
