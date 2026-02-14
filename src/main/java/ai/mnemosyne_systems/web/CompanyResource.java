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
import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Entitlement;
import ai.mnemosyne_systems.model.SupportLevel;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
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
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("/companies")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class CompanyResource {

    @Location("company/companies.html")
    Template companiesTemplate;

    @Location("company/company-form.html")
    Template companyFormTemplate;

    @Location("company/company-view.html")
    Template companyViewTemplate;

    @GET
    public TemplateInstance listCompanies(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        return companiesTemplate.data("companies", Company.listAll()).data("currentUser", user);
    }

    @GET
    @Path("create")
    public TemplateInstance createForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        Company company = new Company();
        java.util.List<Country> countries = Country.list("order by name");
        return companyFormTemplate.data("company", company).data("users", User.list("type", User.TYPE_USER))
                .data("tams", User.list("type", User.TYPE_TAM)).data("entitlements", Entitlement.listAll())
                .data("supportLevels", SupportLevel.listAll()).data("companyEntitlements", java.util.List.of())
                .data("selectedEntitlementLevels", java.util.Map.of()).data("selectedUserIds", java.util.List.of())
                .data("selectedTamIds", java.util.List.of()).data("countries", countries)
                .data("timezones", java.util.List.of()).data("action", "/companies").data("title", "New company")
                .data("currentUser", user);
    }

    @GET
    @Path("{id}/edit")
    public TemplateInstance editForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireAdmin(auth);
        Company company = Company.findById(id);
        if (company == null) {
            throw new NotFoundException();
        }
        java.util.List<User> companyUsers = Company.find("select u from Company c join c.users u where c.id = ?1", id)
                .list();
        java.util.List<CompanyEntitlement> companyEntitlements = CompanyEntitlement.find(
                "select distinct ce from CompanyEntitlement ce join fetch ce.entitlement join fetch ce.supportLevel where ce.company.id = ?1",
                id).list();
        java.util.List<Long> selectedUserIds = companyUsers.stream()
                .filter(selected -> User.TYPE_USER.equalsIgnoreCase(selected.type)).map(selected -> selected.id)
                .toList();
        java.util.List<Long> selectedTamIds = companyUsers.stream()
                .filter(selected -> User.TYPE_TAM.equalsIgnoreCase(selected.type)).map(selected -> selected.id)
                .toList();
        java.util.Map<Long, Long> selectedEntitlementLevels = new java.util.HashMap<>();
        for (CompanyEntitlement entry : companyEntitlements) {
            if (entry.entitlement != null && entry.supportLevel != null) {
                selectedEntitlementLevels.put(entry.entitlement.id, entry.supportLevel.id);
            }
        }
        java.util.List<Country> countries = Country.list("order by name");
        java.util.List<Timezone> timezones = company.country != null
                ? Timezone.list("country = ?1 order by name", company.country) : java.util.List.of();
        return companyFormTemplate.data("company", company).data("users", User.list("type", User.TYPE_USER))
                .data("tams", User.list("type", User.TYPE_TAM)).data("entitlements", Entitlement.listAll())
                .data("supportLevels", SupportLevel.listAll()).data("companyEntitlements", companyEntitlements)
                .data("selectedEntitlementLevels", selectedEntitlementLevels).data("selectedUserIds", selectedUserIds)
                .data("selectedTamIds", selectedTamIds).data("countries", countries).data("timezones", timezones)
                .data("action", "/companies/" + id).data("title", "Edit Company").data("currentUser", user);
    }

    @GET
    @Path("{id}")
    public TemplateInstance viewCompany(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireAdmin(auth);
        Company company = Company.find("select distinct c from Company c left join fetch c.users where c.id = ?1", id)
                .firstResult();
        if (company == null) {
            throw new NotFoundException();
        }
        java.util.List<User> users = Company
                .find("select u from Company c join c.users u where c = ?1 and u.type = ?2 order by u.name", company,
                        User.TYPE_USER)
                .list();
        java.util.List<User> tamUsers = Company
                .find("select u from Company c join c.users u where c = ?1 and u.type = ?2 order by u.name", company,
                        User.TYPE_TAM)
                .list();
        java.util.List<CompanyEntitlement> companyEntitlements = CompanyEntitlement.find(
                "select distinct ce from CompanyEntitlement ce join fetch ce.entitlement join fetch ce.supportLevel where ce.company = ?1",
                company).list();
        return companyViewTemplate.data("company", company).data("companyUsers", users).data("companyTams", tamUsers)
                .data("companyEntitlements", companyEntitlements).data("currentUser", user);
    }

    @POST
    @Path("")
    @Transactional
    public Response createCompany(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @FormParam("name") String name,
            @FormParam("address1") String address1, @FormParam("address2") String address2,
            @FormParam("city") String city, @FormParam("state") String state, @FormParam("zip") String zip,
            @FormParam("countryId") Long countryId, @FormParam("timezoneId") Long timezoneId,
            @FormParam("userIds") java.util.List<Long> userIds, @FormParam("tamIds") java.util.List<Long> tamIds,
            @FormParam("entitlementIds") java.util.List<Long> entitlementIds,
            @FormParam("supportLevelIds") java.util.List<Long> supportLevelIds) {
        requireAdmin(auth);
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
        company.users = resolveUsers(userIds, tamIds);
        company.persist();
        applyEntitlements(company, entitlementIds, supportLevelIds, java.util.List.of());
        return Response.seeOther(URI.create("/companies")).build();
    }

    @POST
    @Path("{id}")
    @Transactional
    public Response updateCompany(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            @FormParam("name") String name, @FormParam("address1") String address1,
            @FormParam("address2") String address2, @FormParam("city") String city, @FormParam("state") String state,
            @FormParam("zip") String zip, @FormParam("countryId") Long countryId,
            @FormParam("timezoneId") Long timezoneId, @FormParam("userIds") java.util.List<Long> userIds,
            @FormParam("tamIds") java.util.List<Long> tamIds,
            @FormParam("entitlementIds") java.util.List<Long> entitlementIds,
            @FormParam("supportLevelIds") java.util.List<Long> supportLevelIds) {
        requireAdmin(auth);
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
        company.users.clear();
        company.users.addAll(resolveUsers(userIds, tamIds));
        java.util.List<CompanyEntitlement> existingEntitlements = CompanyEntitlement.find("company = ?1", company)
                .list();
        java.util.Set<Long> selectedEntitlementIds = applyEntitlements(company, entitlementIds, supportLevelIds,
                existingEntitlements);
        for (CompanyEntitlement entry : existingEntitlements) {
            if (entry.entitlement == null) {
                continue;
            }
            if (selectedEntitlementIds.contains(entry.entitlement.id)) {
                continue;
            }
            if (Ticket.count("companyEntitlement", entry) > 0) {
                continue;
            }
            entry.delete();
        }
        return Response.seeOther(URI.create("/companies")).build();
    }

    @POST
    @Path("{id}/delete")
    @Transactional
    public Response deleteCompany(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireAdmin(auth);
        Company company = Company.findById(id);
        if (company == null) {
            throw new NotFoundException();
        }
        company.delete();
        return Response.seeOther(URI.create("/companies")).build();
    }

    private User requireAdmin(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isAdmin(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }

    private java.util.List<User> resolveUsers(java.util.List<Long> userIds, java.util.List<Long> tamIds) {
        java.util.List<Long> ids = new java.util.ArrayList<>();
        if (userIds != null) {
            ids.addAll(userIds);
        }
        if (tamIds != null) {
            ids.addAll(tamIds);
        }
        if (ids.isEmpty()) {
            return java.util.List.of();
        }
        return User.list("id in ?1 and type in ?2", ids, java.util.List.of(User.TYPE_USER, User.TYPE_TAM));
    }

    private java.util.Set<Long> applyEntitlements(Company company, java.util.List<Long> entitlementIds,
            java.util.List<Long> supportLevelIds, java.util.List<CompanyEntitlement> existingEntitlements) {
        java.util.Set<Long> selectedEntitlementIds = new java.util.HashSet<>();
        if (entitlementIds == null || supportLevelIds == null) {
            return selectedEntitlementIds;
        }
        java.util.Map<Long, CompanyEntitlement> byEntitlement = new java.util.HashMap<>();
        for (CompanyEntitlement entry : existingEntitlements) {
            if (entry.entitlement != null) {
                byEntitlement.put(entry.entitlement.id, entry);
            }
        }
        int count = Math.min(entitlementIds.size(), supportLevelIds.size());
        for (int index = 0; index < count; index++) {
            Long entitlementId = entitlementIds.get(index);
            Long supportLevelId = supportLevelIds.get(index);
            if (entitlementId == null || supportLevelId == null) {
                continue;
            }
            Entitlement entitlement = Entitlement.findById(entitlementId);
            SupportLevel supportLevel = SupportLevel.findById(supportLevelId);
            if (entitlement == null || supportLevel == null) {
                continue;
            }
            selectedEntitlementIds.add(entitlement.id);
            CompanyEntitlement entry = byEntitlement.get(entitlement.id);
            if (entry == null) {
                entry = new CompanyEntitlement();
                entry.company = company;
            }
            entry.entitlement = entitlement;
            entry.supportLevel = supportLevel;
            entry.persist();
        }
        return selectedEntitlementIds;
    }
}
