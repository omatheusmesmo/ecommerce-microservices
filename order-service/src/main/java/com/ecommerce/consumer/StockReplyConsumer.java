package com.ecommerce.consumer;

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
 * Feeds the SAGA the outcome of each stock step. Replies are routed on the {@code eventType}
 * header and deduped on {@code eventId}, because Kafka redelivers and advancing a SAGA step
 * twice would corrupt it.
 */
@ApplicationScoped
public class StockReplyConsumer {

    public static final String STOCK_RESERVED = "StockReserved";
    public static final String STOCK_REJECTED = "StockRejected";
    public static final String STOCK_CONFIRMED = "StockConfirmed";
    public static final String STOCK_RELEASED = "StockReleased";

    private static final Logger LOG = Logger.getLogger(StockReplyConsumer.class);

    @Inject
    StockReplyHandler replyHandler;

    @Incoming("stock-replies")
    @Blocking
    public Uni<Void> onStockReply(Message<String> kafkaMessage) {
        String payload = kafkaMessage.getPayload();
        String eventType = headerOf(kafkaMessage, "eventType");
        String eventId = headerOf(kafkaMessage, "eventId");

        if (eventType == null || eventId == null) {
            LOG.errorf("[KAFKA] Stock reply missing eventType or eventId header: %s", payload);
            return Uni.createFrom()
                    .completionStage(kafkaMessage.nack(
                            new IllegalArgumentException("Stock reply missing eventType or eventId header")));
        }

        try {
            replyHandler.apply(eventId, eventType, payload);
        } catch (Exception e) {
            LOG.errorf(e, "[KAFKA] Failed to apply %s: %s", eventType, payload);
            return Uni.createFrom()
                    .completionStage(kafkaMessage.nack(
                            new IllegalStateException("Failed to apply " + eventType + ": " + payload, e)));
        }

        return Uni.createFrom().completionStage(kafkaMessage.ack());
    }

    private String headerOf(Message<String> kafkaMessage, String name) {
        IncomingKafkaRecordMetadata<?, ?> metadata = kafkaMessage
                .getMetadata(IncomingKafkaRecordMetadata.class)
                .orElseThrow(() -> new IllegalStateException("Missing Kafka record metadata"));
        Header header = metadata.getHeaders().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
