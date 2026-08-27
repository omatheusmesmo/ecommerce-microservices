package com.ecommerce.consumer;

import com.ecommerce.entity.ProcessedOrderEvent;
import com.ecommerce.event.OrderCreatedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.common.header.Header;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

/**
 * Observes the order event stream. Stock is no longer driven from here: it moves only through
 * the SAGA's addressed commands on {@code outbox.event.OrderCommand}, so that a reservation can
 * be held, confirmed or released as one accountable decision instead of a side effect of an
 * event that several services happen to see.
 */
@ApplicationScoped
public class OrderEventConsumer {

    private static final Logger LOG = Logger.getLogger(OrderEventConsumer.class);

    @Inject
    ObjectMapper objectMapper;

    @Incoming("order-events")
    @Blocking
    public Uni<Void> onOrderEvent(Message<String> kafkaMessage) {
        String message = kafkaMessage.getPayload();
        LOG.debugf("[KAFKA] Received message from Order.events: %s", message);

        String eventKey = eventKeyOf(kafkaMessage);
        if (isAlreadyProcessed(eventKey)) {
            LOG.infof("[KAFKA] Skipping already-processed event: %s", eventKey);
            return Uni.createFrom().completionStage(kafkaMessage.ack());
        }

        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(message);
            if (jsonNode.isTextual()) {
                jsonNode = objectMapper.readTree(jsonNode.asText());
                LOG.debugf("[KAFKA] Detected double-encoded JSON, parsed inner content");
            }
        } catch (Exception e) {
            LOG.errorf(e, "[KAFKA] Failed to parse Order.events message: %s", message);
            return Uni.createFrom()
                    .completionStage(kafkaMessage.nack(
                            new IllegalArgumentException("Failed to parse Order.events message: " + message, e)));
        }

        if (jsonNode.has("items") && jsonNode.has("customerName")) {
            try {
                handleOrderCreated(objectMapper.treeToValue(jsonNode, OrderCreatedEvent.class));
            } catch (Exception e) {
                LOG.errorf(e, "[KAFKA] Failed to process Order.events message: %s", message);
                return Uni.createFrom()
                        .completionStage(kafkaMessage.nack(
                                new IllegalStateException("Failed to process Order.events message: " + message, e)));
            }
        } else {
            LOG.debugf("[KAFKA] Ignoring non-OrderCreated event from Order.events");
        }

        markProcessed(eventKey);
        return Uni.createFrom().completionStage(kafkaMessage.ack());
    }

    private String eventKeyOf(Message<String> kafkaMessage) {
        IncomingKafkaRecordMetadata<?, ?> metadata = kafkaMessage
                .getMetadata(IncomingKafkaRecordMetadata.class)
                .orElseThrow(() -> new IllegalStateException("Missing Kafka record metadata"));
        Header eventId = metadata.getHeaders().lastHeader("eventId");
        if (eventId == null) {
            throw new IllegalStateException("Missing eventId header on order event");
        }
        return new String(eventId.value(), StandardCharsets.UTF_8);
    }

    private boolean isAlreadyProcessed(String eventKey) {
        return ProcessedOrderEvent.findById(eventKey) != null;
    }

    private void markProcessed(String eventKey) {
        try {
            new ProcessedOrderEvent(eventKey).persist();
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() != ErrorCategory.DUPLICATE_KEY) {
                LOG.errorf(e, "[KAFKA] Failed to mark event as processed: %s", eventKey);
                throw e;
            }
        }
    }

    private void handleOrderCreated(OrderCreatedEvent event) {
        LOG.infof(
                "[KAFKA] Observed OrderCreated: orderId=%d, customer=%s, total=%s",
                event.orderId(), event.customerName(), event.totalAmount());
    }
}
