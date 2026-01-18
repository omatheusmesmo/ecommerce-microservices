package com.ecommerce.dto;

import com.ecommerce.enums.NotificationChannel;
import com.ecommerce.enums.NotificationType;
import jakarta.validation.constraints. NotBlank;
import jakarta. validation.constraints.NotNull;

import java.util.Map;

public record NotificationRequest(
        @NotNull(message = "Notification type is required")
        NotificationType type,

        @NotNull(message = "Notification channel is required")
        NotificationChannel channel,

        @NotBlank(message = "Recipient cannot be empty")
        String recipient,

        @NotBlank(message = "Subject cannot be empty")
        String subject,

        @NotBlank(message = "Message cannot be empty")
        String message,

        Map<String, Object> data
) {}