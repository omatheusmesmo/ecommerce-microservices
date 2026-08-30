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
 * stopped after undoing work it had already done, and {@code ABANDONED} means it gave up
 * without learning the outcome of its last command, so it cannot claim either.
 *
 * <p>{@code CONFIRM_STOCK} leads to {@code ABANDONED} rather than {@code COMPENSATING} because
 * a confirmation that went unanswered may well have been applied, and releasing stock that was
 * already consumed would be a worse lie than admitting the outcome is unknown.
 */
@RegisterForReflection
public enum OrderSagaStep {
    RESERVE_STOCK,
    PROCESS_PAYMENT,
    CONFIRM_STOCK,
    COMPENSATING,
    COMPLETED,
    COMPENSATED,
    FAILED,
    ABANDONED;

    private static final Map<OrderSagaStep, Set<OrderSagaStep>> TRANSITIONS = Map.of(
            RESERVE_STOCK, EnumSet.of(PROCESS_PAYMENT, CONFIRM_STOCK, COMPENSATING, FAILED),
            PROCESS_PAYMENT, EnumSet.of(CONFIRM_STOCK, COMPENSATING),
            CONFIRM_STOCK, EnumSet.of(COMPLETED, ABANDONED),
            COMPENSATING, EnumSet.of(COMPENSATED, ABANDONED),
            COMPLETED, EnumSet.noneOf(OrderSagaStep.class),
            COMPENSATED, EnumSet.noneOf(OrderSagaStep.class),
            FAILED, EnumSet.noneOf(OrderSagaStep.class),
            ABANDONED, EnumSet.noneOf(OrderSagaStep.class));

    public boolean canTransitionTo(OrderSagaStep target) {
        return TRANSITIONS.get(this).contains(target);
    }

    public boolean isTerminal() {
        return TRANSITIONS.get(this).isEmpty();
    }
}
