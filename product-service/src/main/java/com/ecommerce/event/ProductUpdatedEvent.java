package com.ecommerce.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductUpdatedEvent(
        String productId,
        String name,
        String category,
        BigDecimal price,
        Integer stock,
        LocalDateTime updatedAt
) {
}
