package com.ecommerce.enums;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public enum StockChangeReason {
    PURCHASE,
    RESTOCK,
    ADJUSTMENT,
    RETURN,
    DAMAGE,
    LOST,
    INITIAL}