package com.ecommerce.event;

import java.time.LocalDateTime;

public record StockReservedEvent(long orderId, LocalDateTime reservedAt) {}
