package com.ecommerce.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        String customerName,
        String customerEmail,
        String status,
        BigDecimal totalAmount,
        List<OrderItemEvent> items,
        LocalDateTime createdAt
) {
    public record OrderItemEvent(
            String productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {}
}