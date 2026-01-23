package com.ecommerce.event;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RegisterForReflection
public record OrderCreatedEvent(
        Long orderId,
        String customerName,
        String customerEmail,
        String status,
        BigDecimal totalAmount,
        List<OrderItemEvent> items,
        LocalDateTime createdAt
) {
    @RegisterForReflection
    public record OrderItemEvent(
            String productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {}
}