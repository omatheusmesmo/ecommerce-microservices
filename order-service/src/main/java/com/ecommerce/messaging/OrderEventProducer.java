package com.ecommerce.messaging;

import com.ecommerce.event.OrderCreatedEvent;
import com.ecommerce.event.OrderStatusChangedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrderEventProducer {

    private static final Logger LOG = Logger.getLogger(OrderEventProducer.class);

    @Inject
    @Channel("order-created")
    Emitter<OrderCreatedEvent> orderCreatedEmitter;

    @Inject
    @Channel("order-status-changed")
    Emitter<OrderStatusChangedEvent> orderStatusChangedEmitter;

    public void publishOrderCreated(OrderCreatedEvent event){
        LOG.infof("Publishing OrderCreatedEvent for order: %d", event.orderId());

        orderCreatedEmitter.send(event)
                .whenComplete((success, error) -> {
                    if (error != null) {
                        LOG.errorf(error, "Error publishing OrderCreatedEvent for order: %d", event.orderId());
                    }else {
                        LOG.infof("OrderCreatedEvent published successfully for order: %d", event.orderId());
                    }
                    });
    }

    public void publishOrderStatusChanged(OrderStatusChangedEvent event){
        LOG.infof("Publishing OrderStatusChangedEvent for order:  %d (status: %s → %s)",
                event.orderId(), event.oldStatus(), event.newStatus());

        orderStatusChangedEmitter.send(event)
                .whenComplete((success, error)->{
                    if (error != null) {
                        LOG.errorf(error, "Error publishing OrderStatusChangedEvent for order: %d", event.orderId());
                    }else {
                        LOG.infof("OrderStatusChangedEvent published successfully for order: %d", event.orderId());
                    }
                });
    }
}
