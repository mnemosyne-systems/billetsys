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
import ai.mnemosyne_systems.util.AuthHelper;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Path("/owner")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class OwnerResource {

    @Location("owner/owner-view.html")
    Template ownerViewTemplate;

    @Location("owner/owner-edit.html")
    Template ownerEditTemplate;

    @GET
    public TemplateInstance viewOwner(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        Company company = findOwnerCompany();
        List<User> supportUsers = Company
                .find("select u from Company c join c.users u where c = ?1 and u.type = ?2 order by u.name", company,
                        User.TYPE_SUPPORT)
                .list();
        List<User> tamUsers = Company
                .find("select u from Company c join c.users u where c = ?1 and u.type = ?2 order by u.name", company,
                        User.TYPE_TAM)
                .list();
        return ownerViewTemplate.data("company", company).data("supportUsers", supportUsers).data("tamUsers", tamUsers)
                .data("currentUser", user).data("title", "Owner");
    }

    @GET
    @Path("/edit")
    public TemplateInstance editOwner(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        Company company = findOwnerCompany();
        List<Country> countries = Country.list("order by name");
        List<Timezone> timezones = company.country != null
                ? Timezone.list("country = ?1 order by name", company.country)
                : List.of();
        List<User> supportUsers = User.list("type = ?1 order by name", User.TYPE_SUPPORT);
        List<User> tamUsers = User.list("type = ?1 order by name", User.TYPE_TAM);
        List<Long> selectedSupportIds = company.users.stream()
                .filter(existing -> User.TYPE_SUPPORT.equalsIgnoreCase(existing.type)).map(existing -> existing.id)
                .toList();
        List<Long> selectedTamIds = company.users.stream()
                .filter(existing -> User.TYPE_TAM.equalsIgnoreCase(existing.type)).map(existing -> existing.id)
                .toList();
        return ownerEditTemplate.data("company", company).data("countries", countries).data("timezones", timezones)
                .data("supportUsers", supportUsers).data("tamUsers", tamUsers)
                .data("selectedSupportIds", selectedSupportIds).data("selectedTamIds", selectedTamIds)
                .data("currentUser", user);
    }

    @POST
    @Path("/edit")
    @Transactional
    public Response updateOwner(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @FormParam("name") String name,
            @FormParam("address1") String address1, @FormParam("address2") String address2,
            @FormParam("city") String city, @FormParam("state") String state, @FormParam("zip") String zip,
            @FormParam("countryId") Long countryId, @FormParam("timezoneId") Long timezoneId,
            @FormParam("phoneNumber") String phoneNumber, @FormParam("supportIds") List<Long> supportIds,
            @FormParam("tamIds") List<Long> tamIds) {
        requireAdmin(auth);
        Company company = findOwnerCompany();
        if (name == null || name.isBlank()) {
            throw new jakarta.ws.rs.BadRequestException("Name is required");
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
        company.users.addAll(resolveSelectedUsers(supportIds, tamIds));
        return Response.seeOther(URI.create("/owner")).build();
    }

    private Company findOwnerCompany() {
        Company company = Company
                .find("select distinct c from Company c left join fetch c.users where lower(c.name) = lower(?1)",
                        "mnemosyne systems")
                .firstResult();
        if (company == null) {
            throw new NotFoundException();
        }
        return company;
    }

    private List<User> resolveSelectedUsers(List<Long> supportIds, List<Long> tamIds) {
        List<Long> ids = new ArrayList<>();
        if (supportIds != null) {
            ids.addAll(supportIds);
        }
        if (tamIds != null) {
            ids.addAll(tamIds);
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        return User.list("id in ?1 and type in ?2", ids, List.of(User.TYPE_SUPPORT, User.TYPE_TAM));
    }

    private User requireAdmin(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isAdmin(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }
}
