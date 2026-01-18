package com.ecommerce.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductCreatedEvent(
        String productId,
        String name,
        String category,
        BigDecimal price,
        Integer stock,
        LocalDateTime createdAt
) {}