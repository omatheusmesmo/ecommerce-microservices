package com.ecommerce.saga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderSaga;
import com.ecommerce.entity.OrderSagaStep;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.OutboxEvent;
import com.ecommerce.outbox.OrderCommandPublisher;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.valueobject.Money;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

/**
 * Covers what happens when a step's reply never arrives. The SAGA has no other way to learn
 * that a command was lost, so every assertion here is about the sweep being the only thing
 * standing between a dropped message and an order stuck forever.
 */
@QuarkusTest
class OrderSagaTimeoutTest {

    @Inject
    OrderSagaOrchestrator orchestrator;

    @Inject
    OrderRepository orderRepository;

    @ConfigProperty(name = "order.saga.max-attempts")
    int maxAttempts;

    private Order persistOrderWithSaga() {
        Order order = new Order("Jane Doe", "jane@example.com");
        order.addItem(new OrderItem("product-1", "Product One", 3, new Money(new BigDecimal("50.00"), "BRL")));
        order.calculateTotal();
        orderRepository.persist(order);
        orchestrator.start(order);
        return order;
    }

    private OrderSaga sagaOf(Long orderId) {
        return OrderSaga.findByOrderId(orderId).orElseThrow();
    }

    private long commandCount(Long orderId, String eventType) {
        return OutboxEvent.count(
                "aggregateType = ?1 and aggregateId = ?2 and eventType = ?3",
                "OrderCommand",
                orderId.toString(),
                eventType);
    }

    private OrderSaga overdueSaga(Long orderId, int attempts) {
        OrderSaga saga = sagaOf(orderId);
        saga.attempts = attempts;
        saga.deadlineAt = LocalDateTime.now().minusMinutes(1);
        return saga;
    }

    @Test
    @TestTransaction
    void aStartedSagaIsArmedWithADeadlineSoALostCommandIsNoticed() {
        Order order = persistOrderWithSaga();

        assertNotNull(sagaOf(order.id).deadlineAt, "the first command must be recoverable if it is lost");
    }

    @Test
    @TestTransaction
    void theSweepPicksUpOnlyTheSagasPastTheirDeadline() {
        Order waiting = persistOrderWithSaga();
        Order overdue = persistOrderWithSaga();
        overdueSaga(overdue.id, 0);

        var swept = orchestrator.ordersWithOverdueReplies(100);

        assertTrue(swept.contains(overdue.id), "a step past its deadline must be swept");
        assertFalse(swept.contains(waiting.id), "a step still within its deadline must be left alone");
    }

    @Test
    @TestTransaction
    void anOverdueStepHasItsCommandSentAgain() {
        Order order = persistOrderWithSaga();
        overdueSaga(order.id, 0);

        orchestrator.recoverTimedOut(order.id);

        assertEquals(2, commandCount(order.id, OrderCommandPublisher.RESERVE_STOCK));
        assertEquals(1, sagaOf(order.id).attempts);
        assertTrue(sagaOf(order.id).deadlineAt.isAfter(LocalDateTime.now()), "the retry must get its own deadline");
    }

    @Test
    @TestTransaction
    void aReplyThatLandsBeforeTheSweepArrivesCancelsTheRetry() {
        Order order = persistOrderWithSaga();

        orchestrator.recoverTimedOut(order.id);

        assertEquals(
                1,
                commandCount(order.id, OrderCommandPublisher.RESERVE_STOCK),
                "a SAGA still within its deadline must not have its command re-sent");
        assertEquals(0, sagaOf(order.id).attempts);
    }

    @Test
    @TestTransaction
    void aReservationThatRunsOutOfAttemptsAsksForTheStockBack() {
        Order order = persistOrderWithSaga();
        overdueSaga(order.id, maxAttempts);

        orchestrator.recoverTimedOut(order.id);

        OrderSaga saga = sagaOf(order.id);
        assertEquals(OrderSagaStep.COMPENSATING, saga.currentStep);
        assertEquals(1, commandCount(order.id, OrderCommandPublisher.RELEASE_STOCK));
        assertTrue(saga.failureReason.contains("RESERVE_STOCK"));
    }

    @Test
    @TestTransaction
    void anUnansweredConfirmationIsAbandonedRatherThanReleased() {
        Order order = persistOrderWithSaga();
        orchestrator.onStockReserved(order.id);
        overdueSaga(order.id, maxAttempts);

        orchestrator.recoverTimedOut(order.id);

        assertEquals(OrderSagaStep.ABANDONED, sagaOf(order.id).currentStep);
        assertEquals(
                0,
                commandCount(order.id, OrderCommandPublisher.RELEASE_STOCK),
                "the confirmation may have been applied, so releasing the stock could double-spend it");
    }

    @Test
    @TestTransaction
    void anUnansweredReleaseAbandonsTheSagaWithoutClaimingTheOrderWasCancelled() {
        Order order = persistOrderWithSaga();
        orchestrator.compensate(order.id, "Cancelled by the customer");
        overdueSaga(order.id, maxAttempts);

        orchestrator.recoverTimedOut(order.id);

        OrderSaga saga = sagaOf(order.id);
        assertEquals(OrderSagaStep.ABANDONED, saga.currentStep);
        assertNull(saga.deadlineAt, "an abandoned SAGA must stop being swept");
        assertEquals(
                OrderStatus.PENDING,
                orderRepository.findByIdOptional(order.id).orElseThrow().status,
                "the order must not be reported cancelled while the stock release is unacknowledged");
    }

    @Test
    @TestTransaction
    void aFinishedSagaIsNeverSweptAgain() {
        Order order = persistOrderWithSaga();
        orchestrator.onStockReserved(order.id);
        orchestrator.onStockConfirmed(order.id);

        assertEquals(OrderSagaStep.COMPLETED, sagaOf(order.id).currentStep);
        assertNull(sagaOf(order.id).deadlineAt);
        assertFalse(orchestrator.ordersWithOverdueReplies(100).contains(order.id));
    }
}
