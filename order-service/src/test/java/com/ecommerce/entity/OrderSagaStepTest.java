package com.ecommerce.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class OrderSagaStepTest {

    private static final Set<OrderSagaStep> TERMINAL_STEPS =
            EnumSet.of(OrderSagaStep.COMPLETED, OrderSagaStep.COMPENSATED, OrderSagaStep.FAILED);

    @Test
    void happyPathWithoutPaymentGoesStraightFromReservationToConfirmation() {
        assertTrue(OrderSagaStep.RESERVE_STOCK.canTransitionTo(OrderSagaStep.CONFIRM_STOCK));
        assertTrue(OrderSagaStep.CONFIRM_STOCK.canTransitionTo(OrderSagaStep.COMPLETED));
    }

    @Test
    void paymentStepSlotsBetweenReservationAndConfirmation() {
        assertTrue(OrderSagaStep.RESERVE_STOCK.canTransitionTo(OrderSagaStep.PROCESS_PAYMENT));
        assertTrue(OrderSagaStep.PROCESS_PAYMENT.canTransitionTo(OrderSagaStep.CONFIRM_STOCK));
        assertTrue(OrderSagaStep.PROCESS_PAYMENT.canTransitionTo(OrderSagaStep.COMPENSATING));
    }

    @Test
    void rejectedReservationFailsWithoutCompensating() {
        assertTrue(OrderSagaStep.RESERVE_STOCK.canTransitionTo(OrderSagaStep.FAILED));
        assertFalse(OrderSagaStep.RESERVE_STOCK.canTransitionTo(OrderSagaStep.COMPLETED));
    }

    @Test
    void confirmedStockCannotBeCompensated() {
        assertFalse(OrderSagaStep.CONFIRM_STOCK.canTransitionTo(OrderSagaStep.COMPENSATING));
    }

    @ParameterizedTest
    @EnumSource(OrderSagaStep.class)
    void terminalStepsAcceptNoFurtherTransition(OrderSagaStep step) {
        assertEquals(TERMINAL_STEPS.contains(step), step.isTerminal());

        if (step.isTerminal()) {
            for (OrderSagaStep target : OrderSagaStep.values()) {
                assertFalse(step.canTransitionTo(target), step + " should not transition to " + target);
            }
        }
    }

    @Test
    void advanceToRejectsATransitionTheMachineDoesNotAllow() {
        OrderSaga saga = new OrderSaga(1L);

        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> saga.advanceTo(OrderSagaStep.COMPLETED));

        assertTrue(thrown.getMessage().contains("RESERVE_STOCK"));
        assertEquals(OrderSagaStep.RESERVE_STOCK, saga.currentStep);
    }

    @Test
    void advanceToResetsPerStepBookkeeping() {
        OrderSaga saga = new OrderSaga(1L);
        saga.attempts = 3;
        saga.deadlineAt = LocalDateTime.now();

        saga.advanceTo(OrderSagaStep.CONFIRM_STOCK);

        assertEquals(OrderSagaStep.CONFIRM_STOCK, saga.currentStep);
        assertEquals(0, saga.attempts);
        assertNull(saga.deadlineAt);
    }
}
