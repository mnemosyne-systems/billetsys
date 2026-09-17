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

@Path("/api/tam/users")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("tam")
public class TamUserApiResource {

    @Inject
    CurrentUser cUser;

    @Inject
    EventService eventService;

    @GET
    @Transactional
    public UserDirectoryApiModels.DirectoryListResponse list(@QueryParam("companyId") Long companyId) {
        User currentUser = cUser.get();
        List<Company> companies = Company
                .find("select distinct c from Company c join c.users u where u = ?1 order by c.name", currentUser)
                .list();
        Company selectedCompany = selectCompany(companies, companyId);
        List<User> users = selectedCompany == null ? List.of()
                : Company.<User> find("select u from Company c join c.users u where c = ?1 order by u.name",
                        selectedCompany).list();
        String createPath = selectedCompany != null ? "/tam/users/new?companyId=" + selectedCompany.id
                : "/tam/users/new";
        return new UserDirectoryApiModels.DirectoryListResponse("Users", "",
                selectedCompany == null ? null : selectedCompany.id, false, companies.size() <= 1, createPath,
                companies.stream().map(UserDirectoryApiModels::companyOption).toList(),
                users.stream()
                        .map(user -> UserDirectoryApiModels.userReference(user, "/user/user-profiles/" + user.id, null))
                        .toList());
    }

    @GET
    @Path("/bootstrap")
    @Transactional
    public UserDirectoryApiModels.UserFormResponse bootstrap(@QueryParam("companyId") Long companyId,
            @QueryParam("countryId") Long countryId) {
        User currentUser = cUser.get();
        List<Company> companies = Company
                .find("select distinct c from Company c join c.users u where u = ?1 order by c.name", currentUser)
                .list();
        Company selectedCompany = selectCompany(companies, companyId);
        if (selectedCompany == null) {
            throw new NotFoundException();
        }
        User newUser = new User();
        newUser.type = User.TYPE_USER;
        Country selectedCountry = selectCountry(countryId);
        newUser.country = selectedCountry;
        newUser.timezone = selectedCountry == null ? null
                : Timezone.find("country = ?1 and name = ?2", selectedCountry, "America/New_York").firstResult();
        List<Country> countries = Country.list("order by name");
        List<Timezone> timezones = selectedCountry == null ? List.of()
                : Timezone.list("country = ?1 order by name", selectedCountry);
        return new UserDirectoryApiModels.UserFormResponse("New user", "/tam/users",
                "/tam/users?companyId=" + selectedCompany.id, selectedCompany.id, companies.size() <= 1, true,
                companies.stream().map(UserDirectoryApiModels::companyOption).toList(),
                countries.stream().map(UserDirectoryApiModels::countryOption).toList(),
                timezones.stream().map(UserDirectoryApiModels::timezoneOption).toList(),
                List.of(new UserDirectoryApiModels.TypeOption(User.TYPE_USER, "User"),
                        new UserDirectoryApiModels.TypeOption(User.TYPE_EXTERNAL, "External")),
                UserDirectoryApiModels.userFormData(newUser, selectedCompany.id));
    }

    @POST
    @Path("/{id}/active")
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
        boolean inScope = Company.count(
                "select count(c) from Company c join c.users current join c.users viewed where current = ?1 and viewed = ?2",
                actor, user) > 0;
        if (!inScope) {
            throw new NotFoundException();
        }
        user.active = active;
        user.persist();
        Company company = Company.<Company> find("select c from Company c join c.users u where u = ?1", user)
                .firstResult();
        eventService.record(user.id, active ? EventConstants.USER_ACTIVATED : EventConstants.USER_DEACTIVATED,
                company == null ? null : company.id, actor == null ? null : actor.id,
                active ? "User activated" : "User deactivated");
        String backPath = company != null ? "/tam/users?companyId=" + company.id : "/tam/users";
        return ReactRedirectSupport.redirect(client, backPath);
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

    private Country selectCountry(Long countryId) {
        if (countryId != null) {
            Country country = Country.findById(countryId);
            if (country != null) {
                return country;
            }
        }
        return Country.find("code", "US").firstResult();
    }
}
