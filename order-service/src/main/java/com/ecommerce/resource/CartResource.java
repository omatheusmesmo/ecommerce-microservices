package com.ecommerce.resource;

import com.ecommerce.dto.AddCartItemRequest;
import com.ecommerce.dto.CartResponse;
import com.ecommerce.dto.CreateCartRequest;
import com.ecommerce.dto.UpdateCartItemRequest;
import com.ecommerce.service.CartService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;

@Path("/carts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CartResource {

    private static final Logger LOG = Logger.getLogger(CartResource.class);

    @Inject
    CartService cartService;

    @POST
    @RolesAllowed({"CUSTOMER", "ADMIN"})
    public Response createCart(@Valid CreateCartRequest request) {
        LOG.infof("POST /carts - Creating cart for: %s", request.customerEmail());

        CartResponse response = cartService.createCart(request);

        return Response
                .status(Response.Status.CREATED)
                .entity(response)
                .build();
    }

    @GET
    @RolesAllowed("ADMIN")
    public List<CartResponse> findAll() {
        LOG.info("GET /carts - Listing all carts");
        return cartService.findAll();
    }

    @GET
    @Path("/{id}")
    @Authenticated
    public CartResponse findById(@PathParam("id") Long id) {
        LOG.infof("GET /carts/%d", id);
        return cartService.findById(id);
    }

    @GET
    @Path("/customer/{email}")
    @RolesAllowed("ADMIN")
    public CartResponse findActiveByCustomerEmail(@PathParam("email") String email) {
        LOG.infof("GET /carts/customer/%s", email);
        return cartService.findActiveByCustomerEmail(email);
    }

    @POST
    @Path("/{id}/items")
    @RolesAllowed({"CUSTOMER", "ADMIN"})
    public CartResponse addItem(@PathParam("id") Long id, @Valid AddCartItemRequest request) {
        LOG.infof("POST /carts/%d/items - Adding product: %s", id, request.productId());
        return cartService.addItem(id, request);
    }

    @PUT
    @Path("/{id}/items/{itemId}")
    @RolesAllowed({"CUSTOMER", "ADMIN"})
    public CartResponse updateItemQuantity(
            @PathParam("id") Long id,
            @PathParam("itemId") Long itemId,
            @Valid UpdateCartItemRequest request
    ) {
        LOG.infof("PUT /carts/%d/items/%d - New quantity: %d", id, itemId, request.quantity());
        return cartService.updateItemQuantity(id, itemId, request.quantity());
    }

    @DELETE
    @Path("/{id}/items/{itemId}")
    @RolesAllowed({"CUSTOMER", "ADMIN"})
    public CartResponse removeItem(@PathParam("id") Long id, @PathParam("itemId") Long itemId) {
        LOG.infof("DELETE /carts/%d/items/%d", id, itemId);
        return cartService.removeItem(id, itemId);
    }

    @PATCH
    @Path("/{id}/abandon")
    @RolesAllowed({"CUSTOMER", "ADMIN"})
    public CartResponse abandonCart(@PathParam("id") Long id) {
        LOG.infof("PATCH /carts/%d/abandon", id);

        cartService.abandonCart(id);

        return cartService.findById(id);
    }
}
