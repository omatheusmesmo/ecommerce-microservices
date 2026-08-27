package com.ecommerce.consumer;

import com.ecommerce.event.ConfirmStockReservationCommand;
import com.ecommerce.event.ReleaseStockCommand;
import com.ecommerce.event.ReserveStockCommand;
import com.ecommerce.messaging.StockReplyProducer;
import com.ecommerce.service.ReservationOutcome;
import com.ecommerce.service.StockReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Executes the stock commands the order SAGA addresses to this service and answers each
 * one. Commands are routed on the {@code eventType} header rather than the payload shape,
 * so adding a command never depends on the new payload happening to look different from
 * the existing ones.
 */
@ApplicationScoped
public class StockCommandConsumer {

    public static final String RESERVE_STOCK = "ReserveStock";
    public static final String CONFIRM_STOCK_RESERVATION = "ConfirmStockReservation";
    public static final String RELEASE_STOCK = "ReleaseStock";

    private static final Logger LOG = Logger.getLogger(StockCommandConsumer.class);

    @Inject
    StockReservationService reservationService;

    @Inject
    StockReplyProducer replyProducer;

    @Inject
    ObjectMapper objectMapper;

    @Incoming("stock-commands")
    @Blocking
    public Uni<Void> onStockCommand(Message<String> kafkaMessage) {
        String payload = kafkaMessage.getPayload();
        String eventType = headerOf(kafkaMessage, "eventType");

        if (eventType == null) {
            LOG.errorf("[KAFKA] Stock command without an eventType header: %s", payload);
            return Uni.createFrom()
                    .completionStage(kafkaMessage.nack(
                            new IllegalArgumentException("Stock command without an eventType header")));
        }

        LOG.debugf("[KAFKA] Received %s: %s", eventType, payload);

        try {
            switch (eventType) {
                case RESERVE_STOCK -> handleReserve(objectMapper.readValue(payload, ReserveStockCommand.class));
                case CONFIRM_STOCK_RESERVATION ->
                    handleConfirm(objectMapper.readValue(payload, ConfirmStockReservationCommand.class));
                case RELEASE_STOCK -> handleRelease(objectMapper.readValue(payload, ReleaseStockCommand.class));
                default -> LOG.debugf("[KAFKA] Ignoring unknown stock command: %s", eventType);
            }
        } catch (Exception e) {
            LOG.errorf(e, "[KAFKA] Failed to process %s: %s", eventType, payload);
            return Uni.createFrom()
                    .completionStage(kafkaMessage.nack(
                            new IllegalStateException("Failed to process " + eventType + ": " + payload, e)));
        }

        return Uni.createFrom().completionStage(kafkaMessage.ack());
    }

    private void handleReserve(ReserveStockCommand command) {
        switch (reservationService.reserve(command)) {
            case ReservationOutcome.Reserved(long orderId) -> replyProducer.publishStockReserved(orderId);
            case ReservationOutcome.Rejected(long orderId, String reason) ->
                replyProducer.publishStockRejected(orderId, reason);
        }
    }

    private void handleConfirm(ConfirmStockReservationCommand command) {
        reservationService.confirm(command.orderId());
        replyProducer.publishStockConfirmed(command.orderId());
    }

    private void handleRelease(ReleaseStockCommand command) {
        reservationService.release(command.orderId());
        replyProducer.publishStockReleased(command.orderId());
    }

    private String headerOf(Message<String> kafkaMessage, String name) {
        IncomingKafkaRecordMetadata<?, ?> metadata = kafkaMessage
                .getMetadata(IncomingKafkaRecordMetadata.class)
                .orElseThrow(() -> new IllegalStateException("Missing Kafka record metadata"));
        Header header = metadata.getHeaders().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
