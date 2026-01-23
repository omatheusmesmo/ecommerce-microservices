package com.ecommerce.enums;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
