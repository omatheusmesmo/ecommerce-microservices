package com.ecommerce.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Coordination state of the order SAGA, kept separate from {@link OrderStatus} so that
 * business status stays limited to what clients and the notification-service understand.
 *
 * <p>{@code FAILED} means the SAGA stopped with nothing to undo, {@code COMPENSATED} means it
 * stopped after undoing work it had already done.
 */
@RegisterForReflection
public enum OrderSagaStep {
    RESERVE_STOCK,
    PROCESS_PAYMENT,
    CONFIRM_STOCK,
    COMPENSATING,
    COMPLETED,
    COMPENSATED,
    FAILED;

    private static final Map<OrderSagaStep, Set<OrderSagaStep>> TRANSITIONS = Map.of(
            RESERVE_STOCK, EnumSet.of(PROCESS_PAYMENT, CONFIRM_STOCK, COMPENSATING, FAILED),
            PROCESS_PAYMENT, EnumSet.of(CONFIRM_STOCK, COMPENSATING),
            CONFIRM_STOCK, EnumSet.of(COMPLETED),
            COMPENSATING, EnumSet.of(COMPENSATED),
            COMPLETED, EnumSet.noneOf(OrderSagaStep.class),
            COMPENSATED, EnumSet.noneOf(OrderSagaStep.class),
            FAILED, EnumSet.noneOf(OrderSagaStep.class));

    public boolean canTransitionTo(OrderSagaStep target) {
        return TRANSITIONS.get(this).contains(target);
    }

    public boolean isTerminal() {
        return TRANSITIONS.get(this).isEmpty();
    }
}
