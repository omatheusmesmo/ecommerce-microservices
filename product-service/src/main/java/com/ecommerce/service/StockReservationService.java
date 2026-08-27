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

    public StockReservation reserve(ReserveStockCommand command) {
        List<ReservedItem> requested = command.items().stream()
                .map(item -> new ReservedItem(item.productId(), item.quantity()))
                .toList();

        StockReservation reservation = new StockReservation(command.orderId(), requested);
        try {
            reservation.persist();
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() != ErrorCategory.DUPLICATE_KEY) {
                throw e;
            }
            StockReservation existing = StockReservation.findByOrderId(command.orderId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Reservation vanished after a duplicate key on order " + command.orderId()));
            LOG.infof(
                    "Stock reservation for order %d already decided as %s, replaying it",
                    command.orderId(), existing.status);
            return existing;
        }

        List<ReservedItem> taken = new ArrayList<>();
        for (ReservedItem item : requested) {
            if (productRepository.reserveStock(item.productId(), item.quantity()) == 0) {
                giveBack(taken);
                String reason = "Insufficient stock for product " + item.productId();
                reservation.markRejected(reason);
                reservation.update();
                LOG.infof("Stock reservation rejected for order %d: %s", command.orderId(), reason);
                return reservation;
            }
            taken.add(item);
        }

        LOG.infof("Stock reserved for order %d across %d product(s)", command.orderId(), taken.size());
        return reservation;
    }

    public StockReservation confirm(long orderId) {
        StockReservation reservation = require(orderId);

        if (reservation.status == ReservationStatus.CONFIRMED) {
            return reservation;
        }
        if (reservation.status != ReservationStatus.RESERVED) {
            throw new IllegalStateException(
                    "Cannot confirm a " + reservation.status + " reservation for order " + orderId);
        }

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

        if (reservation.status == ReservationStatus.RELEASED || reservation.status == ReservationStatus.REJECTED) {
            return reservation;
        }
        if (reservation.status != ReservationStatus.RESERVED) {
            throw new IllegalStateException(
                    "Cannot release a " + reservation.status + " reservation for order " + orderId);
        }

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
