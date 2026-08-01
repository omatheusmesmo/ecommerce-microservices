package com.ecommerce.resource;

import com.ecommerce.entity.Category;
import com.ecommerce.service.CategoryService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.NoSuchElementException;

@Path("/categories")
public class CategoryResource {

    private static final Logger LOG = Logger.getLogger(CategoryResource.class);

    @Inject
    CategoryService categoryService;

    @GET
    public List<Category> findRoots() {
        return categoryService.findRoots();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") String id) {
        Category category = categoryService.findById(id);
        if (category == null) {
            throw new NoSuchElementException("Category " + id + " was not found");
        }
        return Response.ok(category).build();
    }

    @GET
    @Path("/{id}/children")
    public List<Category> findChildren(@PathParam("id") String id) {
        return categoryService.findChildren(id);
    }

    @GET
    @Path("/{id}/breadcrumb")
    public List<Category> findBreadcrumb(@PathParam("id") String id) {
        return categoryService.findBreadcrumb(id);
    }

    @POST
    @RolesAllowed({"ADMIN", "SELLER"})
    public Response create(@Valid Category category) {
        LOG.infof("POST /categories - Creating category: %s", category.name);
        Category created = categoryService.create(category);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "SELLER"})
    public Response update(@PathParam("id") String id, @Valid Category category) {
        LOG.infof("PUT /categories/%s - Updating category", id);
        Category updated = categoryService.update(id, category);
        if (updated == null) {
            throw new NoSuchElementException("Category " + id + " was not found");
        }
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response delete(@PathParam("id") String id) {
        LOG.infof("DELETE /categories/%s", id);
        boolean deleted = categoryService.delete(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}
