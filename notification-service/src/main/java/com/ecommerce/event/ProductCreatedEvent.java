package com.ecommerce.event;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@RegisterForReflection
public record ProductCreatedEvent(
        String productId,
        String name,
        String category,
        BigDecimal price,
        Integer stock,
        LocalDateTime createdAt
) {}