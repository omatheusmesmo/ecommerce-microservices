package com.ecommerce.resource;

import com.ecommerce.entity.Product;
import com.ecommerce.service.ProductService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    private static final Logger LOG = Logger.getLogger(ProductResource.class);

    @Inject
    ProductService productService;

    @GET
    public List<Product> findAll(
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
        return productService.findAll(page, size);
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") String id) {
        Product product = productService.findById(id);
        if (product == null) {
            LOG.warnf("Product %s not found", id);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(product).build();
    }

    @GET
    @Path("/category/{category}")
    public List<Product> findByCategory(
            @PathParam("category") String category,
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
        return productService.findByCategory(category, page, size);
    }

    @GET
    @Path("/active")
    public List<Product> findActiveProducts(
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
        return productService.findActiveProducts(page, size);
    }

    @POST
    @RolesAllowed({"ADMIN", "SELLER"})
    public Response create(@Valid Product product) {
        LOG.infof("POST /products - Creating product: %s", product.name);
        Product created = productService.create(product);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "SELLER"})
    public Response update(@PathParam("id") String id, @Valid Product product) {
        LOG.infof("PUT /products/%s - Updating product", id);
        Product updated = productService.update(id, product);
        if (updated == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response delete(@PathParam("id") String id) {
        LOG.infof("DELETE /products/%s", id);
        boolean deleted = productService.delete(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}
