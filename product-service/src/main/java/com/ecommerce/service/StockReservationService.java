package com.ecommerce.service;

import com.ecommerce.entity.ReservationStatus;
import com.ecommerce.entity.StockReservation;
import com.ecommerce.entity.StockReservation.ReservedItem;
import com.ecommerce.event.ReserveStockCommand;
import com.ecommerce.repository.ProductRepository;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.jboss.logging.Logger;

/**
 * Applies the stock half of the order SAGA. Every operation is keyed by order id and
 * safe to repeat, because the reservation document records the outcome: a redelivered
 * command finds the stored status and replays it instead of touching stock again.
 *
 * <p>MongoDB runs standalone here, so there is no transaction spanning the reservation
 * document and the product documents. Reserving several products is therefore a sequence
 * of atomic single-document updates, and a shortfall on any of them rolls back the ones
 * already taken before reporting the rejection.
 */
@ApplicationScoped
public class StockReservationService {

    private static final Logger LOG = Logger.getLogger(StockReservationService.class);

    private final ProductRepository productRepository;

    @Inject
    public StockReservationService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ReservationOutcome reserve(ReserveStockCommand command) {
        long orderId = command.orderId();
        List<ReservedItem> requested = command.items().stream()
                .map(item -> new ReservedItem(item.productId(), item.quantity()))
                .toList();

        StockReservation reservation = new StockReservation(orderId, requested);
        try {
            reservation.persist();
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() != ErrorCategory.DUPLICATE_KEY) {
                throw e;
            }
            StockReservation decided = require(orderId);
            LOG.infof("Stock reservation for order %d already decided as %s, replaying it", orderId, decided.status);
            return outcomeOf(orderId, decided);
        }

        List<ReservedItem> taken = new ArrayList<>();
        for (ReservedItem item : requested) {
            if (productRepository.reserveStock(item.productId(), item.quantity()) == 0) {
                giveBack(taken);
                String reason = "Insufficient stock for product " + item.productId();
                reservation.markRejected(reason);
                reservation.update();
                LOG.infof("Stock reservation rejected for order %d: %s", orderId, reason);
                return new ReservationOutcome.Rejected(orderId, reason);
            }
            taken.add(item);
        }

        LOG.infof("Stock reserved for order %d across %d product(s)", orderId, taken.size());
        return new ReservationOutcome.Reserved(orderId);
    }

    private ReservationOutcome outcomeOf(long orderId, StockReservation reservation) {
        return switch (reservation.status) {
            case REJECTED -> new ReservationOutcome.Rejected(orderId, reservation.rejectionReason);
            case RESERVED, CONFIRMED, RELEASED -> new ReservationOutcome.Reserved(orderId);
        };
    }

    public StockReservation confirm(long orderId) {
        StockReservation reservation = require(orderId);

        return switch (reservation.status) {
            case CONFIRMED -> reservation;
            case RESERVED -> applyConfirmation(orderId, reservation);
            case REJECTED, RELEASED ->
                throw new IllegalStateException(
                        "Cannot confirm a " + reservation.status + " reservation for order " + orderId);
        };
    }

    private StockReservation applyConfirmation(long orderId, StockReservation reservation) {
        for (ReservedItem item : reservation.items) {
            if (productRepository.confirmReservation(item.productId(), item.quantity()) == 0) {
                throw new IllegalStateException(
                        "Product " + item.productId() + " no longer holds the reservation for order " + orderId);
            }
        }

        reservation.markStatus(ReservationStatus.CONFIRMED);
        reservation.update();
        LOG.infof("Stock reservation confirmed for order %d", orderId);
        return reservation;
    }

    public StockReservation release(long orderId) {
        StockReservation reservation = require(orderId);

        return switch (reservation.status) {
            case RELEASED, REJECTED -> reservation;
            case RESERVED -> applyRelease(orderId, reservation);
            case CONFIRMED ->
                throw new IllegalStateException("Cannot release a CONFIRMED reservation for order " + orderId);
        };
    }

    private StockReservation applyRelease(long orderId, StockReservation reservation) {
        giveBack(reservation.items);

        reservation.markStatus(ReservationStatus.RELEASED);
        reservation.update();
        LOG.infof("Stock reservation released for order %d", orderId);
        return reservation;
    }

    private StockReservation require(long orderId) {
        return StockReservation.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("No stock reservation for order " + orderId));
    }

    private void giveBack(List<ReservedItem> items) {
        for (ReservedItem item : items) {
            if (productRepository.releaseReservation(item.productId(), item.quantity()) == 0) {
                LOG.warnf(
                        "Nothing to give back on product %s for %d unit(s); reservation already gone",
                        item.productId(), item.quantity());
            }
        }
    }
}
