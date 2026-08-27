package com.ecommerce.messaging;

import com.ecommerce.event.StockConfirmedEvent;
import com.ecommerce.event.StockRejectedEvent;
import com.ecommerce.event.StockReleasedEvent;
import com.ecommerce.event.StockReservedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

/**
 * Answers the order SAGA. Replies carry the same {@code eventId} and {@code eventType}
 * headers the outbox puts on order events, so the consuming side can dedupe and route
 * on headers rather than guessing from the JSON shape.
 */
@ApplicationScoped
public class StockReplyProducer {

    public static final String STOCK_RESERVED = "StockReserved";
    public static final String STOCK_REJECTED = "StockRejected";
    public static final String STOCK_CONFIRMED = "StockConfirmed";
    public static final String STOCK_RELEASED = "StockReleased";

    private static final Logger LOG = Logger.getLogger(StockReplyProducer.class);

    private final ObjectMapper objectMapper;

    @Channel("stock-replies")
    Emitter<String> emitter;

    @Inject
    public StockReplyProducer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void publishStockReserved(long orderId) {
        publish(orderId, STOCK_RESERVED, new StockReservedEvent(orderId, LocalDateTime.now()));
    }

    public void publishStockRejected(long orderId, String reason) {
        publish(orderId, STOCK_REJECTED, new StockRejectedEvent(orderId, reason, LocalDateTime.now()));
    }

    public void publishStockConfirmed(long orderId) {
        publish(orderId, STOCK_CONFIRMED, new StockConfirmedEvent(orderId, LocalDateTime.now()));
    }

    public void publishStockReleased(long orderId) {
        publish(orderId, STOCK_RELEASED, new StockReleasedEvent(orderId, LocalDateTime.now()));
    }

    private void publish(long orderId, String eventType, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize " + eventType + " for order " + orderId, e);
        }

        emitter.send(Message.of(json)
                .addMetadata(OutgoingKafkaRecordMetadata.<String>builder()
                        .withKey(String.valueOf(orderId))
                        .addHeaders(header("eventId", UUID.randomUUID().toString()), header("eventType", eventType))
                        .build()));

        LOG.infof("Published %s for order %d", eventType, orderId);
    }

    private RecordHeader header(String name, String value) {
        return new RecordHeader(name, value.getBytes(StandardCharsets.UTF_8));
    }
}
