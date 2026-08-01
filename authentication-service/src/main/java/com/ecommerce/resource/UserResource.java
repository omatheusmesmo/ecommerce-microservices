package com.ecommerce.resource;

import com.ecommerce.dto.UserResponse;
import com.ecommerce.entity.Role;
import com.ecommerce.service.AuthService;
import com.ecommerce.service.UserService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

@Path("/users")
@ApplicationScoped
public class UserResource {

    private static final Logger LOG = Logger.getLogger(UserResource.class);

    @Inject
    UserService userService;

    @Inject
    AuthService authService;

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("/profile")
    @Authenticated
    public Response getProfile() {
        Long userId = authService.getUserIdFromToken(jwt.getRawToken());
        UserResponse user = userService.getProfile(userId);

        return Response.ok(user).build();
    }

    @PUT
    @Path("/{userId}/promote")
    @RolesAllowed("ADMIN")
    public Response promote(@PathParam("userId") Long userId, @Valid Role newRole) {
        Long promoterId = authService.getUserIdFromToken(jwt.getRawToken());
        UserResponse user = userService.promote(userId, newRole, promoterId);
        LOG.infof("User promoted: %d to %s", userId, newRole);

        return Response.ok(user).build();
    }

    @GET
    @RolesAllowed("ADMIN")
    public Response listUsers() {
        List<UserResponse> users = userService.listAll();
        return Response.ok(users).build();
    }

    @GET
    @Path("/{userId}")
    @RolesAllowed("ADMIN")
    public Response getUser(@PathParam("userId") Long userId) {
        UserResponse user = userService.getById(userId);
        return Response.ok(user).build();
    }

    @DELETE
    @Path("/{userId}")
    @RolesAllowed("ADMIN")
    public Response deleteUser(@PathParam("userId") Long userId) {
        Long deleterId = authService.getUserIdFromToken(jwt.getRawToken());
        userService.delete(userId, deleterId);
        LOG.infof("User deleted: %d", userId);
        return Response.noContent().build();
    }
}
