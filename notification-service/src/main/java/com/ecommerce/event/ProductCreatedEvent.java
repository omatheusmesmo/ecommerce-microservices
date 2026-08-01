package com.ecommerce.event;

import com.ecommerce.valueobject.Money;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;

@RegisterForReflection
public record ProductCreatedEvent(
        String productId, String name, String categoryId, Money price, Integer stock, LocalDateTime createdAt) {}
