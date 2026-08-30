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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Drives the order lifecycle across services. It is the only component that knows the whole
 * flow: it issues the command for each step, and each callback records the reply, advances the
 * SAGA, and translates a terminal step into the order's business status.
 *
 * <p>Commands are written to the outbox inside the same transaction that advances the SAGA, so
 * the step and the command that carries it out can never disagree.
 *
 * <p>A step whose reply never arrives is recovered by {@link #recoverTimedOut(Long)}, driven by
 * a scheduled sweep rather than by anything in the request path.
 */
@ApplicationScoped
public class OrderSagaOrchestrator {

    private static final Logger LOG = Logger.getLogger(OrderSagaOrchestrator.class);

    private final OrderRepository orderRepository;

    private final OrderEventPublisher eventPublisher;

    private final OrderCommandPublisher commandPublisher;

    private final Duration replyTimeout;

    private final int maxAttempts;

    @Inject
    public OrderSagaOrchestrator(
            OrderRepository orderRepository,
            OrderEventPublisher eventPublisher,
            OrderCommandPublisher commandPublisher,
            @ConfigProperty(name = "order.saga.reply-timeout") Duration replyTimeout,
            @ConfigProperty(name = "order.saga.max-attempts") int maxAttempts) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.commandPublisher = commandPublisher;
        this.replyTimeout = replyTimeout;
        this.maxAttempts = maxAttempts;
    }

    /**
     * Opens the SAGA for a freshly created order and asks for its stock. Requires the caller's
     * transaction so the order, its SAGA and the first command are never persisted apart.
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public OrderSaga start(Order order) {
        OrderSaga saga = new OrderSaga(order.id, replyTimeout);
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
        OrderSaga saga = OrderSaga.findByOrderId(orderId).orElseThrow(() -> noSaga(orderId));
        beginCompensation(saga, reason);
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

    /** Orders whose current step has been waiting for its reply past the deadline. */
    @Transactional
    public List<Long> ordersWithOverdueReplies(int limit) {
        return OrderSaga.findOverdueOrderIds(LocalDateTime.now(), limit);
    }

    /**
     * Re-sends the command of a step whose reply is overdue, or gives up on the SAGA once the
     * attempts are spent. Re-sending is safe because every stock command is keyed by order id
     * and replays its recorded outcome instead of touching stock twice.
     *
     * <p>The deadline is re-read inside this transaction: the sweep selected the SAGA moments
     * earlier, and the reply it was waiting for may have landed in between.
     */
    @Transactional
    public void recoverTimedOut(Long orderId) {
        OrderSaga saga = OrderSaga.findByOrderId(orderId).orElseThrow(() -> noSaga(orderId));

        if (!saga.isOverdue(LocalDateTime.now())) {
            LOG.debugf("Order SAGA for order %d answered before the sweep reached it", orderId);
            return;
        }

        if (saga.attempts >= maxAttempts) {
            giveUp(saga);
            return;
        }

        saga.recordAttempt(replyTimeout);
        reissueCommand(saga);

        LOG.warnf(
                "Order SAGA for order %d had no reply on %s; re-sent its command (attempt %d of %d)",
                orderId, saga.currentStep, saga.attempts, maxAttempts);
    }

    private void reissueCommand(OrderSaga saga) {
        switch (saga.currentStep) {
            case RESERVE_STOCK -> commandPublisher.reserveStock(requireOrder(saga.orderId));
            case CONFIRM_STOCK -> commandPublisher.confirmStockReservation(saga.orderId);
            case COMPENSATING -> commandPublisher.releaseStock(saga.orderId);
            case PROCESS_PAYMENT -> throw new IllegalStateException("The payment step has no command to re-send yet");
            case COMPLETED, COMPENSATED, FAILED, ABANDONED ->
                throw new IllegalStateException("Terminal step " + saga.currentStep + " cannot be waiting for a reply");
        }
    }

    /**
     * Ends a SAGA that ran out of attempts. Whether it can be undone is not a question this
     * method answers on its own: the state machine already records which steps have work that
     * is still reversible, so it decides here too. A step with no route to compensation is one
     * whose command may have been applied without the reply reaching us, and the SAGA stops
     * rather than assert an outcome it cannot know.
     */
    private void giveUp(OrderSaga saga) {
        String reason = "No reply on " + saga.currentStep + " after " + maxAttempts + " attempts";

        if (saga.currentStep.canTransitionTo(OrderSagaStep.COMPENSATING)) {
            LOG.warnf("Order SAGA for order %d gave up on %s; compensating", saga.orderId, saga.currentStep);
            beginCompensation(saga, reason);
            return;
        }

        OrderSagaStep abandonedAt = saga.currentStep;
        transition(saga, OrderSagaStep.ABANDONED);
        saga.failureReason = reason;
        LOG.errorf(
                "Order SAGA for order %d abandoned at %s after %d unanswered attempts;"
                        + " its stock outcome is unknown and needs a look",
                saga.orderId, abandonedAt, maxAttempts);
    }

    private void beginCompensation(OrderSaga saga, String reason) {
        transition(saga, OrderSagaStep.COMPENSATING);
        saga.failureReason = reason;

        commandPublisher.releaseStock(saga.orderId);
    }

    private OrderSaga advance(Long orderId, OrderSagaStep target) {
        OrderSaga saga = OrderSaga.findByOrderId(orderId).orElseThrow(() -> noSaga(orderId));
        return transition(saga, target);
    }

    private OrderSaga transition(OrderSaga saga, OrderSagaStep target) {
        OrderSagaStep previousStep = saga.currentStep;
        saga.advanceTo(target, replyTimeout);

        LOG.infof("Order SAGA for order %d advanced from %s to %s", saga.orderId, previousStep, target);
        return saga;
    }

    private void transitionOrder(Long orderId, OrderStatus newStatus) {
        Order order = requireOrder(orderId);

        OrderStatus oldStatus = order.status;
        order.transitionTo(newStatus);
        eventPublisher.publishStatusChanged(order, oldStatus);

        LOG.infof("Order %d status updated from %s to %s by the SAGA", orderId, oldStatus, newStatus);
    }

    private Order requireOrder(Long orderId) {
        return orderRepository
                .findByIdOptional(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + orderId));
    }

    private static NoSuchElementException noSaga(Long orderId) {
        return new NoSuchElementException("No SAGA found for order id: " + orderId);
    }
}
