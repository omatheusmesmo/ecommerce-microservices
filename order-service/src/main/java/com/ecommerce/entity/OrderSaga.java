package com.ecommerce.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Coordination state of one order's SAGA. Holds a plain {@code orderId} rather than an
 * association so the SAGA stays a separate aggregate from {@link Order}, which keeps
 * retry and deadline bookkeeping out of the business entity.
 */
@Entity
@Table(name = "order_saga")
@SequenceGenerator(name = "order_saga_seq_gen", sequenceName = "order_saga_seq", allocationSize = 50)
public class OrderSaga extends PanacheEntity {

    @NotNull
    @Column(nullable = false, unique = true)
    public Long orderId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    public OrderSagaStep currentStep;

    @Column(nullable = false)
    public int attempts;

    public LocalDateTime deadlineAt;

    public String failureReason;

    @Column(nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(nullable = false)
    public LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    public Long version;

    public OrderSaga() {
        this.currentStep = OrderSagaStep.RESERVE_STOCK;
        this.attempts = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public OrderSaga(Long orderId) {
        this();
        this.orderId = orderId;
    }

    public static Optional<OrderSaga> findByOrderId(Long orderId) {
        return find("orderId", orderId).firstResultOptional();
    }

    /**
     * Moves the SAGA to {@code target}, rejecting any step the state machine does not allow.
     * Resets the per-step bookkeeping, since attempts and deadline belong to the step in progress.
     */
    public void advanceTo(OrderSagaStep target) {
        if (!currentStep.canTransitionTo(target)) {
            throw new IllegalStateException("Cannot advance order SAGA from " + currentStep + " to " + target);
        }
        this.currentStep = target;
        this.attempts = 0;
        this.deadlineAt = null;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
