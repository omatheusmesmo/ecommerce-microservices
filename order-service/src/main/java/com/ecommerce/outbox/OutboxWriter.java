package com.ecommerce.outbox;

import com.ecommerce.entity.OutboxEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Writes a row to the outbox table in the caller's transaction, so a state change and the
 * message announcing it commit together. The aggregate type decides the destination topic:
 * the Debezium EventRouter routes on it, and {@code OutboxDevPublisher} mirrors that outside
 * {@code %prod}.
 */
@ApplicationScoped
public class OutboxWriter {

    private static final Logger LOG = Logger.getLogger(OutboxWriter.class);

    private final ObjectMapper objectMapper;

    @Inject
    public OutboxWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(String aggregateType, Long aggregateId, String eventType, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize " + eventType + " for order " + aggregateId, e);
        }

        new OutboxEvent(aggregateType, aggregateId.toString(), eventType, json).persist();

        LOG.infof(
                "Persisted to outbox: aggregate_type=%s, event_type=%s, aggregate_id=%d",
                aggregateType, eventType, aggregateId);
    }
}
