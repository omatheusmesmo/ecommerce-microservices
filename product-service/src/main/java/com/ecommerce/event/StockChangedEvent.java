package com.ecommerce.event;

import com.ecommerce.entity.StockChangedReason;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockChangedEvent(
        String productId,
        String productName,
        Integer oldStock,
        Integer newStock,
        StockChangedReason reason,
        LocalDateTime changedAt
) {
}
