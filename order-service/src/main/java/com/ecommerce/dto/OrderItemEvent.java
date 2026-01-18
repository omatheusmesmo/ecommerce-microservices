package com.ecommerce.dto;

import java.math.BigDecimal;

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