package com.ecommerce.consumer;

import com.ecommerce.event.ProductCreatedEvent;
import com.ecommerce.event.StockChangedEvent;
import com.ecommerce.service.NotificationService;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ProductEventConsumer {

    private static final Logger LOG = Logger.getLogger(ProductEventConsumer.class);

    @Inject
    NotificationService notificationService;

    @Incoming("product-created")
    @Blocking
    public void onProductCreated(ProductCreatedEvent event){
        try{
            LOG.infof("[KAFKA] Received product-created event:  productId=%s, name=%s, price=R$%.2f",
                    event.productId(), event.name(), event.price());

            notificationService.notifyProductCreated(
                    event.productId(),
                    event.name(),
                    event.price()
            );

            LOG.infof("[KAFKA] Product-created event processed successfully: productId=%s", event.productId());
        } catch (Exception e) {
            LOG.errorf("[KAFKA] Failed to process product-created: productId=%s", event.productId());
        }
    }

    @Incoming("stock-changed")
    @Blocking
    public void onStockChanged(StockChangedEvent event){
        try {
            LOG.infof("[KAFKA] Received stock-changed event: productId=%s, stock %d → %d (%s)",
                    event.productId(), event.oldStock(), event.newStock(), event.reason());

            notificationService.notifyStockChanged(
                    event.productId(),
                    event.productName(),
                    event.oldStock(),
                    event.newStock(),
                    event.reason()
            );

            LOG.infof("[KAFKA] Stock-changed event processed successfully: productId=%s", event.productId());
        } catch (Exception e) {
            LOG.errorf("[KAFKA] Failed to process stock-changed event: productId=%s", event.productId());
        }
    }
}
