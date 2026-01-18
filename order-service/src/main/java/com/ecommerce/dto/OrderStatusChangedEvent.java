package com.ecommerce.dto;

import com.ecommerce.entity.OrderStatus;

import java.time.LocalDateTime;

public record OrderStatusChangedEvent(
        Long orderId,
        String customerEmail,
        String oldStatus,
        String newStatus,
        LocalDateTime changedAt
) {
    public OrderStatusChangedEvent(Long orderId, OrderStatus oldStatus, OrderStatus newStatus, String customerEmail, LocalDateTime changedAt) {
        this(orderId, customerEmail, oldStatus.name(), newStatus.name(), changedAt);
    }
}