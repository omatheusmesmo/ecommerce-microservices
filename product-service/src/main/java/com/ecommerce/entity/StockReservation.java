package com.ecommerce.entity;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.common.MongoEntity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bson.codecs.pojo.annotations.BsonId;

/**
 * What the product-service has set aside for one order. Keyed by the order id so
 * that a redelivered command finds the existing document instead of reserving
 * twice: with MongoDB running standalone there is no multi-document transaction
 * to lean on, so the document itself is the idempotency record.
 */
@MongoEntity(collection = "stock_reservations")
public class StockReservation extends PanacheMongoEntityBase {

    @BsonId
    public String id;

    public List<ReservedItem> items = new ArrayList<>();

    public ReservationStatus status = ReservationStatus.RESERVED;

    public String rejectionReason;

    public LocalDateTime createdAt = LocalDateTime.now();

    public LocalDateTime updatedAt = LocalDateTime.now();

    public StockReservation() {}

    public StockReservation(long orderId, List<ReservedItem> items) {
        this.id = idOf(orderId);
        this.items = new ArrayList<>(items);
    }

    public static String idOf(long orderId) {
        return String.valueOf(orderId);
    }

    public static Optional<StockReservation> findByOrderId(long orderId) {
        return Optional.ofNullable(findById(idOf(orderId)));
    }

    public void markRejected(String reason) {
        this.status = ReservationStatus.REJECTED;
        this.rejectionReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void markStatus(ReservationStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public record ReservedItem(String productId, int quantity) {}
}
