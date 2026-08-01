package com.ecommerce.dto;

import com.ecommerce.entity.CartItem;
import com.ecommerce.valueobject.Money;

public record CartItemResponse(
        Long id, String productId, String productName, Integer quantity, Money unitPrice, Money subtotal) {
    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(
                item.id, item.productId, item.productName, item.quantity, item.unitPrice, item.getSubtotal());
    }
}
