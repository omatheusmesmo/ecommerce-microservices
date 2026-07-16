package com.ecommerce.service;

import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.dto.UserResponse;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class UserServiceTest {

    @Inject
    UserService userService;

    @Inject
    UserRepository userRepository;

    private Long persistUser(String email, Role role) {
        User user = new User();
        user.email = email;
        user.passwordHash = "hash";
        user.fullName = "Test User";
        user.role = role;
        user.active = true;
        userRepository.persist(user);
        return user.id;
    }

    @Test
    @TestTransaction
    void register_newEmail_persistsInactiveUser() {
        UserResponse response = userService.register(
                new RegisterRequest("new@example.com", "Passw0rd!23", "New User"));

        assertNotNull(response);
        assertEquals("new@example.com", response.email());
        assertFalse(response.active());
        assertNotNull(userRepository.findByEmail("new@example.com"));
    }

    @Test
    @TestTransaction
    void register_existingEmail_throwsIllegalArgumentException() {
        persistUser("taken@example.com", Role.CUSTOMER);

        assertThrows(IllegalArgumentException.class, () -> userService.register(
                new RegisterRequest("taken@example.com", "Passw0rd!23", "Someone")));
    }

    @Test
    @TestTransaction
    void promote_byAdmin_updatesRole() {
        Long adminId = persistUser("admin@example.com", Role.ADMIN);
        Long targetId = persistUser("target@example.com", Role.CUSTOMER);

        UserResponse response = userService.promote(targetId, Role.SELLER, adminId);

        assertEquals(Role.SELLER, response.role());
        assertEquals(Role.SELLER, userRepository.findById(targetId).role);
    }

    @Test
    @TestTransaction
    void promote_byNonAdmin_throwsSecurityException() {
        Long nonAdminId = persistUser("customer@example.com", Role.CUSTOMER);
        Long targetId = persistUser("target2@example.com", Role.CUSTOMER);

        assertThrows(SecurityException.class, () -> userService.promote(targetId, Role.SELLER, nonAdminId));
        assertEquals(Role.CUSTOMER, userRepository.findById(targetId).role);
    }

    @Test
    @TestTransaction
    void promote_targetNotFound_throwsIllegalArgumentException() {
        Long adminId = persistUser("admin2@example.com", Role.ADMIN);

        assertThrows(IllegalArgumentException.class,
                () -> userService.promote(Long.MAX_VALUE, Role.SELLER, adminId));
    }

    @Test
    @TestTransaction
    void delete_byAdmin_deletesTargetUser() {
        Long adminId = persistUser("admin3@example.com", Role.ADMIN);
        Long targetId = persistUser("target3@example.com", Role.CUSTOMER);

        assertDoesNotThrow(() -> userService.delete(targetId, adminId));

        assertNull(userRepository.findById(targetId));
    }

    @Test
    @TestTransaction
    void delete_byNonAdmin_throwsSecurityException() {
        Long nonAdminId = persistUser("customer2@example.com", Role.CUSTOMER);
        Long targetId = persistUser("target4@example.com", Role.CUSTOMER);

        assertThrows(SecurityException.class, () -> userService.delete(targetId, nonAdminId));
        assertNotNull(userRepository.findById(targetId));
    }

    @Test
    @TestTransaction
    void delete_targetNotFound_throwsNoSuchElementException() {
        Long adminId = persistUser("admin4@example.com", Role.ADMIN);

        assertThrows(NoSuchElementException.class, () -> userService.delete(Long.MAX_VALUE, adminId));
    }
}
