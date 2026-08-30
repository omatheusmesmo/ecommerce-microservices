package com.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ReservationStatus;
import com.ecommerce.entity.StockReservation;
import com.ecommerce.event.ReserveStockCommand;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.valueobject.Money;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StockReservationServiceTest {

    private static final AtomicLong ORDER_IDS = new AtomicLong(System.currentTimeMillis());

    @Inject
    StockReservationService reservationService;

    @Inject
    ProductService productService;

    @Inject
    ProductRepository productRepository;

    @Inject
    CategoryRepository categoryRepository;

    private long nextOrderId() {
        return ORDER_IDS.incrementAndGet();
    }

    private String newProduct(int stock) {
        Category category = new Category("Reservation Category", null);
        categoryRepository.persist(category);
        Product product = new Product(
                "Reservation Product",
                "Description",
                new Money(new BigDecimal("100.00"), "BRL"),
                stock,
                category.id.toString());
        return productService.create(product).id.toString();
    }

    private Product reload(String productId) {
        return productRepository.findById(new ObjectId(productId));
    }

    private ReserveStockCommand reserveOf(long orderId, String productId, int quantity) {
        return new ReserveStockCommand(orderId, List.of(new ReserveStockCommand.Item(productId, quantity)));
    }

    @Test
    public void reservingHoldsStockWithoutRemovingIt() {
        String productId = newProduct(10);
        long orderId = nextOrderId();

        ReservationOutcome outcome = reservationService.reserve(reserveOf(orderId, productId, 4));

        assertInstanceOf(ReservationOutcome.Reserved.class, outcome);

        Product product = reload(productId);
        assertEquals(10, product.totalOnHand());
        assertEquals(4, product.totalReserved());
        assertEquals(6, product.totalAvailable());
    }

    @Test
    public void aSecondOrderCanOnlyTakeWhatTheFirstOneLeftAvailable() {
        String productId = newProduct(10);

        reservationService.reserve(reserveOf(nextOrderId(), productId, 8));
        ReservationOutcome second = reservationService.reserve(reserveOf(nextOrderId(), productId, 3));

        ReservationOutcome.Rejected rejected = assertInstanceOf(ReservationOutcome.Rejected.class, second);
        assertTrue(rejected.reason().contains(productId));

        Product product = reload(productId);
        assertEquals(8, product.totalReserved());
        assertEquals(2, product.totalAvailable());
    }

    @Test
    public void aShortfallOnOneItemGivesBackWhatTheEarlierItemsTook() {
        String plentiful = newProduct(10);
        String scarce = newProduct(1);
        long orderId = nextOrderId();

        ReservationOutcome outcome = reservationService.reserve(new ReserveStockCommand(
                orderId, List.of(new ReserveStockCommand.Item(plentiful, 5), new ReserveStockCommand.Item(scarce, 3))));

        assertInstanceOf(ReservationOutcome.Rejected.class, outcome);

        assertEquals(0, reload(plentiful).totalReserved());
        assertEquals(0, reload(scarce).totalReserved());
    }

    @Test
    public void repeatingAReserveCommandDoesNotReserveTwice() {
        String productId = newProduct(10);
        long orderId = nextOrderId();

        reservationService.reserve(reserveOf(orderId, productId, 4));
        ReservationOutcome replay = reservationService.reserve(reserveOf(orderId, productId, 4));

        assertInstanceOf(ReservationOutcome.Reserved.class, replay);
        assertEquals(4, reload(productId).totalReserved());
    }

    @Test
    public void confirmingTurnsTheReservationIntoASale() {
        String productId = newProduct(10);
        long orderId = nextOrderId();
        reservationService.reserve(reserveOf(orderId, productId, 4));

        StockReservation confirmed = reservationService.confirm(orderId);

        assertEquals(ReservationStatus.CONFIRMED, confirmed.status);

        Product product = reload(productId);
        assertEquals(6, product.totalOnHand());
        assertEquals(0, product.totalReserved());
        assertEquals(6, product.totalAvailable());
    }

    @Test
    public void releasingGivesTheStockBack() {
        String productId = newProduct(10);
        long orderId = nextOrderId();
        reservationService.reserve(reserveOf(orderId, productId, 4));

        StockReservation released = reservationService.release(orderId).orElseThrow();

        assertEquals(ReservationStatus.RELEASED, released.status);

        Product product = reload(productId);
        assertEquals(10, product.totalOnHand());
        assertEquals(0, product.totalReserved());
    }

    @Test
    public void confirmAndReleaseAreSafeToRepeat() {
        String productId = newProduct(10);
        long confirmedOrder = nextOrderId();
        long releasedOrder = nextOrderId();
        reservationService.reserve(reserveOf(confirmedOrder, productId, 2));
        reservationService.reserve(reserveOf(releasedOrder, productId, 2));

        reservationService.confirm(confirmedOrder);
        reservationService.confirm(confirmedOrder);
        reservationService.release(releasedOrder);
        reservationService.release(releasedOrder);

        Product product = reload(productId);
        assertEquals(8, product.totalOnHand());
        assertEquals(0, product.totalReserved());
    }

    @Test
    public void aConfirmedReservationCannotBeReleased() {
        String productId = newProduct(10);
        long orderId = nextOrderId();
        reservationService.reserve(reserveOf(orderId, productId, 4));
        reservationService.confirm(orderId);

        assertThrows(IllegalStateException.class, () -> reservationService.release(orderId));
    }

    @Test
    public void confirmingAnUnknownOrderIsRejected() {
        assertThrows(NoSuchElementException.class, () -> reservationService.confirm(nextOrderId()));
    }

    @Test
    public void releasingAnUnknownOrderSucceedsWithNothingToGiveBack() {
        assertTrue(reservationService.release(nextOrderId()).isEmpty());
    }

    @Test
    public void aRejectedReservationIsRememberedSoItIsNotRetriedBlindly() {
        String productId = newProduct(1);
        long orderId = nextOrderId();

        reservationService.reserve(reserveOf(orderId, productId, 5));
        StockReservation stored = StockReservation.findByOrderId(orderId).orElse(null);

        assertNotNull(stored);
        assertEquals(ReservationStatus.REJECTED, stored.status);
    }
}
