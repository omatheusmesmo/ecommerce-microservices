package com.ecommerce.dto;

import com.ecommerce.entity.OrderItem;

import java.math. BigDecimal;

public record OrderItemResponse(
        Long id,
        String productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.id,
                item.productId,
                item.productName,
                item.quantity,
                item.unitPrice,
                item.getSubtotal()
        );
    }
}