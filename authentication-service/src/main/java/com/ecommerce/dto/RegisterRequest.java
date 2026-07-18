package com.ecommerce.dto;

import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RegisterForReflection
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank
        @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
                message = "Password must contain at least one lowercase letter, one uppercase letter, one digit, and one special character"
        )
        String password,
        @NotBlank String fullName
) {
    public User toUser(String hashedPassword) {
        User user = new User();
        user.email = email;
        user.passwordHash = hashedPassword;
        user.fullName = fullName;
        user.role = Role.CUSTOMER;
        return user;
    }
}
