package com.ecommerce.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record TokenResponse(String accessToken, String refreshToken) {}
