package com.ecommerce.dto;

import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartStatus;
import com.ecommerce.valueobject.Money;
import java.time.LocalDateTime;
import java.util.List;

public record CartResponse(
        Long id,
        String customerEmail,
        CartStatus status,
        Money totalAmount,
        List<CartItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static CartResponse from(Cart cart) {
        return new CartResponse(
                cart.id,
                cart.customerEmail,
                cart.status,
                cart.totalAmount,
                cart.getItems().stream().map(CartItemResponse::from).toList(),
                cart.createdAt,
                cart.updatedAt);
    }
}
