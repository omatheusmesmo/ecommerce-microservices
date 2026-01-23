package com.ecommerce.event;

import com.ecommerce.enums.OrderStatus;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;

@RegisterForReflection
public record OrderStatusChangedEvent(
        Long orderId,
        String customerEmail,
        OrderStatus oldStatus,
        OrderStatus newStatus,
        LocalDateTime changedAt
) {}