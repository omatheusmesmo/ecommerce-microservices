package com.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RefreshRequest;
import com.ecommerce.dto.TokenResponse;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.util.CryptoUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AuthServiceTest {

    @Inject
    AuthService authService;

    @Inject
    UserRepository userRepository;

    private Long persistActiveUser(String email, String rawPassword) {
        User user = new User();
        user.email = email;
        user.passwordHash = CryptoUtil.hashPassword(rawPassword);
        user.fullName = "Test User";
        user.role = Role.CUSTOMER;
        user.active = true;
        userRepository.persist(user);
        return user.id;
    }

    @Test
    @TestTransaction
    void login_validCredentials_returnsAccessAndRefreshTokens() {
        persistActiveUser("login@example.com", "correct-password");

        TokenResponse response = authService.login(new LoginRequest("login@example.com", "correct-password"));

        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
    }

    @Test
    @TestTransaction
    void login_unknownEmail_throwsSecurityException() {
        assertThrows(
                SecurityException.class, () -> authService.login(new LoginRequest("nobody@example.com", "whatever")));
    }

    @Test
    @TestTransaction
    void login_inactiveUser_throwsSecurityException() {
        String email = "inactive@example.com";
        Long id = persistActiveUser(email, "correct-password");
        userRepository.findById(id).active = false;

        assertThrows(SecurityException.class, () -> authService.login(new LoginRequest(email, "correct-password")));
    }

    @Test
    @TestTransaction
    void login_wrongPassword_throwsSecurityException() {
        String email = "login2@example.com";
        persistActiveUser(email, "correct-password");

        assertThrows(SecurityException.class, () -> authService.login(new LoginRequest(email, "wrong-password")));
    }

    @Test
    @TestTransaction
    void refresh_validToken_revokesOldTokenAndReturnsNewTokens() {
        persistActiveUser("refresh@example.com", "correct-password");
        TokenResponse loginResponse = authService.login(new LoginRequest("refresh@example.com", "correct-password"));

        TokenResponse refreshResponse = authService.refresh(new RefreshRequest(loginResponse.refreshToken()));

        assertNotNull(refreshResponse.accessToken());
        assertNotNull(refreshResponse.refreshToken());
        assertNotEquals(loginResponse.refreshToken(), refreshResponse.refreshToken());

        assertThrows(
                SecurityException.class,
                () -> authService.refresh(new RefreshRequest(loginResponse.refreshToken())),
                "the old refresh token should have been revoked");
    }

    @Test
    @TestTransaction
    void refresh_invalidToken_throwsSecurityException() {
        assertThrows(SecurityException.class, () -> authService.refresh(new RefreshRequest("bogus-token")));
    }
}
