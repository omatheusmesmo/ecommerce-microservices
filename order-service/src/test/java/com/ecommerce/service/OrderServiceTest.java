package com.ecommerce.service;

import com.ecommerce.dto.CreateOrderRequest;
import com.ecommerce.dto.OrderItemRequest;
import com.ecommerce.dto.OrderResponse;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.OutboxEvent;
import com.ecommerce.repository.OrderRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class OrderServiceTest {

    @Inject
    OrderService orderService;

    @Inject
    OrderRepository orderRepository;

    private CreateOrderRequest createOrderRequest() {
        return new CreateOrderRequest(
                "Jane Doe",
                "jane@example.com",
                List.of(new OrderItemRequest("prod-1", "Gaming Chair", 2, new BigDecimal("100.00"))),
                new BigDecimal("15.00"));
    }

    private Long persistOrder(OrderStatus status) {
        Order order = new Order("Jane Doe", "jane@example.com");
        order.status = status;
        orderRepository.persist(order);
        return order.id;
    }

    @Test
    @TestTransaction
    void createOrder_persistsOrderAndWritesMatchingOutboxEventAtomically() {
        long outboxCountBefore = OutboxEvent.count();

        OrderResponse response = orderService.createOrder(createOrderRequest());

        assertNotNull(response.id());
        assertEquals(new BigDecimal("215.00"), response.totalAmount());
        assertTrue(orderRepository.findByIdOptional(response.id()).isPresent());

        assertEquals(outboxCountBefore + 1, OutboxEvent.count());
        OutboxEvent event = OutboxEvent
                .find("aggregateType = ?1 and aggregateId = ?2", "Order", response.id().toString())
                .firstResult();
        assertNotNull(event);
        assertEquals("OrderCreated", event.eventType);
    }

    @Test
    @TestTransaction
    void findById_notFound_throwsNoSuchElementException() {
        assertThrows(NoSuchElementException.class, () -> orderService.findById(Long.MAX_VALUE));
    }

    @Test
    @TestTransaction
    void cancelOrder_pendingOrder_transitionsToCancelled() {
        Long id = persistOrder(OrderStatus.PENDING);

        orderService.cancelOrder(id);

        assertEquals(OrderStatus.CANCELLED, orderRepository.findById(id).status);
    }

    @Test
    @TestTransaction
    void cancelOrder_deliveredOrder_throwsIllegalStateException() {
        Long id = persistOrder(OrderStatus.DELIVERED);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.cancelOrder(id));
        assertEquals("Cannot cancel a delivered order", ex.getMessage());
        assertEquals(OrderStatus.DELIVERED, orderRepository.findById(id).status);
    }

    @Test
    @TestTransaction
    void cancelOrder_alreadyCancelledOrder_throwsIllegalStateException() {
        Long id = persistOrder(OrderStatus.CANCELLED);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.cancelOrder(id));
        assertEquals("Order is already cancelled", ex.getMessage());
    }

    @Test
    @TestTransaction
    void cancelOrder_notFound_throwsNoSuchElementException() {
        assertThrows(NoSuchElementException.class, () -> orderService.cancelOrder(Long.MAX_VALUE));
    }

    @Test
    @TestTransaction
    void updateStatus_updatesOrderAndReturnsNewStatus() {
        Long id = persistOrder(OrderStatus.PENDING);

        OrderResponse response = orderService.updateStatus(id, OrderStatus.CONFIRMED);

        assertEquals(OrderStatus.CONFIRMED, response.status());
        assertEquals(OrderStatus.CONFIRMED, orderRepository.findById(id).status);
    }

    @Test
    @TestTransaction
    void updateStatus_notFound_throwsNoSuchElementException() {
        assertThrows(NoSuchElementException.class,
                () -> orderService.updateStatus(Long.MAX_VALUE, OrderStatus.CONFIRMED));
    }
}
