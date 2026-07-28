package com.ecommerce.event;

import com.ecommerce.dto.OrderResponse;
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
        Money shippingCost,
        List<OrderItemEvent> items,
        LocalDateTime createdAt
) {
    public static OrderCreatedEvent from(OrderResponse orderResponse) {
        return new OrderCreatedEvent(
                orderResponse.id(),
                orderResponse.customerName(),
                orderResponse.customerEmail(),
                orderResponse.status().name(),
                orderResponse.totalAmount(),
                orderResponse.shippingCost(),
                orderResponse.items().stream()
                        .map(OrderItemEvent::from)
                        .toList(),
                orderResponse.createdAt()
        );
    }
}
