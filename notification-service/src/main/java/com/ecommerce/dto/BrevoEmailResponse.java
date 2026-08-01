package com.ecommerce.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record BrevoEmailResponse(String messageId) {}
