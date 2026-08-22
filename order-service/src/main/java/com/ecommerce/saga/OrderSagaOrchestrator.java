package com.ecommerce.saga;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderSaga;
import com.ecommerce.entity.OrderSagaStep;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.outbox.OrderEventPublisher;
import com.ecommerce.repository.OrderRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.NoSuchElementException;
import org.jboss.logging.Logger;

/**
 * Drives the order lifecycle across services. It is the only component that knows the whole
 * flow: each callback records the outcome of a step, advances the SAGA, and translates a
 * terminal step into the order's business status.
 */
@ApplicationScoped
public class OrderSagaOrchestrator {

    private static final Logger LOG = Logger.getLogger(OrderSagaOrchestrator.class);

    private final OrderRepository orderRepository;

    private final OrderEventPublisher eventPublisher;

    @Inject
    public OrderSagaOrchestrator(OrderRepository orderRepository, OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Opens the SAGA for a freshly created order. Requires the caller's transaction so the order
     * and its SAGA are never persisted apart.
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public OrderSaga start(Long orderId) {
        OrderSaga saga = new OrderSaga(orderId);
        saga.persist();

        LOG.infof("Order SAGA started for order %d at step %s", orderId, saga.currentStep);
        return saga;
    }

    @Transactional
    public void onStockReserved(Long orderId) {
        advance(orderId, OrderSagaStep.CONFIRM_STOCK);
    }

    @Transactional
    public void onStockRejected(Long orderId, String reason) {
        OrderSaga saga = advance(orderId, OrderSagaStep.FAILED);
        saga.failureReason = reason;

        transitionOrder(orderId, OrderStatus.CANCELLED);
    }

    @Transactional
    public void onStockConfirmed(Long orderId) {
        advance(orderId, OrderSagaStep.COMPLETED);

        transitionOrder(orderId, OrderStatus.CONFIRMED);
    }

    @Transactional
    public void onCompensationStarted(Long orderId, String reason) {
        OrderSaga saga = advance(orderId, OrderSagaStep.COMPENSATING);
        saga.failureReason = reason;
    }

    @Transactional
    public void onStockReleased(Long orderId) {
        advance(orderId, OrderSagaStep.COMPENSATED);

        transitionOrder(orderId, OrderStatus.CANCELLED);
    }

    private OrderSaga advance(Long orderId, OrderSagaStep target) {
        OrderSaga saga = OrderSaga.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("No SAGA found for order id: " + orderId));

        OrderSagaStep previousStep = saga.currentStep;
        saga.advanceTo(target);

        LOG.infof("Order SAGA for order %d advanced from %s to %s", orderId, previousStep, target);
        return saga;
    }

    private void transitionOrder(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository
                .findByIdOptional(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + orderId));

        OrderStatus oldStatus = order.status;
        order.transitionTo(newStatus);
        eventPublisher.publishStatusChanged(order, oldStatus);

        LOG.infof("Order %d status updated from %s to %s by the SAGA", orderId, oldStatus, newStatus);
    }
}
