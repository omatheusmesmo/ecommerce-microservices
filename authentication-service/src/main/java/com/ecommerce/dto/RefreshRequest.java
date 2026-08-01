package com.ecommerce.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;

@RegisterForReflection
public record RefreshRequest(@NotBlank String refreshToken) {}
