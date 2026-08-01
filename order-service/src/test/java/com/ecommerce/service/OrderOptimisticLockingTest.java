package com.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.repository.OrderRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OrderOptimisticLockingTest {

    @Inject
    OrderService orderService;

    @Inject
    OrderRepository orderRepository;

    private Long persistPendingOrder() {
        return QuarkusTransaction.requiringNew().call(() -> {
            Order order = new Order("Jane Doe", "jane@example.com");
            orderRepository.persist(order);
            return order.id;
        });
    }

    @Test
    void updateStatus_incrementsVersion() {
        Long id = persistPendingOrder();
        Long before = QuarkusTransaction.requiringNew().call(() -> orderRepository.findById(id).version);

        orderService.updateStatus(id, OrderStatus.CONFIRMED);

        Long after = QuarkusTransaction.requiringNew().call(() -> orderRepository.findById(id).version);
        assertEquals(before + 1, after);
    }

    @Test
    void staleWrite_afterConcurrentUpdate_throwsOptimisticLock() {
        Long id = persistPendingOrder();

        Order stale = QuarkusTransaction.requiringNew().call(() -> {
            Order order = orderRepository.findById(id);
            orderRepository.getEntityManager().detach(order);
            return order;
        });

        orderService.updateStatus(id, OrderStatus.CONFIRMED);

        assertThrows(
                OptimisticLockException.class,
                () -> QuarkusTransaction.requiringNew().run(() -> {
                    stale.status = OrderStatus.SHIPPED;
                    orderRepository.getEntityManager().merge(stale);
                    orderRepository.getEntityManager().flush();
                }));
    }
}
