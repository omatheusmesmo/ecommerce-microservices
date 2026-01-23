package com.ecommerce.consumer;

import com.ecommerce.event.OrderCreatedEvent;
import com.ecommerce.service.ProductService;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrderEventConsumer {

    private static final Logger LOG = Logger.getLogger(OrderEventConsumer.class);

    @Inject
    ProductService productService;

    @Incoming("order-created")
    @Blocking
    public void onOrderCreated(OrderCreatedEvent event) {
        try {
            LOG.infof("[KAFKA] Received order-created event: orderId=%d, customer=%s, total=R$%.2f",
                    event.orderId(), event.customerName(), event.totalAmount());

            for (var item : event.items()) {
                try {
                    productService.decreaseStock(item.productId(), item.quantity());
                    LOG.infof("[KAFKA] Stock decreased for product %s: -%d", item.productId(), item.quantity());
                } catch (Exception e) {
                    LOG.errorf(e, "[KAFKA] Failed to decrease stock for product %s in order %d", item.productId(), event.orderId());
                }
            }

            LOG.infof("[KAFKA] Order-created event processed successfully: orderId=%d", event.orderId());
        } catch (Exception e) {
            LOG.errorf(e, "[KAFKA] Failed to process order-created event: orderId=%d", event.orderId());
        }
    }
}
