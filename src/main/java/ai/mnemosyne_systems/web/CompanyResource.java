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
import ai.mnemosyne_systems.model.Level;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.*;
import io.quarkus.elytron.security.common.BcryptUtil;
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
import java.util.ArrayList;
import java.util.List;

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
    @Path("/create")
    public TemplateInstance createForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        Company company = new Company();
        User primaryContact = new User();
        List<Country> countries = Country.list("order by name");
        return companyFormTemplate.data("company", company).data("users", User.list("type", User.TYPE_USER))
                .data("tams", User.list("type", User.TYPE_TAM)).data("entitlements", Entitlement.listAll())
                .data("supportLevels", Level.listAll()).data("companyEntitlements", java.util.List.of())
                .data("selectedEntitlementLevels", java.util.Map.of()).data("selectedUserIds", java.util.List.of())
                .data("selectedTamIds", java.util.List.of()).data("countries", countries)
                .data("timezones", java.util.List.of()).data("action", "/companies").data("title", "New company")
                .data("currentUser", user).data("primaryContact", primaryContact);
    }

    @GET
    @Path("{id}/edit")
    public TemplateInstance editForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireAdmin(auth);
        Company company = Company.findById(id);
        if (company == null) {
            throw new NotFoundException();
        }
        User primaryContact = company.primaryContact;
        if (primaryContact == null) {
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
                .data("supportLevels", Level.listAll()).data("companyEntitlements", companyEntitlements)
                .data("selectedEntitlementLevels", selectedEntitlementLevels).data("selectedUserIds", selectedUserIds)
                .data("selectedTamIds", selectedTamIds).data("countries", countries).data("timezones", timezones)
                .data("action", "/companies/" + id).data("title", "Edit Company").data("currentUser", user)
                .data("primaryContact", primaryContact);
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
                .data("companyEntitlements", companyEntitlements).data("currentUser", user)
                .data("action", "/companies/" + id).data("title", "Edit Company").data("currentUser", user)
                .data("primaryContact", company.primaryContact);
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
            @FormParam("levelIds") java.util.List<Long> levelIds, @FormParam("phoneNumber") String phoneNumber,
            @FormParam("primaryContactUsername") String primaryContactUsername,
            @FormParam("primaryContactPhoneNumber") String primaryContactPhoneNumber,
            @FormParam("primaryPhoneNumberExtension") String primaryPhoneNumberExtension,
            @FormParam("primaryContactEmail") String primaryContactEmail,
            @FormParam("primaryContactPassword") String primaryContactPassword,
            @FormParam("primaryContactCountry") Long primaryContactCountryId,
            @FormParam("primaryContactTimeZone") Long primaryContactTZ,
            @FormParam("primaryContactSocial") String primaryContactSocial,
            @FormParam("primaryContactFullName") String primaryContactFullName) {
        requireAdmin(auth);
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
        Company company = new Company();
        validatePrimaryContactUser(primaryContactUsername, primaryContactEmail, primaryContactPassword);
        User primaryContact = buildPrimaryContact(primaryContactUsername, primaryContactFullName, primaryContactEmail,
                primaryContactPhoneNumber, primaryPhoneNumberExtension, primaryContactSocial, primaryContactTZ,
                primaryContactPassword, primaryContactCountryId);
        primaryContact.persist();
        List<Long> userIdsModified = new ArrayList<>(userIds);
        userIdsModified.add(primaryContact.id);
        company.name = name;
        company.address1 = address1;
        company.address2 = address2;
        company.city = city;
        company.state = state;
        company.zip = zip;
        company.country = countryId != null ? Country.findById(countryId) : null;
        company.timezone = timezoneId != null ? Timezone.findById(timezoneId) : null;
        company.users = resolveUsers(userIdsModified, tamIds);
        company.phoneNumber = phoneNumber;
        company.primaryContact = primaryContact;
        company.persist();
        applyEntitlements(company, entitlementIds, levelIds, java.util.List.of());
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
            @FormParam("levelIds") java.util.List<Long> levelIds, @FormParam("phoneNumber") String phoneNumber,
            @FormParam("primaryContact") Long primaryContactId) {
        requireAdmin(auth);
        Company company = Company.findById(id);
        if (company == null) {
            throw new NotFoundException();
        }
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
        if (company.primaryContact == null) {
            throw new NotFoundException();
        }
        List<Long> userIdsModified = new ArrayList<>(userIds);
        if (primaryContactId == null) {
            throw new BadRequestException("Primary contact id is required");
        } else {
            if (!primaryContactId.equals(company.primaryContact.id)) {
                User updatedPrimaryContact = User.findById(primaryContactId);
                if (updatedPrimaryContact == null) {
                    throw new NotFoundException();
                }
                company.primaryContact = updatedPrimaryContact;
                userIdsModified.add(primaryContactId);
            }
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
        company.users.clear();
        company.users.addAll(resolveUsers(userIdsModified, tamIds));
        java.util.List<CompanyEntitlement> existingEntitlements = CompanyEntitlement.find("company = ?1", company)
                .list();
        java.util.Set<String> selectedEntitlementPairs = applyEntitlements(company, entitlementIds, levelIds,
                existingEntitlements);
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
        return Response.seeOther(URI.create("/companies")).build();
    }

    @GET
    @Path("{id}")
    public TemplateInstance view(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
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

    private java.util.Set<String> applyEntitlements(Company company, java.util.List<Long> entitlementIds,
            java.util.List<Long> levelIds, java.util.List<CompanyEntitlement> existingEntitlements) {
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
            String pairKey = entitlement.id + ":" + supportLevel.id;
            selectedEntitlementPairs.add(pairKey);
            CompanyEntitlement entry = byEntitlementPair.get(pairKey);
            if (entry == null) {
                entry = new CompanyEntitlement();
                entry.company = company;
            }
            entry.entitlement = entitlement;
            entry.supportLevel = supportLevel;
            entry.persist();
        }
        return selectedEntitlementPairs;
    }

    private void validatePrimaryContactUser(String username, String email, String password) {
        if (username == null || username.isEmpty()) {
            throw new BadRequestException("Primary Contact username is required");
        }
        if (email == null || email.isEmpty()) {
            throw new BadRequestException("Primary Contact email is required");
        }
        if (password == null || password.isEmpty()) {
            throw new BadRequestException("Primary Contact password is required");
        }
    }

    private User buildPrimaryContact(String username, String fullName, String email, String phoneNumber,
            String phoneExtension, String social, Long timezone, String password, Long countryId) {
        User user = new User();
        user.name = username;
        user.email = email;
        user.phoneNumber = phoneNumber != null && !phoneNumber.isBlank() ? phoneNumber : null;
        user.phoneExtension = phoneExtension != null && !phoneExtension.isBlank() ? phoneExtension : null;
        user.country = countryId != null ? Country.findById(countryId) : null;
        user.passwordHash = BcryptUtil.bcryptHash(password);
        user.social = social != null && !social.isBlank() ? social : null;
        user.timezone = timezone != null ? Timezone.findById(timezone) : null;
        user.fullName = fullName != null && !fullName.isBlank() ? fullName : null;
        user.type = User.TYPE_USER;
        return user;
    }
}
