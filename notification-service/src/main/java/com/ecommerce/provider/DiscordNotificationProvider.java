package com.ecommerce.provider;

import com.ecommerce.client.DiscordWebhookClient;
import com.ecommerce.dto.NotificationRequest;
import com.ecommerce.enums.NotificationChannel;
import com.ecommerce.enums.NotificationType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DiscordNotificationProvider extends AbstractNotificationProvider{

    @Inject
    DiscordWebhookClient discordClient;

    @ConfigProperty(name = "notification.discord.enabled", defaultValue = "true")
    boolean enabled;

    @Override
    protected void doSend(NotificationRequest request) {
        if (!discordClient.isConfigured()){
            log.warn("Discord webhook not configured, skipping notification");
            return;
        }
        String emoji = getEmojiForType(request.type());
        String color = getColorForType(request.type());
        List<DiscordWebhookClient.Field> fields = buildFields(request);

        discordClient.sendRichMessage(
                emoji + " " + request.subject(),
                request.message(),
                color,
                fields
        );
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.DISCORD;
    }

    @Override
    public boolean isEnabled() {
        return enabled && discordClient.isConfigured();
    }

    private String getEmojiForType(NotificationType type) {
        return switch (type) {
            case ORDER_CREATED -> "🛒";
            case ORDER_STATUS_CHANGED, STOCK_RESTOCKED -> "📦";
            case ORDER_DELIVERED -> "✅";
            case ORDER_CANCELLED -> "❌";
            case PRODUCT_CREATED -> "✨";
            case STOCK_LOW_ALERT -> "⚠️";
            case STOCK_OUT_ALERT -> "🚨";
        };
    }

    private String getColorForType(NotificationType type) {
        return switch (type) {
            case ORDER_CREATED, ORDER_DELIVERED, STOCK_RESTOCKED -> "green";
            case ORDER_CANCELLED, STOCK_OUT_ALERT -> "red";
            case ORDER_STATUS_CHANGED -> "blue";
            case STOCK_LOW_ALERT -> "orange";
            case PRODUCT_CREATED -> "purple";
        };
    }

    private List<DiscordWebhookClient.Field> buildFields(NotificationRequest request){
        Map<String, Object> data = request.data();
        if (data == null || data.isEmpty()){
            return List.of();
        }

        List<DiscordWebhookClient.Field> fields = new ArrayList<>();

        for (Map.Entry<String, Object> entry : data.entrySet()){
            String fieldName = formatFieldName(entry.getKey());
            String fieldValue = formatFieldValue(entry.getValue());

            fields.add(new DiscordWebhookClient.Field(fieldName, fieldValue, true));
        }

        return fields;
    }

    private String formatFieldName(String key) {
        return key.replaceAll("([A-Z])", " $1")
                .trim()
                .substring(0, 1).toUpperCase() +
                key.replaceAll("([A-Z])", " $1").trim().substring(1);
    }

    private String formatFieldValue(Object value) {
        if (value == null) {
            return "N/A";
        }

        if (value instanceof BigDecimal bd) {
            return String.format("R$ %.2f", bd);
        }

        if (value instanceof Double d) {
            return String. format("R$ %.2f", d);
        }

        if (value instanceof Integer || value instanceof Long) {
            return String.format("%,d", ((Number) value).longValue());
        }

        return String.valueOf(value);
    }
}
