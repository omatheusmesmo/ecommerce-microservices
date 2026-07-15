package com.ecommerce.service;

import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.dto.UserResponse;
import com.ecommerce.entity.ActionType;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.entity.UserActionToken;
import com.ecommerce.repository.UserRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class UserServiceTest {

    @Inject
    UserService userService;

    @InjectMock
    UserRepository userRepository;

    @InjectMock
    UserActionTokenService userActionTokenService;

    @BeforeEach
    void setUp() {
        reset(userRepository, userActionTokenService);
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.id = id;
        user.email = "user" + id + "@example.com";
        user.fullName = "User " + id;
        user.role = role;
        user.active = true;
        return user;
    }

    @Test
    void register_newEmail_persistsInactiveUserAndCreatesActivationToken() {
        UserActionToken token = new UserActionToken();
        when(userRepository.findByEmail("new@example.com")).thenReturn(null);
        when(userActionTokenService.createForUser(any(), eq(ActionType.ACTIVATE))).thenReturn(token);
        when(userActionTokenService.getRawToken(token)).thenReturn("raw-activation-token");

        UserResponse response = userService.register(
                new RegisterRequest("new@example.com", "Passw0rd!23", "New User"));

        assertNotNull(response);
        assertEquals("new@example.com", response.email());
        assertFalse(response.active());
        verify(userRepository, times(1)).persist(any(User.class));
    }

    @Test
    void register_existingEmail_throwsIllegalArgumentException() {
        when(userRepository.findByEmail("taken@example.com")).thenReturn(user(1L, Role.CUSTOMER));

        assertThrows(IllegalArgumentException.class, () -> userService.register(
                new RegisterRequest("taken@example.com", "Passw0rd!23", "Someone")));
        verify(userRepository, never()).persist(any(User.class));
    }

    @Test
    void promote_byAdmin_updatesRole() {
        User admin = user(1L, Role.ADMIN);
        User target = user(2L, Role.CUSTOMER);
        when(userRepository.findById(1L)).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(target);

        UserResponse response = userService.promote(2L, Role.SELLER, 1L);

        assertEquals(Role.SELLER, response.role());
        verify(userRepository, times(1)).persist(target);
    }

    @Test
    void promote_byNonAdmin_throwsSecurityException() {
        User nonAdmin = user(1L, Role.CUSTOMER);
        when(userRepository.findById(1L)).thenReturn(nonAdmin);

        assertThrows(SecurityException.class, () -> userService.promote(2L, Role.SELLER, 1L));
        verify(userRepository, never()).persist(any(User.class));
    }

    @Test
    void promote_targetNotFound_throwsIllegalArgumentException() {
        User admin = user(1L, Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> userService.promote(2L, Role.SELLER, 1L));
    }

    @Test
    void delete_byAdmin_deletesTargetUser() {
        User admin = user(1L, Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(admin);
        when(userRepository.deleteById(2L)).thenReturn(true);

        assertDoesNotThrow(() -> userService.delete(2L, 1L));
        verify(userRepository, times(1)).deleteById(2L);
    }

    @Test
    void delete_byNonAdmin_throwsSecurityException() {
        User nonAdmin = user(1L, Role.CUSTOMER);
        when(userRepository.findById(1L)).thenReturn(nonAdmin);

        assertThrows(SecurityException.class, () -> userService.delete(2L, 1L));
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void delete_targetNotFound_throwsNoSuchElementException() {
        User admin = user(1L, Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(admin);
        when(userRepository.deleteById(2L)).thenReturn(false);

        assertThrows(NoSuchElementException.class, () -> userService.delete(2L, 1L));
    }
}
