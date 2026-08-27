package com.ecommerce.event;

import java.time.LocalDateTime;

public record StockRejectedEvent(long orderId, String reason, LocalDateTime rejectedAt) {}
