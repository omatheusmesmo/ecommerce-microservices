package com.ecommerce.event;

import com.ecommerce.valueobject.Money;

import java.time.LocalDateTime;

public record ProductUpdatedEvent(
        String productId,
        String name,
        String category,
        Money price,
        Integer stock,
        LocalDateTime updatedAt
) {
}
