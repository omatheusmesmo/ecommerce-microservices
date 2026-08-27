package com.ecommerce.outbox;

import com.ecommerce.dto.OrderResponse;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.event.OrderCreatedEvent;
import com.ecommerce.event.OrderStatusChangedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;

/**
 * Announces order domain events. These are broadcast: whoever subscribes to
 * {@code outbox.event.Order} reacts, and this service does not know or care who.
 */
@ApplicationScoped
public class OrderEventPublisher {

    static final String AGGREGATE_TYPE = "Order";

    private final OutboxWriter outboxWriter;

    @Inject
    public OrderEventPublisher(OutboxWriter outboxWriter) {
        this.outboxWriter = outboxWriter;
    }

    public void publishOrderCreated(OrderResponse order) {
        outboxWriter.write(AGGREGATE_TYPE, order.id(), "OrderCreated", OrderCreatedEvent.from(order));
    }

    public void publishStatusChanged(Order order, OrderStatus oldStatus) {
        outboxWriter.write(
                AGGREGATE_TYPE,
                order.id,
                "OrderStatusChanged",
                new OrderStatusChangedEvent(
                        order.id, oldStatus, order.status, order.customerEmail, LocalDateTime.now()));
    }
}
