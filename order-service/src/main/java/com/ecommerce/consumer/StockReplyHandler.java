package com.ecommerce.consumer;

import com.ecommerce.entity.ProcessedEvent;
import com.ecommerce.event.StockRejectedEvent;
import com.ecommerce.event.StockReplyEvent;
import com.ecommerce.saga.OrderSagaOrchestrator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Applies one stock reply to the SAGA. A separate bean from the consumer on purpose: the
 * dedup check, the step advance and the mark-as-processed have to share a transaction, and a
 * {@code @Transactional} method invoked from within its own bean is not intercepted at all.
 */
@ApplicationScoped
public class StockReplyHandler {

    private static final Logger LOG = Logger.getLogger(StockReplyHandler.class);

    private final OrderSagaOrchestrator orchestrator;

    private final ObjectMapper objectMapper;

    @Inject
    public StockReplyHandler(OrderSagaOrchestrator orchestrator, ObjectMapper objectMapper) {
        this.orchestrator = orchestrator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void apply(String eventId, String eventType, String payload) throws JsonProcessingException {
        if (ProcessedEvent.alreadyProcessed(eventId)) {
            LOG.infof("[KAFKA] Skipping already-processed stock reply: %s", eventId);
            return;
        }

        switch (eventType) {
            case StockReplyConsumer.STOCK_RESERVED -> orchestrator.onStockReserved(orderIdOf(payload));
            case StockReplyConsumer.STOCK_REJECTED -> {
                StockRejectedEvent rejected = objectMapper.readValue(payload, StockRejectedEvent.class);
                orchestrator.onStockRejected(rejected.orderId(), rejected.reason());
            }
            case StockReplyConsumer.STOCK_CONFIRMED -> orchestrator.onStockConfirmed(orderIdOf(payload));
            case StockReplyConsumer.STOCK_RELEASED -> orchestrator.onStockReleased(orderIdOf(payload));
            default -> {
                LOG.debugf("[KAFKA] Ignoring unknown stock reply: %s", eventType);
                return;
            }
        }

        new ProcessedEvent(eventId).persist();
    }

    private Long orderIdOf(String payload) throws JsonProcessingException {
        return objectMapper.readValue(payload, StockReplyEvent.class).orderId();
    }
}
