package com.ecommerce.dto;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.valueobject.Money;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerName,
        String customerEmail,
        OrderStatus status,
        Money totalAmount,
        Money shippingCost,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.id,
                order.customerName,
                order.customerEmail,
                order.status,
                order.totalAmount,
                order.shippingCost,
                order.getItems().stream()
                        .map(OrderItemResponse::from)
                        .toList(),
                order.createdAt,
                order.updatedAt
        );
    }

    public static OrderResponse fromWithoutItems(Order order) {
        return new OrderResponse(
                order.id,
                order.customerName,
                order.customerEmail,
                order.status,
                order.totalAmount,
                order.shippingCost,
                List.of(),
                order.createdAt,
                order.updatedAt
        );
    }
}
