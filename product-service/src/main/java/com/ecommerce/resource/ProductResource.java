package com.ecommerce.resource;

import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.service.ProductService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.NoSuchElementException;

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
            throw new NoSuchElementException("Product " + id + " was not found");
        }
        return Response.ok(product).build();
    }

    @GET
    @Path("/category/{categoryId}")
    public List<Product> findByCategory(
            @PathParam("categoryId") String categoryId,
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
        return productService.findByCategory(categoryId, page, size);
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
            throw new NoSuchElementException("Product " + id + " was not found");
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

    @GET
    @Path("/{id}/variants")
    public List<ProductVariant> findVariants(@PathParam("id") String id) {
        return productService.findVariants(id);
    }

    @POST
    @Path("/{id}/variants")
    @RolesAllowed({"ADMIN", "SELLER"})
    public Response addVariant(@PathParam("id") String id, @Valid ProductVariant variant) {
        LOG.infof("POST /products/%s/variants - Adding variant: %s", id, variant.sku());
        ProductVariant created = productService.addVariant(id, variant);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}/variants/{sku}")
    @RolesAllowed({"ADMIN", "SELLER"})
    public Response updateVariant(@PathParam("id") String id, @PathParam("sku") String sku, @Valid ProductVariant variant) {
        LOG.infof("PUT /products/%s/variants/%s - Updating variant", id, sku);
        ProductVariant updated = productService.updateVariant(id, sku, variant);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}/variants/{sku}")
    @RolesAllowed("ADMIN")
    public Response removeVariant(@PathParam("id") String id, @PathParam("sku") String sku) {
        LOG.infof("DELETE /products/%s/variants/%s", id, sku);
        boolean removed = productService.removeVariant(id, sku);
        if (!removed) {
            throw new NoSuchElementException("Variant " + sku + " was not found");
        }
        return Response.noContent().build();
    }
}
