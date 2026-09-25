/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.event.EventConstants;
import ai.mnemosyne_systems.service.DirectoryService;
import ai.mnemosyne_systems.service.EventService;
import ai.mnemosyne_systems.util.CurrentUser;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/support")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("support")
public class SupportUserApiResource {

    @Inject
    CurrentUser cUser;

    @Inject
    EventService eventService;

    @Inject
    DirectoryService directoryService;

    @GET
    @Path("/users")
    @Transactional
    public UserDirectoryApiModels.DirectoryListResponse list(@QueryParam("companyId") Long companyId) {
        User currentUser = cUser.get();
        SupportResource.SupportTicketCounts counts = SupportResource.loadTicketCounts(currentUser);
        List<Company> companies = Company.list("order by name");
        Company selectedCompany = selectCompany(companies, companyId);
        List<UserDirectoryApiModels.UserReference> users = selectedCompany == null ? List.of()
                : directoryService.userEntriesForCompany(selectedCompany.id).stream()
                        .map(entry -> new UserDirectoryApiModels.UserReference(entry.id(), entry.username(),
                                entry.displayName(), entry.email(), entry.type(),
                                UserDirectoryApiModels.typeLabel(entry.type()), detailPath(entry.id(), entry.type()),
                                null, entry.active()))
                        .toList();
        String createPath = selectedCompany != null ? "/support/users/new?companyId=" + selectedCompany.id
                : "/support/users/new";
        return new UserDirectoryApiModels.DirectoryListResponse("Users", "",
                selectedCompany == null ? null : selectedCompany.id, true, false, createPath,
                companies.stream().map(UserDirectoryApiModels::companyOption).toList(), users);
    }

    @GET
    @Path("/users/bootstrap")
    @Transactional
    public UserDirectoryApiModels.UserFormResponse bootstrap(@QueryParam("companyId") Long companyId,
            @QueryParam("countryId") Long countryId) {
        User currentUser = cUser.get();
        SupportResource.loadTicketCounts(currentUser);
        List<Company> companies = Company.list("order by name");
        Company selectedCompany = companyId == null ? (companies.isEmpty() ? null : companies.get(0))
                : Company.findById(companyId);
        if (selectedCompany == null) {
            throw new NotFoundException();
        }
        User newUser = new User();
        newUser.type = User.TYPE_USER;
        Country selectedCountry = selectCountry(null, countryId);
        newUser.country = selectedCountry;
        newUser.timezone = selectedCountry == null ? null
                : Timezone.find("country = ?1 and name = ?2", selectedCountry, "America/New_York").firstResult();
        List<Country> countries = Country.list("order by name");
        List<Timezone> timezones = selectedCountry == null ? List.of()
                : Timezone.list("country = ?1 order by name", selectedCountry);
        return new UserDirectoryApiModels.UserFormResponse("New user", "/support/users",
                "/support/users?companyId=" + selectedCompany.id, selectedCompany.id, false, true,
                companies.stream().map(UserDirectoryApiModels::companyOption).toList(),
                countries.stream().map(UserDirectoryApiModels::countryOption).toList(),
                timezones.stream().map(UserDirectoryApiModels::timezoneOption).toList(),
                List.of(new UserDirectoryApiModels.TypeOption(User.TYPE_USER, "User"),
                        new UserDirectoryApiModels.TypeOption(User.TYPE_TAM, "TAM"),
                        new UserDirectoryApiModels.TypeOption(User.TYPE_EXTERNAL, "External")),
                UserDirectoryApiModels.userFormData(newUser, selectedCompany.id));
    }

    @GET
    @Path("/support-users/{id}")
    @Transactional
    public UserDirectoryApiModels.UserDetailResponse supportUser(@PathParam("id") Long id) {
        return profileDetail(id, User.TYPE_SUPPORT, "/support/users");
    }

    @GET
    @Path("/tam-users/{id}")
    @Transactional
    public UserDirectoryApiModels.UserDetailResponse tamUser(@PathParam("id") Long id) {
        return profileDetail(id, User.TYPE_TAM, "/support/users");
    }

    @GET
    @Path("/superuser-users/{id}")
    @Transactional
    public UserDirectoryApiModels.UserDetailResponse superuser(@PathParam("id") Long id) {
        return profileDetail(id, User.TYPE_SUPERUSER, "/support/users");
    }

    @GET
    @Path("/user-profiles/{id}")
    @Transactional
    public UserDirectoryApiModels.UserDetailResponse userProfile(@PathParam("id") Long id) {
        return profileDetail(id, null, "/support/users");
    }

    @GET
    @Path("/companies/{id}")
    @Transactional
    public UserDirectoryApiModels.CompanyDetailResponse company(@PathParam("id") Long id) {
        Company company = Company.findById(id);
        if (company == null) {
            throw new NotFoundException();
        }
        return new UserDirectoryApiModels.CompanyDetailResponse(company.id, company.name, company.address1,
                company.address2, company.city, company.state, company.zip, company.phoneNumber,
                company.country == null ? null : company.country.name,
                company.timezone == null ? null : company.timezone.name,
                usersForCompany(company, User.TYPE_USER, "/support/user-profiles/"),
                usersForCompany(company, User.TYPE_SUPERUSER, "/support/superuser-users/"),
                usersForCompany(company, User.TYPE_TAM, "/support/tam-users/"), List.of(),
                "/support/users?companyId=" + company.id);
    }

    @POST
    @Path("/users/{id}/active")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response setActive(@PathParam("id") Long id, @HeaderParam("X-Billetsys-Client") String client,
            @FormParam("active") Boolean active) {
        User actor = cUser.get();
        if (active == null) {
            throw new BadRequestException("Active is required");
        }
        User user = User.findById(id);
        if (user == null) {
            throw new NotFoundException();
        }
        if (actor != null && actor.id != null && actor.id.equals(user.id)) {
            throw new BadRequestException("Cannot change your own active status");
        }
        if (!User.TYPE_USER.equalsIgnoreCase(user.type) && !User.TYPE_EXTERNAL.equalsIgnoreCase(user.type)) {
            throw new NotFoundException();
        }
        user.active = active;
        user.persist();
        Company company = Company.<Company> find("select c from Company c join c.users u where u = ?1", user)
                .firstResult();
        eventService.record(user.id, active ? EventConstants.USER_ACTIVATED : EventConstants.USER_DEACTIVATED,
                company == null ? null : company.id, actor == null ? null : actor.id,
                active ? "User activated" : "User deactivated");
        String backPath = company != null ? "/support/users?companyId=" + company.id : "/support/users";
        return ReactRedirectSupport.redirect(client, backPath);
    }

    private UserDirectoryApiModels.UserDetailResponse profileDetail(Long id, String expectedType, String backPath) {
        User user = User.findById(id);
        if (user == null) {
            throw new NotFoundException();
        }
        if (expectedType != null && !expectedType.equalsIgnoreCase(user.type)) {
            throw new NotFoundException();
        }
        Company company = Company.<Company> find("select c from Company c join c.users u where u = ?1", user)
                .firstResult();
        return new UserDirectoryApiModels.UserDetailResponse(user.id, user.name, user.getDisplayName(), user.fullName,
                user.email, user.social, user.phoneNumber, user.phoneExtension, user.type,
                UserDirectoryApiModels.typeLabel(user.type), user.country == null ? null : user.country.name,
                user.timezone == null ? null : user.timezone.name, user.logoBase64, company == null ? null : company.id,
                company == null ? null : company.name, company == null ? null : "/support/companies/" + company.id,
                null, null, backPath, user.active);
    }

    private List<UserDirectoryApiModels.UserReference> usersForCompany(Company company, String type, String basePath) {
        return User.<User> find(
                "select distinct u from Company c join c.users u where c = ?1 and lower(u.type) = ?2 order by u.name",
                company, type).list().stream()
                .map(user -> UserDirectoryApiModels.userReference(user, basePath + user.id, null)).toList();
    }

    private String detailPath(Long id, String type) {
        if (id == null || type == null) {
            return null;
        }
        return switch (type.toLowerCase()) {
            case User.TYPE_SUPPORT -> "/support/support-users/" + id;
            case User.TYPE_TAM -> "/support/tam-users/" + id;
            case User.TYPE_SUPERUSER -> "/support/superuser-users/" + id;
            default -> "/support/user-profiles/" + id;
        };
    }

    private Company selectCompany(List<Company> companies, Long companyId) {
        if (companies == null || companies.isEmpty()) {
            return null;
        }
        if (companyId == null) {
            return companies.get(0);
        }
        return companies.stream().filter(company -> company.id != null && company.id.equals(companyId)).findFirst()
                .orElse(companies.get(0));
    }

    private Country selectCountry(User user, Long countryId) {
        if (countryId != null) {
            Country country = Country.findById(countryId);
            if (country != null) {
                return country;
            }
        }
        if (user != null && user.country != null) {
            return user.country;
        }
        return Country.find("code", "US").firstResult();
    }

}
