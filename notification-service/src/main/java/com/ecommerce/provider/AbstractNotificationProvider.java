package com.ecommerce.provider;

import com.ecommerce.dto.NotificationRequest;
import org.jboss.logging.Logger;

public abstract class AbstractNotificationProvider implements NotificationProvider{

    protected final Logger log = Logger.getLogger(AbstractNotificationProvider.class);

    @Override
    public void send(NotificationRequest request) {
        if (!isEnabled()){
            log.debugf("Provider %s is disabled, skipping notification",getChannel());
        }

        try{
            log.infof("Sending %s notification to %s via %s",
                    request.type(), request.recipient(), getChannel());
            doSend(request);

            log.infof("Notification sent successfully via %s", getChannel());
        } catch (Exception e){
            log.errorf("Failed to send notification via %s", getChannel());
            handleError(request, e);
        }
    }

    protected abstract void doSend(NotificationRequest request);

    protected void handleError(NotificationRequest request, Exception e){
        log.warnf("Notification failed but continuing: %s", e.getMessage());
    }

}
