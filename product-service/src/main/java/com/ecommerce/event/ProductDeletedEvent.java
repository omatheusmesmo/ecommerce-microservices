package com.ecommerce.event;

import java.time.LocalDateTime;

public record ProductDeletedEvent(
        String productId,
        String name,
        LocalDateTime deletedAt
) {
}
