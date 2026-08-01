package com.ecommerce.producer;

import com.ecommerce.event.TokenConfirmationEvent;
import com.ecommerce.event.TokenUrlEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EmailProducer {

    private static final Logger LOG = Logger.getLogger(EmailProducer.class);

    @Inject
    @Channel("authentication-email")
    Emitter<Object> emailEmitter;

    public void onTokenUrl(@Observes TokenUrlEvent event) {
        LOG.infof("Emitting URL event to Kafka for %s", event.email());
        emailEmitter.send(event);
    }

    public void onTokenConfirmation(@Observes TokenConfirmationEvent event) {
        LOG.infof("Emitting confirmation event to Kafka for %s", event.email());
        emailEmitter.send(event);
    }
}
