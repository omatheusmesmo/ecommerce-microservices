package com.ecommerce.outbox;

import com.ecommerce.dto.OrderResponse;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.OutboxEvent;
import com.ecommerce.event.OrderCreatedEvent;
import com.ecommerce.event.OrderStatusChangedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

/**
 * Writes order domain events to the outbox table in the caller's transaction, so the state
 * change and the event it announces commit together.
 */
@ApplicationScoped
public class OrderEventPublisher {

    private static final Logger LOG = Logger.getLogger(OrderEventPublisher.class);

    private static final String AGGREGATE_TYPE = "Order";

    private final ObjectMapper objectMapper;

    @Inject
    public OrderEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void publishOrderCreated(OrderResponse order) {
        publish(order.id(), "OrderCreated", OrderCreatedEvent.from(order));
    }

    public void publishStatusChanged(Order order, OrderStatus oldStatus) {
        publish(
                order.id,
                "OrderStatusChanged",
                new OrderStatusChangedEvent(
                        order.id, oldStatus, order.status, order.customerEmail, LocalDateTime.now()));
    }

    private void publish(Long orderId, String eventType, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize " + eventType + " for order " + orderId, e);
        }

        new OutboxEvent(AGGREGATE_TYPE, orderId.toString(), eventType, json).persist();

        LOG.infof(
                "Event persisted to outbox: aggregate_type=%s, event_type=%s, aggregate_id=%d",
                AGGREGATE_TYPE, eventType, orderId);
    }
}
