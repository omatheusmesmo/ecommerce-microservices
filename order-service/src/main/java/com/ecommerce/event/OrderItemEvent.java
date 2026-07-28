package com.ecommerce.event;

import com.ecommerce.dto.OrderItemResponse;
import com.ecommerce.valueobject.Money;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record OrderItemEvent(
        String productId,
        String productName,
        Integer quantity,
        Money unitPrice,
        Money subtotal
) {
    public static OrderItemEvent from(OrderItemResponse item) {
        return new OrderItemEvent(
                item.productId(),
                item.productName(),
                item.quantity(),
                item.unitPrice(),
                item.subtotal()
        );
    }
}
