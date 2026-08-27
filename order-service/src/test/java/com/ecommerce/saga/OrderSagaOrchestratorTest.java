package com.ecommerce.saga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ecommerce.dto.CreateOrderRequest;
import com.ecommerce.dto.OrderItemRequest;
import com.ecommerce.dto.OrderResponse;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderSaga;
import com.ecommerce.entity.OrderSagaStep;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.OutboxEvent;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.service.OrderService;
import com.ecommerce.valueobject.Address;
import com.ecommerce.valueobject.Money;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OrderSagaOrchestratorTest {

    private static final Address SHIPPING_ADDRESS =
            new Address("Av. Paulista", "1000", "Apto 42", "São Paulo", "SP", "01310-100", "BR");

    @Inject
    OrderSagaOrchestrator orchestrator;

    @Inject
    OrderService orderService;

    @Inject
    OrderRepository orderRepository;

    private Long persistOrderWithSaga() {
        Order order = new Order("Jane Doe", "jane@example.com");
        order.addItem(new OrderItem("product-1", "Product One", 2, new Money(new BigDecimal("50.00"), "BRL")));
        order.calculateTotal();
        orderRepository.persist(order);
        orchestrator.start(order);
        return order.id;
    }

    private OrderSaga sagaOf(Long orderId) {
        return OrderSaga.findByOrderId(orderId).orElseThrow();
    }

    private OrderStatus statusOf(Long orderId) {
        return orderRepository.findByIdOptional(orderId).orElseThrow().status;
    }

    @Test
    @TestTransaction
    void creatingAnOrderOpensItsSagaInTheSameTransaction() {
        CreateOrderRequest request = new CreateOrderRequest(
                "Jane Doe",
                "jane@example.com",
                List.of(new OrderItemRequest("prod-1", "Gaming Chair", 2, new Money(new BigDecimal("100.00"), "BRL"))),
                new Money(new BigDecimal("15.00"), "BRL"),
                SHIPPING_ADDRESS,
                null);

        OrderResponse response = orderService.createOrder(request, null);

        OrderSaga saga = sagaOf(response.id());
        assertEquals(OrderSagaStep.RESERVE_STOCK, saga.currentStep);
        assertEquals(0, saga.attempts);
        assertEquals(OrderStatus.PENDING, statusOf(response.id()));
    }

    @Test
    @TestTransaction
    void reservedThenConfirmedStockCompletesTheSagaAndConfirmsTheOrder() {
        Long orderId = persistOrderWithSaga();

        orchestrator.onStockReserved(orderId);
        assertEquals(OrderSagaStep.CONFIRM_STOCK, sagaOf(orderId).currentStep);
        assertEquals(OrderStatus.PENDING, statusOf(orderId));

        orchestrator.onStockConfirmed(orderId);
        assertEquals(OrderSagaStep.COMPLETED, sagaOf(orderId).currentStep);
        assertEquals(OrderStatus.CONFIRMED, statusOf(orderId));
    }

    @Test
    @TestTransaction
    void rejectedReservationFailsTheSagaAndCancelsTheOrder() {
        Long orderId = persistOrderWithSaga();

        orchestrator.onStockRejected(orderId, "Insufficient stock for product prod-1");

        OrderSaga saga = sagaOf(orderId);
        assertEquals(OrderSagaStep.FAILED, saga.currentStep);
        assertEquals("Insufficient stock for product prod-1", saga.failureReason);
        assertEquals(OrderStatus.CANCELLED, statusOf(orderId));
    }

    @Test
    @TestTransaction
    void compensationReleasesStockAndCancelsTheOrder() {
        Long orderId = persistOrderWithSaga();

        orchestrator.compensate(orderId, "Cancelled by the customer");
        assertEquals(OrderSagaStep.COMPENSATING, sagaOf(orderId).currentStep);
        assertEquals(OrderStatus.PENDING, statusOf(orderId));

        orchestrator.onStockReleased(orderId);
        assertEquals(OrderSagaStep.COMPENSATED, sagaOf(orderId).currentStep);
        assertEquals(OrderStatus.CANCELLED, statusOf(orderId));
    }

    @Test
    @TestTransaction
    void everyOrderStatusChangeDrivenByTheSagaIsAnnouncedThroughTheOutbox() {
        Long orderId = persistOrderWithSaga();
        long eventsBefore = OutboxEvent.count("aggregateType", "Order");
        long commandsBefore = OutboxEvent.count("aggregateType", "OrderCommand");

        orchestrator.onStockReserved(orderId);
        orchestrator.onStockConfirmed(orderId);

        assertEquals(eventsBefore + 1, OutboxEvent.count("aggregateType", "Order"), "one status change announced");
        assertEquals(
                commandsBefore + 1,
                OutboxEvent.count("aggregateType", "OrderCommand"),
                "one ConfirmStockReservation command issued");

        OutboxEvent published = OutboxEvent.<OutboxEvent>find(
                        "aggregateId = ?1 and eventType = ?2", orderId.toString(), "OrderStatusChanged")
                .firstResult();
        assertTrue(published.payload.contains("CONFIRMED"));
    }

    @Test
    @TestTransaction
    void aStepOutOfOrderIsRejectedAndLeavesTheSagaWhereItWas() {
        Long orderId = persistOrderWithSaga();

        assertThrows(IllegalStateException.class, () -> orchestrator.onStockConfirmed(orderId));

        assertEquals(OrderSagaStep.RESERVE_STOCK, sagaOf(orderId).currentStep);
    }

    @Test
    @TestTransaction
    void aReplyForAnUnknownOrderIsRejected() {
        assertThrows(NoSuchElementException.class, () -> orchestrator.onStockReserved(-1L));
    }
}
