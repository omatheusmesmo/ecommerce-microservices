package com.ecommerce.event;

import java.time.LocalDateTime;

public record StockReleasedEvent(long orderId, LocalDateTime releasedAt) {}
