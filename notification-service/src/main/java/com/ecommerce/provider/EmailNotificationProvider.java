package com.ecommerce.provider;

import com.ecommerce.client.BrevoEmailClient;
import com.ecommerce.dto.BrevoEmailRequest;
import com.ecommerce.dto.NotificationRequest;
import com.ecommerce.enums.NotificationChannel;
import com.ecommerce.enums.NotificationType;
import com.ecommerce.enums.OrderStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.util.Map;

@ApplicationScoped
public class EmailNotificationProvider extends AbstractNotificationProvider {

    @Inject
    BrevoEmailClient brevoClient;

    @ConfigProperty(name = "notification.email.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "brevo.sender.name", defaultValue = "E-commerce Notifications")
    String senderName;

    @ConfigProperty(name = "brevo.sender.email", defaultValue = "noreply@ecommerce.com")
    String senderEmail;

    @Override
    protected void doSend(NotificationRequest request) {
        if (!brevoClient.isConfigured()) {
            log.warn("Brevo API not configured, skipping email");
            return;
        }

        String customerName = extractCustomerName(request);
        String htmlContent = buildHtmlContent(request);
        String textContent = buildTextContent(request);

        BrevoEmailRequest emailRequest = BrevoEmailRequest.builder()
                .sender(senderName, senderEmail)
                .to(request.recipient(), customerName)
                .subject(request.subject())
                .htmlContent(htmlContent)
                .textContent(textContent)
                .params(request.data())
                .build();

        brevoClient.sendEmail(emailRequest);
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean isEnabled() {
        return enabled && brevoClient.isConfigured();
    }

    private String buildHtmlContent(NotificationRequest request) {
        return switch (request.type()) {
            case ORDER_CREATED -> buildOrderCreatedHtml(request);
            case ORDER_STATUS_CHANGED -> buildOrderStatusChangedHtml(request);
            case ORDER_DELIVERED -> buildOrderDeliveredHtml(request);
            case ORDER_CANCELLED -> buildOrderCancelledHtml(request);
            default -> buildGenericHtml(request);
        };
    }

    private String buildTextContent(NotificationRequest request) {
        return request.message();
    }

    private String buildOrderCreatedHtml(NotificationRequest request) {
        Map<String, Object> data = request.data();
        Long orderId = getLong(data, "orderId");
        String customerName = getString(data, "customerName");
        BigDecimal totalAmount = getBigDecimal(data, "totalAmount");

        return String.format("""
            <! DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 20px auto; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 0 20px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px 20px; text-align: center; }
                    .header h1 { margin: 0; font-size:  28px; }
                    .content { padding: 30px 20px; }
                    .order-info { background: #f8f9fa; padding: 20px; margin: 20px 0; border-left: 4px solid #667eea; border-radius: 5px; }
                    .order-info h3 { margin-top: 0; color: #667eea; }
                    .order-info p { margin: 10px 0; }
                    .button { display: inline-block; padding: 12px 30px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; text-decoration: none; border-radius: 25px; margin: 20px 0; font-weight: bold; }
                    .footer { text-align: center; padding: 20px; background: #f8f9fa; color: #777; font-size: 12px; border-top: 1px solid #eee; }
                    .highlight { color: #667eea; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Order Confirmed!</h1>
                    </div>
                    <div class="content">
                        <p>Hi <strong>%s</strong>,</p>
                        <p>Thank you for your order! We've received it and it's being processed.</p>

                        <div class="order-info">
                            <h3>📋 Order Details</h3>
                            <p><strong>Order Number:</strong> <span class="highlight">#%d</span></p>
                            <p><strong>Total Amount:</strong> <span class="highlight">R$ %.2f</span></p>
                            <p><strong>Status:</strong> <span style="color: #28a745;">✓ Processing</span></p>
                        </div>

                        <p>You'll receive another email when your order ships.</p>

                        <center>
                            <a href="#" class="button">View Order Details</a>
                        </center>
                    </div>
                    <div class="footer">
                        <p><strong>E-commerce Microservices</strong></p>
                        <p>Questions? Contact us at <a href="mailto:support@ecommerce.com">support@ecommerce.com</a></p>
                        <p style="margin-top: 10px; color: #999;">© 2026 E-commerce. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, customerName, orderId, totalAmount);
    }

    private String buildOrderStatusChangedHtml(NotificationRequest request) {
        Map<String, Object> data = request.data();
        Long orderId = getLong(data, "orderId");
        String newStatusStr = getString(data, "newStatus");

        OrderStatus newStatus = OrderStatus.valueOf(newStatusStr);

        String emoji = getEmojiForStatus(newStatus);
        String statusMessage = getMessageForStatus(newStatus);
        String color = getColorForStatus(newStatus);
        String gradient = getGradientForStatus(newStatus);

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin:  0; padding: 0; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 20px auto; background: white; border-radius: 10px; overflow:  hidden; box-shadow: 0 0 20px rgba(0,0,0,0.1); }
                    .header { background: %s; color: white; padding:  30px 20px; text-align: center; }
                    .header h1 { margin: 0; font-size:  28px; }
                    .content { padding: 30px 20px; text-align: center; }
                    .status-badge { display: inline-block; padding: 10px 25px; background: %s; color: white; border-radius: 20px; font-weight: bold; font-size: 16px; margin:  20px 0; }
                    .order-number { font-size: 24px; color: %s; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; background: #f8f9fa; color:  #777; font-size: 12px; border-top: 1px solid #eee; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>%s Order Update</h1>
                    </div>
                    <div class="content">
                        <p style="font-size: 18px;">%s</p>
                        <div class="order-number">Order <strong>#%d</strong></div>
                        <div class="status-badge">%s</div>
                    </div>
                    <div class="footer">
                        <p><strong>E-commerce Microservices</strong></p>
                        <p>© 2026 E-commerce. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, gradient, color, color, emoji, statusMessage, orderId, newStatusStr);
    }

    private String buildOrderDeliveredHtml(NotificationRequest request) {
        return buildOrderStatusChangedHtml(request);
    }

    private String buildOrderCancelledHtml(NotificationRequest request) {
        return buildOrderStatusChangedHtml(request);
    }

    private String buildGenericHtml(NotificationRequest request) {
        return String.format("""
            <! DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin:  20px auto; padding: 20px; background: white; border-radius: 10px; }
                    .content { padding: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="content">
                        <h2>%s</h2>
                        <p>%s</p>
                    </div>
                </div>
            </body>
            </html>
            """, request.subject(), request.message());
    }

    private String extractCustomerName(NotificationRequest request) {
        if (request.data() != null && request.data().containsKey("customerName")) {
            return String.valueOf(request.data().get("customerName"));
        }
        return request.recipient().split("@")[0];
    }

    private String getEmojiForStatus(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> "✅";
            case SHIPPED -> "🚚";
            case DELIVERED -> "📦";
            case CANCELLED -> "❌";
            default -> "📋";
        };
    }

    private String getMessageForStatus(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> "Your order has been confirmed and is being prepared! ";
            case SHIPPED -> "Your order is on the way!";
            case DELIVERED -> "Your order has been delivered successfully!";
            case CANCELLED -> "Your order has been cancelled.";
            default -> "Your order status has been updated.";
        };
    }

    private String getColorForStatus(OrderStatus status) {
        return switch (status) {
            case CONFIRMED, DELIVERED -> "#28a745";
            case SHIPPED -> "#ff9800";
            case CANCELLED -> "#dc3545";
            default -> "#007bff";
        };
    }

    private String getGradientForStatus(OrderStatus status) {
        return switch (status) {
            case CONFIRMED, DELIVERED -> "linear-gradient(135deg, #667eea 0%, #764ba2 100%)";
            case SHIPPED -> "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)";
            case CANCELLED -> "linear-gradient(135deg, #fa709a 0%, #fee140 100%)";
            default -> "linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)";
        };
    }

    private Long getLong(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? String.valueOf(value) : "";
    }

    private BigDecimal getBigDecimal(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof BigDecimal bd) {
            return bd;
        } else if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }
}