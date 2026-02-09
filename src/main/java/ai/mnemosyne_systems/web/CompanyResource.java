/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.Company;
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
import java.util.List;

@Path("/companies")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class CompanyResource {

    @Location("companies/list.html")
    Template listTemplate;

    @Location("companies/form.html")
    Template formTemplate;

    @GET
    public TemplateInstance list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        return listTemplate.data("companies", Company.listAll()).data("currentUser", user);
    }

    @GET
    @Path("/new")
    public TemplateInstance createForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        Company company = new Company();
        company.country = "United States of America";
        return formTemplate.data("company", company)
                .data("users", User.list("type in ?1", List.of(User.TYPE_USER, User.TYPE_TAM)))
                .data("action", "/companies").data("title", "New company").data("currentUser", user);
    }

    @GET
    @Path("/{id}/edit")
    public TemplateInstance editForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireAdmin(auth);
        Company company = Company.findById(id);
        if (company == null) {
            throw new NotFoundException();
        }
        return formTemplate.data("company", company)
                .data("users", User.list("type in ?1", List.of(User.TYPE_USER, User.TYPE_TAM)))
                .data("action", "/companies/" + id).data("title", "Edit Company").data("currentUser", user);
    }

    @POST
    @Transactional
    public Response create(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @FormParam("name") String name,
            @FormParam("address1") String address1, @FormParam("address2") String address2,
            @FormParam("city") String city, @FormParam("state") String state, @FormParam("zip") String zip,
            @FormParam("country") String country, @FormParam("userIds") List<Long> userIds) {
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
        company.country = country;
        company.users = resolveUsers(userIds);
        company.persist();
        return Response.seeOther(URI.create("/companies")).build();
    }

    @POST
    @Path("/{id}")
    @Transactional
    public Response update(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            @FormParam("name") String name, @FormParam("address1") String address1,
            @FormParam("address2") String address2, @FormParam("city") String city, @FormParam("state") String state,
            @FormParam("zip") String zip, @FormParam("country") String country,
            @FormParam("userIds") List<Long> userIds) {
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
        company.country = country;
        company.users.clear();
        company.users.addAll(resolveUsers(userIds));
        return Response.seeOther(URI.create("/companies")).build();
    }

    @POST
    @Path("/{id}/delete")
    @Transactional
    public Response delete(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
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

    private List<User> resolveUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return User.list("id in ?1 and type in ?2", userIds, List.of(User.TYPE_USER, User.TYPE_TAM));
    }
}
