package com.ecommerce.service;

import com.ecommerce.dto.NotificationRequest;
import com.ecommerce.enums.NotificationChannel;
import com.ecommerce.enums.NotificationType;
import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.StockChangeReason;
import com.ecommerce.provider.NotificationProvider;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class);

    @Inject
    Instance<NotificationProvider> providers;

    private final Map<NotificationChannel, NotificationProvider> providerMap = new HashMap<>();

    @PostConstruct
    public void init(){
        LOG.info("Initializing notification service...");

        providers.forEach(provider ->{
            providerMap.put(provider.getChannel(), provider);
            LOG.infof("Registered notification provider: %s (enabled=%s)", provider.getChannel(),provider.isEnabled());
        });

        if (!providerMap.isEmpty()){
            LOG.warn("No notification providers registered!");
        }
    }

    public void send(@Valid @NotNull NotificationRequest request){

        NotificationProvider provider = providerMap.get(request.channel());

        if (provider==null){
            LOG.warnf("No provider found for channel: %s", request.channel());
            return;
        }
        if (!provider.isEnabled()){
            LOG.debugf("Provider %s is disabled", request.channel());
            return;
        }
        provider.send(request);
    }

    public void broadcast(@Valid @NotNull NotificationRequest request,
                          @NotNull List<NotificationChannel> channels){
        channels.forEach(channel -> {
            NotificationRequest channelRequest = new NotificationRequest(
                    request.type(),
                    channel,
                    request.recipient(),
                    request.subject(),
                    request.message(),
                    request.data()
            );
            sendInternal(channelRequest);
        });
    }

    public void sendInternal(NotificationRequest request){
        NotificationProvider provider = providerMap.get(request.channel());

        if (provider != null && provider.isEnabled()){
            provider.send(request);
        }
    }

    public void notifyOrderCreated(Long orderId, String customerEmail, String customerName, BigDecimal totalAmount){
        Map<String, Object> data = Map.of(
                "orderId", orderId,
                "customerName", customerName,
                "customerEmail", customerEmail,
                "totalAmount", totalAmount
        );

        send(new NotificationRequest(
                NotificationType.ORDER_CREATED,
                NotificationChannel.DISCORD,
                "admin",
                "New Order Created",
                String.format("Order #%d placed by %s", orderId, customerEmail),
                data
        ));

        send(new NotificationRequest(
                NotificationType.ORDER_CREATED,
                NotificationChannel.EMAIL,
                customerEmail,
                "Order Confirmation - Order #" + orderId,
                String.format("Thank you for your order, %s", customerName),
                data
        ));
    }

    public void notifyOrderStatusChanged(Long orderId, String customerEmail,
                                         OrderStatus oldStatus, OrderStatus newStatus) {
        Map<String, Object> data = Map.of(
                "orderId", orderId,
                "oldStatus", oldStatus.toString(),
                "newStatus", newStatus.toString()
        );

        NotificationRequest request = new NotificationRequest(
                NotificationType.ORDER_STATUS_CHANGED,
                NotificationChannel.EMAIL,
                customerEmail,
                "Order Status Update - Order #" + orderId,
                String.format("Order #%d:  %s → %s", orderId, oldStatus, newStatus),
                data
        );

        broadcast(request, List.of(
                NotificationChannel.DISCORD,
                NotificationChannel.EMAIL
        ));
    }

    public void notifyProductCreated(String productId, String productName, BigDecimal price) {
        Map<String, Object> data = Map.of(
                "productId", productId,
                "productName", productName,
                "price", price
        );

        send(new NotificationRequest(
                NotificationType.PRODUCT_CREATED,
                NotificationChannel. DISCORD,
                "admin",
                "New Product Added",
                String.format("%s is now available", productName),
                data
        ));
    }

    public void notifyStockChanged(String productId, String productName,
                                   Integer oldStock, Integer newStock,
                                   StockChangeReason reason) {
        NotificationType type = determineStockNotificationType(oldStock, newStock, reason);

        if (type == null) {
            return;
        }

        Map<String, Object> data = Map.of(
                "productId", productId,
                "productName", productName,
                "oldStock", oldStock,
                "newStock", newStock,
                "reason", reason.toString()
        );

        String message = switch (type) {
            case STOCK_LOW_ALERT -> String.format("%s is running low (%d units left)", productName, newStock);
            case STOCK_OUT_ALERT -> String.format("%s is OUT OF STOCK!", productName);
            case STOCK_RESTOCKED -> String.format("%s restocked (+%d units)", productName, newStock - oldStock);
            default -> "";
        };

        send(new NotificationRequest(
                type,
                NotificationChannel.DISCORD,
                "admin",
                "Stock Alert",
                message,
                data
        ));
    }

    private NotificationType determineStockNotificationType(Integer oldStock, Integer newStock,
                                                            StockChangeReason reason) {
        if (newStock == 0 && oldStock > 0) {
            return NotificationType.STOCK_OUT_ALERT;
        }

        if (newStock <= 5 && newStock < oldStock) {
            return NotificationType.STOCK_LOW_ALERT;
        }

        if (reason == StockChangeReason.RESTOCK && (newStock - oldStock) >= 10) {
            return NotificationType.STOCK_RESTOCKED;
        }

        return null;
    }
}
