package com.ecommerce.consumer;

import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DeadLetterQueueConsumer {

    private static final Logger LOG = Logger.getLogger(DeadLetterQueueConsumer.class);

    @Inject
    MeterRegistry registry;

    @Incoming("order-events-dlq")
    @Blocking
    public void onDeadLetterMessage(String message) {
        registry.counter("notification.dlq.messages", "source", "order-events").increment();
        LOG.errorf("⚠️ CRITICAL: Event sent to DLQ after all retries failed - Manual investigation required!");
        LOG.errorf("DLQ Event Payload: %s", message);
        LOG.errorf("Action Required: Check Discord/Brevo API status, verify webhooks, inspect event schema");
        LOG.warnf("Event preserved in DLQ topic for reprocessing: outbox.event.Order.dlq");
    }

    @Incoming("authentication-email-dlq")
    @Blocking
    public void onAuthenticationEmailDeadLetterMessage(String message) {
        registry.counter("notification.dlq.messages", "source", "authentication-email").increment();
        LOG.errorf("⚠️ CRITICAL: Authentication email event sent to DLQ - Manual investigation required!");
        LOG.errorf("DLQ Event Payload: %s", message);
        LOG.warnf("Event preserved in DLQ topic for reprocessing: authentication-email.dlq");
    }
}
