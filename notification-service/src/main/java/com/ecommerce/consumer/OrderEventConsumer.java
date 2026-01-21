package com.ecommerce.consumer;

import com.ecommerce.event.OrderCreatedEvent;
import com.ecommerce.event.OrderStatusChangedEvent;
import com.ecommerce.service.NotificationService;
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

    @Incoming("order-created")
    @Blocking
    public void onOrderCreated(OrderCreatedEvent event){
        try{
            LOG.infof("[KAFKA] Received order-created event: orderId=%d, , customer=%s, total=R$%.2f",
                    event. orderId(), event.customerName(), event.totalAmount());

            notificationService.notifyOrderCreated(
                    event.orderId(),
                    event.customerEmail(),
                    event.customerName(),
                    event.totalAmount()
            );

            LOG.infof("[KAFKA] Order-created event processed successfully: orderId=%d", event.orderId());
        } catch (Exception e){
            LOG.errorf(e, "[KAFKA] Failed to process order-created event: orderId=%d", event.orderId());
        }
    }

    @Incoming("order-status-changed")
    @Blocking
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        try {
            LOG.infof("[KAFKA] Received order-status-changed event:  orderId=%d, %s → %s",
                    event.orderId(), event.oldStatus(), event.newStatus());

            notificationService.notifyOrderStatusChanged(
                    event.orderId(),
                    event.customerEmail(),
                    event.oldStatus(),
                    event.newStatus()
            );

            LOG.infof("[KAFKA] Order-status-changed event processed successfully: orderId=%d", event.orderId());
        } catch (Exception e) {
            LOG.errorf(e, "[KAFKA] Failed to process order-status-changed event: orderId=%d", event.orderId());
        }
    }
}
