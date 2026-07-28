package com.ecommerce.event;

import com.ecommerce.valueobject.Money;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;
import java.util.List;

@RegisterForReflection
public record OrderCreatedEvent(
        Long orderId,
        String customerName,
        String customerEmail,
        String status,
        Money totalAmount,
        List<OrderItemEvent> items,
        LocalDateTime createdAt
) {
    @RegisterForReflection
    public record OrderItemEvent(
            String productId,
            String productName,
            Integer quantity,
            Money unitPrice,
            Money subtotal
    ) {}
}
