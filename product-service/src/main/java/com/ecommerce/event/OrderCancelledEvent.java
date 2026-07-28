package com.ecommerce.event;

import com.ecommerce.valueobject.Money;

import java.time.LocalDateTime;
import java.util.List;

public record OrderCancelledEvent(
        long orderId,
        String customerName,
        Money totalAmount,
        List<OrderItem> items,
        LocalDateTime cancelledAt
) {
    public record OrderItem(String productId, int quantity) {}
}
