package com.ecommerce.event;

import com. ecommerce.enums.StockChangeReason;

import java.time.LocalDateTime;

public record StockChangedEvent(
        String productId,
        String productName,
        Integer oldStock,
        Integer newStock,
        StockChangeReason reason,
        LocalDateTime changedAt
) {}