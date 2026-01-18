package com.ecommerce.messaging;

import com.ecommerce.event.ProductCreatedEvent;
import com.ecommerce.event.ProductDeletedEvent;
import com.ecommerce.event.ProductUpdatedEvent;
import com.ecommerce.event.StockChangedEvent;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ProductEventProducer {

    private static final Logger LOG = Logger.getLogger(ProductEventProducer.class);

    @Channel("product-created")
    Emitter<ProductCreatedEvent> productCreatedEmitter;

    @Channel("product-updated")
    Emitter<ProductUpdatedEvent> productUpdatedEmitter;

    @Channel("product-deleted")
    Emitter<ProductDeletedEvent> productDeletedEmitter;

    @Channel("stock-changed")
    Emitter<StockChangedEvent> stockChangedEmitter;

    public void publishProductCreated(ProductCreatedEvent event){
        productCreatedEmitter.send(
                Message.of(event).addMetadata(buildMetadata(event.productId()))
        );
        LOG.infof("Product created event published: %s", event.productId());
    }

    public void publishProductUpdated(ProductUpdatedEvent event){
        productUpdatedEmitter.send(
                Message.of(event).addMetadata(buildMetadata(event.productId()))
        );
        LOG.infof("Product updated event published: %s", event.productId());
    }

    public void publishStockChanged(StockChangedEvent event){
        stockChangedEmitter.send(
                Message.of(event).addMetadata(buildMetadata(event.productId()))
        );
        LOG.infof("Stock changed event published: %s", event.productId());
    }

    public void publishProductDeleted(ProductDeletedEvent event){
        productDeletedEmitter.send(
                Message.of(event).addMetadata(buildMetadata(event.productId()))
        );
        LOG.infof("Product deleted event published: %s", event.productId());
    }

    private OutgoingKafkaRecordMetadata<String> buildMetadata(String key){
        return OutgoingKafkaRecordMetadata.<String>builder()
                .withKey(key)
                .build();
    }
}
