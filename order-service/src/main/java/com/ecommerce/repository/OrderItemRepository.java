package com.ecommerce.repository;

import com.ecommerce.entity.OrderItem;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class OrderItemRepository implements PanacheRepository<OrderItem> {

    public List<OrderItem> findByOrderId(Long orderId) {
        return list("SELECT i " +
                "FROM OrderItem i " +
                "WHERE i IN (" +
                "SELECT o.items " +
                "FROM Order o " +
                "WHERE o.id = ? 1)",
                orderId);
    }

    public List<OrderItem> findByProductId(String productId) {
        return list("productId", productId);
    }

    public long countSalesByProductId(String productId) {
        return count("productId", productId);
    }
}