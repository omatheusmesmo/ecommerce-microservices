package com.ecommerce.dto;

import java. math.BigDecimal;
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
    public static OrderCreatedEvent from(OrderResponse orderResponse) {
        return new OrderCreatedEvent(
                orderResponse.id(),
                orderResponse. customerName(),
                orderResponse. customerEmail(),
                orderResponse. status().name(),  // Enum → String
                orderResponse.totalAmount(),
                orderResponse.items().stream()
                        .map(OrderItemEvent::from)
                        .toList(),
                orderResponse.createdAt()
        );
    }
}