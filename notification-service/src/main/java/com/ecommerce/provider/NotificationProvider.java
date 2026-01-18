package com.ecommerce.provider;

import com.ecommerce.dto.NotificationRequest;
import com.ecommerce.enums.NotificationChannel;

public interface NotificationProvider {

    void send(NotificationRequest request);

    NotificationChannel getChannel();

    boolean isEnabled();
}
