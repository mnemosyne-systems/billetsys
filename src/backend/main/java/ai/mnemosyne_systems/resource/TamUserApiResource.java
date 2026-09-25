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

@Path("/api/tam/users")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("tam")
public class TamUserApiResource {

    @Inject
    CurrentUser cUser;

    @Inject
    EventService eventService;

    @Inject
    DirectoryService directoryService;

    @GET
    @Transactional
    public UserDirectoryApiModels.DirectoryListResponse list(@QueryParam("companyId") Long companyId) {
        User currentUser = cUser.get();
        List<DirectoryService.CompanyOption> companies = directoryService.companyOptionsForActor(currentUser.id);
        Long selectedCompanyId = DirectoryService.selectCompanyId(companies, companyId);
        List<UserDirectoryApiModels.UserReference> users = selectedCompanyId == null ? List.of()
                : directoryService.userEntriesForCompany(selectedCompanyId).stream()
                        .map(entry -> new UserDirectoryApiModels.UserReference(entry.id(), entry.username(),
                                entry.displayName(), entry.email(), entry.type(),
                                UserDirectoryApiModels.typeLabel(entry.type()), "/user/user-profiles/" + entry.id(),
                                null, entry.active()))
                        .toList();
        String createPath = selectedCompanyId != null ? "/tam/users/new?companyId=" + selectedCompanyId
                : "/tam/users/new";
        return new UserDirectoryApiModels.DirectoryListResponse("Users", "", selectedCompanyId, false,
                companies.size() <= 1, createPath, companies.stream()
                        .map(option -> new UserDirectoryApiModels.CompanyOption(option.id(), option.name())).toList(),
                users);
    }

    @GET
    @Path("/bootstrap")
    @Transactional
    public UserDirectoryApiModels.UserFormResponse bootstrap(@QueryParam("companyId") Long companyId,
            @QueryParam("countryId") Long countryId) {
        User currentUser = cUser.get();
        List<DirectoryService.CompanyOption> companies = directoryService.companyOptionsForActor(currentUser.id);
        Long selectedCompanyId = DirectoryService.selectCompanyId(companies, companyId);
        if (selectedCompanyId == null) {
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
                "/tam/users?companyId=" + selectedCompanyId, selectedCompanyId, companies.size() <= 1, true,
                companies.stream().map(option -> new UserDirectoryApiModels.CompanyOption(option.id(), option.name()))
                        .toList(),
                countries.stream().map(UserDirectoryApiModels::countryOption).toList(),
                timezones.stream().map(UserDirectoryApiModels::timezoneOption).toList(),
                List.of(new UserDirectoryApiModels.TypeOption(User.TYPE_USER, "User"),
                        new UserDirectoryApiModels.TypeOption(User.TYPE_EXTERNAL, "External")),
                UserDirectoryApiModels.userFormData(newUser, selectedCompanyId));
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
