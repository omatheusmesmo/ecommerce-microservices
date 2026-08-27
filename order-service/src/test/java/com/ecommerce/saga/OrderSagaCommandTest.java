package com.ecommerce.saga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ecommerce.consumer.StockReplyConsumer;
import com.ecommerce.consumer.StockReplyHandler;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderSaga;
import com.ecommerce.entity.OrderSagaStep;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.OutboxEvent;
import com.ecommerce.outbox.OrderCommandPublisher;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.service.OrderService;
import com.ecommerce.valueobject.Money;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OrderSagaCommandTest {

    @Inject
    OrderSagaOrchestrator orchestrator;

    @Inject
    OrderRepository orderRepository;

    @Inject
    OrderService orderService;

    @Inject
    StockReplyHandler replyHandler;

    private Order persistOrderWithSaga() {
        Order order = new Order("Jane Doe", "jane@example.com");
        order.addItem(new OrderItem("product-1", "Product One", 3, new Money(new BigDecimal("50.00"), "BRL")));
        order.calculateTotal();
        orderRepository.persist(order);
        orchestrator.start(order);
        return order;
    }

    private OutboxEvent commandOf(Long orderId, String eventType) {
        return OutboxEvent.<OutboxEvent>find(
                        "aggregateType = ?1 and aggregateId = ?2 and eventType = ?3",
                        "OrderCommand",
                        orderId.toString(),
                        eventType)
                .firstResult();
    }

    @Test
    @TestTransaction
    void startingTheSagaAsksForTheOrdersStock() {
        Order order = persistOrderWithSaga();

        OutboxEvent command = commandOf(order.id, OrderCommandPublisher.RESERVE_STOCK);

        assertNotNull(command, "starting a SAGA must issue a ReserveStock command");
        assertTrue(command.payload.contains("product-1"), "the command must carry the ordered products");
        assertTrue(command.payload.contains("\"quantity\":3"), "the command must carry the ordered quantities");
    }

    @Test
    @TestTransaction
    void aReservationReplyAsksForTheReservationToBeConfirmed() {
        Order order = persistOrderWithSaga();

        orchestrator.onStockReserved(order.id);

        assertNotNull(
                commandOf(order.id, OrderCommandPublisher.CONFIRM_STOCK_RESERVATION),
                "a reserved reply must issue ConfirmStockReservation");
        assertEquals(
                OrderSagaStep.CONFIRM_STOCK, OrderSaga.findByOrderId(order.id).orElseThrow().currentStep);
    }

    @Test
    @TestTransaction
    void compensatingAsksForTheStockBack() {
        Order order = persistOrderWithSaga();

        orchestrator.compensate(order.id, "Cancelled by the customer");

        assertNotNull(
                commandOf(order.id, OrderCommandPublisher.RELEASE_STOCK),
                "compensation must issue a ReleaseStock command");
    }

    @Test
    @TestTransaction
    void cancellingAnOrderThatHoldsStockCompensatesInsteadOfCancellingOutright() {
        Order order = persistOrderWithSaga();

        orderService.cancelOrder(order.id);

        assertNotNull(
                commandOf(order.id, OrderCommandPublisher.RELEASE_STOCK),
                "cancelling an order holding a reservation must release it");
        assertEquals(
                OrderSagaStep.COMPENSATING, OrderSaga.findByOrderId(order.id).orElseThrow().currentStep);
        assertEquals(
                OrderStatus.PENDING,
                orderRepository.findByIdOptional(order.id).orElseThrow().status,
                "the order stays PENDING until the release is acknowledged");
    }

    @Test
    @TestTransaction
    void aRedeliveredReplyAdvancesTheSagaOnlyOnce() throws Exception {
        Order order = persistOrderWithSaga();
        String eventId = UUID.randomUUID().toString();
        String payload = "{\"orderId\":" + order.id + "}";

        replyHandler.apply(eventId, StockReplyConsumer.STOCK_RESERVED, payload);
        replyHandler.apply(eventId, StockReplyConsumer.STOCK_RESERVED, payload);

        assertEquals(
                OrderSagaStep.CONFIRM_STOCK,
                OrderSaga.findByOrderId(order.id).orElseThrow().currentStep,
                "the second delivery must not advance the SAGA again");
        assertEquals(
                1,
                OutboxEvent.count(
                        "aggregateType = ?1 and aggregateId = ?2 and eventType = ?3",
                        "OrderCommand",
                        order.id.toString(),
                        OrderCommandPublisher.CONFIRM_STOCK_RESERVATION),
                "the second delivery must not issue the command again");
    }

    @Test
    @TestTransaction
    void aRejectionReplyFailsTheSagaAndCancelsTheOrder() throws Exception {
        Order order = persistOrderWithSaga();
        String payload = "{\"orderId\":" + order.id + ",\"reason\":\"Insufficient stock for product product-1\"}";

        replyHandler.apply(UUID.randomUUID().toString(), StockReplyConsumer.STOCK_REJECTED, payload);

        OrderSaga saga = OrderSaga.findByOrderId(order.id).orElseThrow();
        assertEquals(OrderSagaStep.FAILED, saga.currentStep);
        assertTrue(saga.failureReason.contains("product-1"));
        assertEquals(
                OrderStatus.CANCELLED,
                orderRepository.findByIdOptional(order.id).orElseThrow().status);
    }
}
