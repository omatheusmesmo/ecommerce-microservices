package com.ecommerce.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.math.BigDecimal;

@RegisterForReflection
public record OrderItemEvent(
        String productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
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