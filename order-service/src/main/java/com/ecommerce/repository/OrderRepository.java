package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class OrderRepository implements PanacheRepository<Order> {

    public Optional<Order> findByIdWithItems(Long id) {
        return find(
                "SELECT o FROM Order o " +
                        "LEFT JOIN FETCH o.items " +
                        "WHERE o.id = :  id",
                Parameters.with("id", id)
        ).firstResultOptional();
    }

    public List<Order> findByStatus(OrderStatus status){
        return list("status", status);
    }

    public List<Order> findByCustomerEmail(String email){
        return list("customerEmail", email);
    }

    public List<Order> findByPeriod(LocalDateTime start, LocalDateTime end){
        return list(
                "createdAt BETWEEN :start AND :end",
                Parameters.with("start", start).and("end", end)
        );
    }

    public long countByStatus(OrderStatus status) {
        return count("status", status);
    }

    public List<Order> findPendingOlderThan(int days) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        return list(
                "status = :status AND createdAt < :threshold",
                Parameters.with("status", OrderStatus.PENDING).and("threshold", threshold)
        );
    }
}
