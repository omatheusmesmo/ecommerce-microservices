package com.ecommerce.outbox;

import com.ecommerce.command.ConfirmStockReservationCommand;
import com.ecommerce.command.ReleaseStockCommand;
import com.ecommerce.command.ReserveStockCommand;
import com.ecommerce.entity.Order;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/**
 * Sends the SAGA's commands to the services that own each step. They go out under a distinct
 * aggregate type, and therefore a distinct topic, because a command is addressed to one
 * service while the order events on {@code outbox.event.Order} are broadcast to whoever
 * subscribes.
 */
@ApplicationScoped
public class OrderCommandPublisher {

    public static final String RESERVE_STOCK = "ReserveStock";
    public static final String CONFIRM_STOCK_RESERVATION = "ConfirmStockReservation";
    public static final String RELEASE_STOCK = "ReleaseStock";

    static final String AGGREGATE_TYPE = "OrderCommand";

    private final OutboxWriter outboxWriter;

    @Inject
    public OrderCommandPublisher(OutboxWriter outboxWriter) {
        this.outboxWriter = outboxWriter;
    }

    public void reserveStock(Order order) {
        List<ReserveStockCommand.Item> items = order.getItems().stream()
                .map(item -> new ReserveStockCommand.Item(item.productId, item.quantity))
                .toList();

        outboxWriter.write(AGGREGATE_TYPE, order.id, RESERVE_STOCK, new ReserveStockCommand(order.id, items));
    }

    public void confirmStockReservation(Long orderId) {
        outboxWriter.write(
                AGGREGATE_TYPE, orderId, CONFIRM_STOCK_RESERVATION, new ConfirmStockReservationCommand(orderId));
    }

    public void releaseStock(Long orderId) {
        outboxWriter.write(AGGREGATE_TYPE, orderId, RELEASE_STOCK, new ReleaseStockCommand(orderId));
    }
}
