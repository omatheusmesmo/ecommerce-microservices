package com.ecommerce.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StockRejectedEvent(long orderId, String reason) {}
