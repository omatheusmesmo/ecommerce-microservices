package com.ecommerce.resource;

import com.ecommerce.dto.ActivationRequest;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.PasswordResetRequest;
import com.ecommerce.dto.PasswordResetUpdateRequest;
import com.ecommerce.dto.RefreshRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.dto.TokenResponse;
import com.ecommerce.dto.UserResponse;
import com.ecommerce.service.AuthService;
import com.ecommerce.service.UserService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private static final Logger LOG = Logger.getLogger(AuthResource.class);

    @Inject
    UserService userService;

    @Inject
    AuthService authService;

    @Inject
    JsonWebToken jwt;

    @POST
    @Path("/register")
    public Response register(@Valid RegisterRequest request) {
        UserResponse user = userService.register(request);
        LOG.infof("User registered: %s", user.email());
        return Response.status(Response.Status.CREATED).entity(user).build();
    }

    @POST
    @Path("/activate")
    public Response activate(@Valid ActivationRequest request) {
        userService.activateUser(request);
        LOG.infof("User activated with token");
        return Response.ok(Map.of("message", "Account activated successfully")).build();
    }

    @POST
    @Path("/request-password-reset")
    public Response requestPasswordReset(@Valid PasswordResetRequest request) {
        userService.requestPasswordReset(request);
        LOG.infof("Password reset requested for: %s", request.email());
        return Response.ok(Map.of("message", "Password reset email sent if account exists"))
                .build();
    }

    @POST
    @Path("/reset-password")
    public Response resetPassword(@Valid PasswordResetUpdateRequest request) {
        userService.resetPassword(request);
        LOG.infof("Password reset successfully");
        return Response.ok(Map.of("message", "Password reset successfully")).build();
    }

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest request) {
        TokenResponse tokens = authService.login(request);
        LOG.infof("User logged in: %s", request.email());

        return Response.ok(tokens).build();
    }

    @POST
    @Path("/refresh")
    public Response refresh(@Valid RefreshRequest request) {
        TokenResponse newTokens = authService.refresh(request);
        LOG.infof("Token refreshed successfully");
        return Response.ok(newTokens).build();
    }
}
