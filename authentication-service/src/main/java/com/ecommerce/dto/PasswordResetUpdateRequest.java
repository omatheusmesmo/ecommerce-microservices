package com.ecommerce.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RegisterForReflection
public record PasswordResetUpdateRequest(
        @NotBlank String token,

        @NotBlank
        @Size(min = 8)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Password must contain at least one lowercase letter, one uppercase letter, and one digit")
        String newPassword) {}
