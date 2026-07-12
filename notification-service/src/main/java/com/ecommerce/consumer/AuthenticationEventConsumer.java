package com.ecommerce.consumer;

import com.ecommerce.event.TokenConfirmationEvent;
import com.ecommerce.event.TokenUrlEvent;
import com.ecommerce.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuthenticationEventConsumer {

    private static final Logger LOG = Logger.getLogger(AuthenticationEventConsumer.class);

    @Inject
    NotificationService notificationService;

    @Inject
    ObjectMapper objectMapper;

    @Incoming("authentication-email")
    @Blocking
    public void onAuthenticationEvent(String message) {
        LOG.debugf("[KAFKA] Received raw message from authentication-email: %s", message);

        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(message);
        } catch (Exception e) {
            LOG.errorf(e, "[KAFKA] Failed to parse authentication-email message: %s", message);
            throw new IllegalArgumentException("Failed to parse authentication-email message: " + message, e);
        }

        if (jsonNode.has("url")) {
            TokenUrlEvent event = parseOrThrow(jsonNode, TokenUrlEvent.class, message);
            LOG.infof("[KAFKA] Successfully parsed TokenUrlEvent: userId=%d, actionType=%s", event.userId(), event.actionType());
            handleTokenUrl(event);
        } else if (jsonNode.has("actionType") && jsonNode.has("email")) {
            TokenConfirmationEvent event = parseOrThrow(jsonNode, TokenConfirmationEvent.class, message);
            LOG.infof("[KAFKA] Successfully parsed TokenConfirmationEvent: userId=%d, actionType=%s", event.userId(), event.actionType());
            handleTokenConfirmation(event);
        } else {
            LOG.warnf("[KAFKA] Unknown event type in authentication-email - payload: %s", message);
            throw new IllegalArgumentException("Unknown event type in authentication-email - payload: " + message);
        }
    }

    private <T> T parseOrThrow(JsonNode jsonNode, Class<T> type, String message) {
        try {
            return objectMapper.treeToValue(jsonNode, type);
        } catch (Exception e) {
            LOG.errorf(e, "[KAFKA] Failed to map authentication-email message to %s: %s", type.getSimpleName(), message);
            throw new IllegalArgumentException("Failed to map authentication-email message to " + type.getSimpleName() + ": " + message, e);
        }
    }

    private void handleTokenUrl(TokenUrlEvent event) {
        LOG.infof("[KAFKA] Processing TokenUrlEvent: userId=%d, actionType=%s", event.userId(), event.actionType());

        try {
            notificationService.notifyAuthenticationLink(event.userId(), event.email(), event.actionType(), event.url());
        } catch (Exception e) {
            LOG.errorf(e, "[KAFKA] Failed to process TokenUrlEvent: userId=%d", event.userId());
            throw e;
        }

        LOG.infof("[KAFKA] TokenUrlEvent processed successfully: userId=%d", event.userId());
    }

    private void handleTokenConfirmation(TokenConfirmationEvent event) {
        LOG.infof("[KAFKA] Processing TokenConfirmationEvent: userId=%d, actionType=%s", event.userId(), event.actionType());

        try {
            notificationService.notifyAuthenticationConfirmation(event.userId(), event.email(), event.actionType());
        } catch (Exception e) {
            LOG.errorf(e, "[KAFKA] Failed to process TokenConfirmationEvent: userId=%d", event.userId());
            throw e;
        }

        LOG.infof("[KAFKA] TokenConfirmationEvent processed successfully: userId=%d", event.userId());
    }
}
