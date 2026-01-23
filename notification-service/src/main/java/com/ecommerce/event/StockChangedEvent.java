package com.ecommerce.event;

import com.ecommerce.enums.StockChangeReason;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;

@RegisterForReflection
public record StockChangedEvent(
        String productId,
        String productName,
        Integer oldStock,
        Integer newStock,
        StockChangeReason reason,
        LocalDateTime changedAt
) {}

