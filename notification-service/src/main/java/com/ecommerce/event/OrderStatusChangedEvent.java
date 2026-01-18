package com.ecommerce.event;

import com.ecommerce.enums. OrderStatus;

import java.time.LocalDateTime;

public record OrderStatusChangedEvent(
        Long orderId,
        String customerEmail,
        OrderStatus oldStatus,
        OrderStatus newStatus,
        LocalDateTime changedAt
) {}