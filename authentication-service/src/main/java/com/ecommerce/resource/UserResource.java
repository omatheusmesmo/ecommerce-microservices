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
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.util.List;


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
        LOG.debugf("GET /users/profile request received");

        Long userId = authService.getUserIdFromToken(jwt.getRawToken());
        UserResponse user = userService.getProfile(userId);

        return Response.ok(user).build();
    }

    @PUT
    @Path("/{userId}/promote")
    @RolesAllowed("ADMIN")
    public Response promote(@PathParam("userId") Long userId, @Valid Role newRole) {
        LOG.debugf("PUT /users/%d/promote request received to role: %s", userId, newRole);

        Long promoterId = authService.getUserIdFromToken(jwt.getRawToken());
        UserResponse user = userService.promote(userId, newRole, promoterId);
        LOG.infof("User promoted: %d to %s", userId, newRole);

        return Response.ok(user).build();
    }

    @GET
    @RolesAllowed("ADMIN")
    public Response listUsers() {
        LOG.debugf("GET /users request received");
        List<UserResponse> users = userService.listAll();
        return Response.ok(users).build();
    }

    @GET
    @Path("/{userId}")
    @RolesAllowed("ADMIN")
    public Response getUser(@PathParam("userId") Long userId) {
        LOG.debugf("GET /users/%d request received", userId);
        UserResponse user = userService.getById(userId);
        return Response.ok(user).build();
    }

    @DELETE
    @Path("/{userId}")
    @RolesAllowed("ADMIN")
    public Response deleteUser(@PathParam("userId") Long userId) {
        LOG.debugf("DELETE /users/%d request received", userId);
        Long deleterId = authService.getUserIdFromToken(jwt.getRawToken());
        userService.delete(userId, deleterId);
        LOG.infof("User deleted: %d", userId);
        return Response.noContent().build();
    }
}