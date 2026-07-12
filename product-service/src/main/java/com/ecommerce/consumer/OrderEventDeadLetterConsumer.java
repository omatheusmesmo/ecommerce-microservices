package com.ecommerce.consumer;

import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrderEventDeadLetterConsumer {

    private static final Logger LOG = Logger.getLogger(OrderEventDeadLetterConsumer.class);

    @Incoming("order-events-dlq")
    @Blocking
    public void onDeadLetterMessage(String message) {
        LOG.errorf("[KAFKA] Order event sent to DLQ after retries exhausted - manual investigation required");
        LOG.errorf("[KAFKA] DLQ payload: %s", message);
    }
}
