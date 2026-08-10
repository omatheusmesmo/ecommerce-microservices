package com.ecommerce.resource;

import com.ecommerce.dto.AddressRequest;
import com.ecommerce.dto.AddressResponse;
import com.ecommerce.service.AddressService;
import com.ecommerce.service.AuthService;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

@Path("/users/me/addresses")
@ApplicationScoped
@Authenticated
public class AddressResource {

    private static final Logger LOG = Logger.getLogger(AddressResource.class);

    @Inject
    AddressService addressService;

    @Inject
    AuthService authService;

    @Inject
    JsonWebToken jwt;

    @GET
    public Response listAddresses() {
        List<AddressResponse> addresses = addressService.listForUser(currentUserId());
        return Response.ok(addresses).build();
    }

    @GET
    @Path("/{addressId}")
    public Response getAddress(@PathParam("addressId") Long addressId) {
        AddressResponse address = addressService.getById(currentUserId(), addressId);
        return Response.ok(address).build();
    }

    @POST
    public Response createAddress(@Valid AddressRequest request) {
        AddressResponse address = addressService.create(currentUserId(), request);
        LOG.infof("Address created: %d", address.id());
        return Response.status(Response.Status.CREATED).entity(address).build();
    }

    @PUT
    @Path("/{addressId}")
    public Response updateAddress(@PathParam("addressId") Long addressId, @Valid AddressRequest request) {
        AddressResponse address = addressService.update(currentUserId(), addressId, request);
        return Response.ok(address).build();
    }

    @DELETE
    @Path("/{addressId}")
    public Response deleteAddress(@PathParam("addressId") Long addressId) {
        addressService.delete(currentUserId(), addressId);
        return Response.noContent().build();
    }

    private Long currentUserId() {
        return authService.getUserIdFromToken(jwt.getRawToken());
    }
}
