package com.ecommerce.service;

import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RefreshRequest;
import com.ecommerce.dto.TokenResponse;
import com.ecommerce.entity.RefreshToken;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.util.CryptoUtil;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class AuthServiceTest {

    @Inject
    AuthService authService;

    @InjectMock
    UserRepository userRepository;

    @InjectMock
    RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        reset(userRepository, refreshTokenService);
    }

    private User activeUser(Long id, String email, String rawPassword) {
        User user = new User();
        user.id = id;
        user.email = email;
        user.passwordHash = CryptoUtil.hashPassword(rawPassword);
        user.fullName = "Test User";
        user.role = Role.CUSTOMER;
        user.active = true;
        return user;
    }

    @Test
    void login_validCredentials_returnsAccessAndRefreshTokens() {
        User user = activeUser(1L, "login@example.com", "correct-password");
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.id = 10L;

        when(userRepository.findByEmail("login@example.com")).thenReturn(user);
        when(refreshTokenService.createForUser(user.id)).thenReturn(refreshToken);
        when(refreshTokenService.getRawToken(refreshToken)).thenReturn("raw-refresh-token");

        TokenResponse response = authService.login(new LoginRequest("login@example.com", "correct-password"));

        assertNotNull(response.accessToken());
        assertEquals("raw-refresh-token", response.refreshToken());
    }

    @Test
    void login_unknownEmail_throwsSecurityException() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(null);

        assertThrows(SecurityException.class,
                () -> authService.login(new LoginRequest("nobody@example.com", "whatever")));
        verify(refreshTokenService, never()).createForUser(any());
    }

    @Test
    void login_inactiveUser_throwsSecurityException() {
        User user = activeUser(2L, "inactive@example.com", "correct-password");
        user.active = false;
        when(userRepository.findByEmail("inactive@example.com")).thenReturn(user);

        assertThrows(SecurityException.class,
                () -> authService.login(new LoginRequest("inactive@example.com", "correct-password")));
    }

    @Test
    void login_wrongPassword_throwsSecurityException() {
        User user = activeUser(3L, "login2@example.com", "correct-password");
        when(userRepository.findByEmail("login2@example.com")).thenReturn(user);

        assertThrows(SecurityException.class,
                () -> authService.login(new LoginRequest("login2@example.com", "wrong-password")));
    }

    @Test
    void refresh_validToken_revokesOldTokenAndReturnsNewTokens() {
        User user = activeUser(4L, "refresh@example.com", "correct-password");
        RefreshToken oldToken = new RefreshToken();
        oldToken.id = 20L;
        oldToken.userId = user.id;
        RefreshToken newToken = new RefreshToken();
        newToken.id = 21L;

        when(refreshTokenService.findByToken("old-raw-token")).thenReturn(oldToken);
        when(userRepository.findById(user.id)).thenReturn(user);
        when(refreshTokenService.createForUser(user.id)).thenReturn(newToken);
        when(refreshTokenService.getRawToken(newToken)).thenReturn("new-raw-token");

        TokenResponse response = authService.refresh(new RefreshRequest("old-raw-token"));

        assertNotNull(response.accessToken());
        assertEquals("new-raw-token", response.refreshToken());
        verify(refreshTokenService, times(1)).revoke(oldToken);
    }

    @Test
    void refresh_invalidToken_throwsSecurityException() {
        when(refreshTokenService.findByToken("bogus-token")).thenReturn(null);

        assertThrows(SecurityException.class,
                () -> authService.refresh(new RefreshRequest("bogus-token")));
        verify(refreshTokenService, never()).revoke(any());
    }
}
