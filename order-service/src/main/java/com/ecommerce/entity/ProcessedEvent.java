package com.ecommerce.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * One row per message this service has already acted on, keyed by the business eventId the
 * producer stamps on the record. Kafka redelivers, and a reply that advances the SAGA is not
 * safe to apply twice.
 */
@Entity
@Table(name = "processed_event")
public class ProcessedEvent extends PanacheEntityBase {

    @Id
    @Column(name = "event_id", length = 255)
    public String eventId;

    @Column(nullable = false)
    public LocalDateTime processedAt = LocalDateTime.now();

    public ProcessedEvent() {}

    public ProcessedEvent(String eventId) {
        this.eventId = eventId;
    }

    public static boolean alreadyProcessed(String eventId) {
        return findById(eventId) != null;
    }
}
