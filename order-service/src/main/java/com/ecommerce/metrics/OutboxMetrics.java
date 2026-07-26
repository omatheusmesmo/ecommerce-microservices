package com.ecommerce.metrics;

import com.ecommerce.entity.OutboxEvent;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class OutboxMetrics {

    private final AtomicLong pendingEvents = new AtomicLong(0);
    private final AtomicLong oldestEventAgeSeconds = new AtomicLong(0);

    @Inject
    EntityManager entityManager;

    @Inject
    MeterRegistry registry;

    void onStart(@Observes StartupEvent event) {
        Gauge.builder("outbox.pending.events", pendingEvents, AtomicLong::get)
                .description("Events currently sitting in the outbox table")
                .register(registry);
        Gauge.builder("outbox.oldest.event.age.seconds", oldestEventAgeSeconds, AtomicLong::get)
                .description("Age of the oldest event still in the outbox table")
                .register(registry);
    }

    @Scheduled(every = "60s")
    @Transactional
    public void refresh() {
        pendingEvents.set(OutboxEvent.count());

        LocalDateTime oldest = entityManager
                .createQuery("SELECT MIN(o.createdAt) FROM OutboxEvent o", LocalDateTime.class)
                .getSingleResult();

        oldestEventAgeSeconds.set(
                oldest == null ? 0 : Duration.between(oldest, LocalDateTime.now()).getSeconds());
    }
}
