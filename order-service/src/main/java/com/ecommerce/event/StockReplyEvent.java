package com.ecommerce.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The one field every stock reply carries. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StockReplyEvent(long orderId) {}
