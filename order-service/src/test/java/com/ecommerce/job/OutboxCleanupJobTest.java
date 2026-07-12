package com.ecommerce.job;

import com.ecommerce.entity.OutboxEvent;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class OutboxCleanupJobTest {

    @Inject
    OutboxCleanupJob outboxCleanupJob;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    void deleteEventsOlderThan_removesOnlyEventsPastRetention() {
        OutboxEvent stale = persistEventCreatedAt(LocalDateTime.now().minusDays(10));
        OutboxEvent fresh = persistEventCreatedAt(LocalDateTime.now().minusHours(1));

        int deletedCount = outboxCleanupJob.deleteEventsOlderThan(7);
        entityManager.clear();

        assertEquals(1, deletedCount);
        assertNull(OutboxEvent.findById(stale.id));
        assertNotNull(OutboxEvent.findById(fresh.id));
    }

    @Test
    @Transactional
    void deleteEventsOlderThan_returnsZero_whenNothingIsStale() {
        persistEventCreatedAt(LocalDateTime.now());

        int deletedCount = outboxCleanupJob.deleteEventsOlderThan(7);

        assertEquals(0, deletedCount);
    }

    private OutboxEvent persistEventCreatedAt(LocalDateTime createdAt) {
        OutboxEvent event = new OutboxEvent("Order", "1", "OrderCreated", "{}");
        event.createdAt = createdAt;
        event.persistAndFlush();
        return event;
    }
}
