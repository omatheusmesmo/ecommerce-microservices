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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
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

    public OrderSaga(Long orderId, Duration replyTimeout) {
        this(orderId);
        this.deadlineAt = LocalDateTime.now().plus(replyTimeout);
    }

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
     * Order ids of the SAGAs whose reply is overdue, oldest deadline first. Returns ids rather
     * than entities so the caller can take each one in its own transaction: a SAGA that loses
     * the optimistic-lock race against a late reply must not roll back the rest of the sweep.
     */
    public static List<Long> findOverdueOrderIds(LocalDateTime threshold, int limit) {
        return getEntityManager()
                .createQuery(
                        "SELECT s.orderId FROM OrderSaga s WHERE s.deadlineAt IS NOT NULL"
                                + " AND s.deadlineAt < :threshold ORDER BY s.deadlineAt",
                        Long.class)
                .setParameter("threshold", threshold)
                .setMaxResults(limit)
                .getResultList();
    }

    /**
     * Moves the SAGA to {@code target}, rejecting any step the state machine does not allow.
     * Resets the per-step bookkeeping, since attempts and deadline belong to the step in progress.
     *
     * <p>A non-terminal step always has a command waiting to be answered, so it always gets a
     * deadline, and a terminal one never does. Setting it here rather than at each call site is
     * what makes {@code deadlineAt != null} mean "a reply is outstanding" for every step,
     * including the ones added later.
     */
    public void advanceTo(OrderSagaStep target, Duration replyTimeout) {
        if (!currentStep.canTransitionTo(target)) {
            throw new IllegalStateException("Cannot advance order SAGA from " + currentStep + " to " + target);
        }
        this.currentStep = target;
        this.attempts = 0;
        this.deadlineAt = target.isTerminal() ? null : LocalDateTime.now().plus(replyTimeout);
    }

    /** Counts one re-send of the current step's command and gives the reply a fresh deadline. */
    public void recordAttempt(Duration replyTimeout) {
        this.attempts++;
        this.deadlineAt = LocalDateTime.now().plus(replyTimeout);
    }

    public boolean isOverdue(LocalDateTime now) {
        return deadlineAt != null && deadlineAt.isBefore(now);
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
