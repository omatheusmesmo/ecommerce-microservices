package com.ecommerce.job;

import com.ecommerce.entity.OutboxEvent;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.panache.common.Sort;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

/**
 * Stand-in for the Debezium connector outside %prod, where nothing captures the
 * outbox table via CDC: polls unpublished rows and emits them to the same topic
 * Debezium's Outbox Event Router would use, then deletes them.
 */
@ApplicationScoped
@UnlessBuildProfile("prod")
public class OutboxDevPublisher {

    private static final Logger LOG = Logger.getLogger(OutboxDevPublisher.class);

    @Channel("outbox-order-events")
    Emitter<String> emitter;

    @Scheduled(every = "2s")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = OutboxEvent.listAll(Sort.by("id"));
        for (OutboxEvent event : events) {
            emitter.send(Message.of(event.payload)
                    .addMetadata(OutgoingKafkaRecordMetadata.<String>builder()
                            .withKey(event.aggregateId)
                            .addHeaders(new RecordHeader(
                                    "eventId", event.eventId.toString().getBytes(StandardCharsets.UTF_8)))
                            .build()));
            event.delete();
        }
        if (!events.isEmpty()) {
            LOG.infof("Published %d outbox event(s) to Kafka", events.size());
        }
    }
}
