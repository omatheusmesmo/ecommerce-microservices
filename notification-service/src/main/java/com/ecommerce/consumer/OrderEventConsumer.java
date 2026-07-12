package com.ecommerce.consumer;

import com.ecommerce.event.OrderCreatedEvent;
import com.ecommerce.event.OrderStatusChangedEvent;
import com.ecommerce.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrderEventConsumer {

    private static final Logger LOG = Logger.getLogger(OrderEventConsumer.class);

    @Inject
    NotificationService notificationService;

    @Inject
    ObjectMapper objectMapper;

    @Incoming("order-events")
    @Blocking
    public void onOrderEvent(String message) {
        LOG.debugf("[KAFKA] Received raw message from outbox.event.Order: %s", message);

        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(message);
            if (jsonNode.isTextual()) {
                jsonNode = objectMapper.readTree(jsonNode.asText());
                LOG.debugf("[KAFKA] Detected double-encoded JSON, parsed inner content");
            }
        } catch (Exception e) {
            LOG.errorf(e, "[KAFKA] Failed to parse outbox.event.Order message: %s", message);
            throw new IllegalArgumentException("Failed to parse outbox.event.Order message: " + message, e);
        }

        if (jsonNode.has("oldStatus") && jsonNode.has("newStatus")) {
            OrderStatusChangedEvent event = parseOrThrow(jsonNode, OrderStatusChangedEvent.class, message);
            LOG.infof("[KAFKA] Successfully parsed OrderStatusChangedEvent: orderId=%d", event.orderId());
            handleOrderStatusChanged(event);
        } else if (jsonNode.has("items") && jsonNode.has("customerName")) {
            OrderCreatedEvent event = parseOrThrow(jsonNode, OrderCreatedEvent.class, message);
            LOG.infof("[KAFKA] Successfully parsed OrderCreatedEvent: orderId=%d", event.orderId());
            handleOrderCreated(event);
        } else {
            LOG.warnf("[KAFKA] Unknown event type in outbox.event.Order - payload: %s", message);
            throw new IllegalArgumentException("Unknown event type in outbox.event.Order - payload: " + message);
        }
    }

    private <T> T parseOrThrow(JsonNode jsonNode, Class<T> type, String message) {
        try {
            return objectMapper.treeToValue(jsonNode, type);
        } catch (Exception e) {
            LOG.errorf(e, "[KAFKA] Failed to map outbox.event.Order message to %s: %s", type.getSimpleName(), message);
            throw new IllegalArgumentException("Failed to map outbox.event.Order message to " + type.getSimpleName() + ": " + message, e);
        }
    }

    private void handleOrderCreated(OrderCreatedEvent event) {
        LOG.infof("[KAFKA] Processing OrderCreated event: orderId=%d, customer=%s, total=R$%.2f",
                event.orderId(), event.customerName(), event.totalAmount());

        try {
            notificationService.notifyOrderCreated(
                    event.orderId(),
                    event.customerEmail(),
                    event.customerName(),
                    event.totalAmount()
            );
        } catch (Exception e) {
            LOG.errorf(e, "[KAFKA] Failed to process OrderCreated event: orderId=%d", event.orderId());
            throw e;
        }

        LOG.infof("[KAFKA] OrderCreated event processed successfully: orderId=%d", event.orderId());
    }

    private void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        LOG.infof("[KAFKA] Processing OrderStatusChanged event: orderId=%d, %s → %s",
                event.orderId(), event.oldStatus(), event.newStatus());

        try {
            notificationService.notifyOrderStatusChanged(
                    event.orderId(),
                    event.customerEmail(),
                    event.oldStatus(),
                    event.newStatus()
            );
        } catch (Exception e) {
            LOG.errorf(e, "[KAFKA] Failed to process OrderStatusChanged event: orderId=%d", event.orderId());
            throw e;
        }

        LOG.infof("[KAFKA] OrderStatusChanged event processed successfully: orderId=%d", event.orderId());
    }
}
