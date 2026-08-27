package com.ecommerce.event;

import java.util.List;

public record ReserveStockCommand(long orderId, List<Item> items) {
    public record Item(String productId, int quantity) {}
}
