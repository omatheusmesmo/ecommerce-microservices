package com.ecommerce.entity;

import io.quarkus. hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util. ArrayList;
import java.util. List;

@Entity
@Table(name = "orders")
@SequenceGenerator(
        name = "orders_seq_gen",
        sequenceName = "orders_seq",
        allocationSize = 50
)
public class Order extends PanacheEntity {

    @Column(name = "customer_name", nullable = false)
    public String customerName;

    @Column(name = "customer_email", nullable = false)
    public String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    public OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    public BigDecimal totalAmount;

    @Column(name = "shipping_cost", nullable = false, precision = 10, scale = 2)
    public BigDecimal shippingCost;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
        this.status = OrderStatus. PENDING;
        this.totalAmount = BigDecimal.ZERO;
        this.shippingCost = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime. now();
    }

    public Order(String customerName, String customerEmail) {
        this();
        this.customerName = customerName;
        this.customerEmail = customerEmail;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.order = this;
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.order = null;
    }

    public void calculateTotal() {
        BigDecimal subtotal = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalAmount = subtotal.add(this.shippingCost);
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime. now();
    }
}