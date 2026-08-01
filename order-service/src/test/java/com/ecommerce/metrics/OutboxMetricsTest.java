package com.ecommerce.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ecommerce.entity.OutboxEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OutboxMetricsTest {

    @Inject
    OutboxMetrics outboxMetrics;

    @Inject
    MeterRegistry registry;

    private void clearOutbox() {
        QuarkusTransaction.requiringNew().run(() -> OutboxEvent.deleteAll());
    }

    @Test
    void gauges_reflectOutboxContents() {
        clearOutbox();

        QuarkusTransaction.requiringNew().run(() -> {
            OutboxEvent old = new OutboxEvent("Order", "1", "OrderCreated", "{}");
            old.createdAt = LocalDateTime.now().minusMinutes(5);
            old.persist();
            new OutboxEvent("Order", "2", "OrderCreated", "{}").persist();
        });

        outboxMetrics.refresh();

        assertEquals(2.0, registry.get("outbox.pending.events").gauge().value());
        assertTrue(
                registry.get("outbox.oldest.event.age.seconds").gauge().value() >= 300,
                "oldest event age should reflect the 5-minute-old row");
    }

    @Test
    void oldestAge_isZero_whenOutboxEmpty() {
        clearOutbox();

        outboxMetrics.refresh();

        assertEquals(0.0, registry.get("outbox.pending.events").gauge().value());
        assertEquals(
                0.0, registry.get("outbox.oldest.event.age.seconds").gauge().value());
    }
}
