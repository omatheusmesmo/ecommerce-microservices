package com.ecommerce.dto;

import com.ecommerce.entity.OrderItem;
import com.ecommerce.valueobject.Money;

public record OrderItemResponse(
        Long id,
        String productId,
        String productName,
        Integer quantity,
        Money unitPrice,
        Money subtotal
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
