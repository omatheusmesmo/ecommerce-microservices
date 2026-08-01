package com.ecommerce.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@RegisterForReflection
public enum CartStatus {
    ACTIVE,
    ABANDONED;

    private static final Map<CartStatus, Set<CartStatus>> TRANSITIONS = Map.of(
            ACTIVE, EnumSet.of(ABANDONED),
            ABANDONED, EnumSet.noneOf(CartStatus.class));

    public boolean canTransitionTo(CartStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }
}
