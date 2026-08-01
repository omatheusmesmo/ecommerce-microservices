package com.ecommerce.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@RegisterForReflection
public record PasswordResetRequest(@NotBlank @Email String email) {}
