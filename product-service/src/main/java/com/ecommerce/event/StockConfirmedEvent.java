package com.ecommerce.event;

import java.time.LocalDateTime;

public record StockConfirmedEvent(long orderId, LocalDateTime confirmedAt) {}
