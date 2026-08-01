package com.ecommerce.event;

import com.ecommerce.valueobject.Money;
import java.time.LocalDateTime;

public record ProductCreatedEvent(
        String productId, String name, String categoryId, Money price, Integer stock, LocalDateTime createdAt) {}
