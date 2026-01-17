package com.ecommerce.resource;

import com.ecommerce.entity.Product;
import com.ecommerce.service.ProductService;
import jakarta.inject.Inject;
import jakarta. ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs. core.Response;
import org. jboss.logging.Logger;

import java. util.List;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    private static final Logger LOG = Logger.getLogger(ProductResource. class);

    @Inject
    ProductService productService;

    @GET
    public List<Product> findAll() {
        LOG.debug("GET /products - Listing all products");
        return productService.findAll();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") String id) {
        LOG.debugf("GET /products/%s - Finding product by ID", id);
        Product product = productService.findById(id);
        if (product == null) {
            LOG.warnf("Product %s not found", id);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response. ok(product).build();
    }

    @GET
    @Path("/category/{category}")
    public List<Product> findByCategory(@PathParam("category") String category) {
        LOG.debugf("GET /products/category/%s", category);
        return productService. findByCategory(category);
    }

    @GET
    @Path("/active")
    public List<Product> findActiveProducts() {
        LOG.debug("GET /products/active");
        return productService.findActiveProducts();
    }

    @POST
    public Response create(Product product) {
        LOG.infof("POST /products - Creating product: %s", product.name);
        Product created = productService.create(product);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, Product product) {
        LOG.infof("PUT /products/%s - Updating product", id);
        Product updated = productService.update(id, product);
        if (updated == null) {
            return Response.status(Response.Status. NOT_FOUND).build();
        }
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        LOG.infof("DELETE /products/%s", id);
        boolean deleted = productService.delete(id);
        if (! deleted) {
            return Response. status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}