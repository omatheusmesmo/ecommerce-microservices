package com.ecommerce.saga;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderSaga;
import com.ecommerce.entity.OrderSagaStep;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.outbox.OrderCommandPublisher;
import com.ecommerce.outbox.OrderEventPublisher;
import com.ecommerce.repository.OrderRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.NoSuchElementException;
import org.jboss.logging.Logger;

/**
 * Drives the order lifecycle across services. It is the only component that knows the whole
 * flow: it issues the command for each step, and each callback records the reply, advances the
 * SAGA, and translates a terminal step into the order's business status.
 *
 * <p>Commands are written to the outbox inside the same transaction that advances the SAGA, so
 * the step and the command that carries it out can never disagree.
 */
@ApplicationScoped
public class OrderSagaOrchestrator {

    private static final Logger LOG = Logger.getLogger(OrderSagaOrchestrator.class);

    private final OrderRepository orderRepository;

    private final OrderEventPublisher eventPublisher;

    private final OrderCommandPublisher commandPublisher;

    @Inject
    public OrderSagaOrchestrator(
            OrderRepository orderRepository,
            OrderEventPublisher eventPublisher,
            OrderCommandPublisher commandPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.commandPublisher = commandPublisher;
    }

    /**
     * Opens the SAGA for a freshly created order and asks for its stock. Requires the caller's
     * transaction so the order, its SAGA and the first command are never persisted apart.
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public OrderSaga start(Order order) {
        OrderSaga saga = new OrderSaga(order.id);
        saga.persist();

        commandPublisher.reserveStock(order);

        LOG.infof("Order SAGA started for order %d at step %s", order.id, saga.currentStep);
        return saga;
    }

    @Transactional
    public void onStockReserved(Long orderId) {
        advance(orderId, OrderSagaStep.CONFIRM_STOCK);

        commandPublisher.confirmStockReservation(orderId);
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

    /**
     * Starts undoing a SAGA that already holds stock. The order stays where it is until the
     * release is acknowledged, because until then the reservation is still out there.
     */
    @Transactional
    public void compensate(Long orderId, String reason) {
        OrderSaga saga = advance(orderId, OrderSagaStep.COMPENSATING);
        saga.failureReason = reason;

        commandPublisher.releaseStock(orderId);
    }

    @Transactional
    public void onStockReleased(Long orderId) {
        advance(orderId, OrderSagaStep.COMPENSATED);

        transitionOrder(orderId, OrderStatus.CANCELLED);
    }

    public boolean holdsStock(Long orderId) {
        return OrderSaga.findByOrderId(orderId)
                .map(saga -> saga.currentStep == OrderSagaStep.RESERVE_STOCK
                        || saga.currentStep == OrderSagaStep.CONFIRM_STOCK)
                .orElse(false);
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
