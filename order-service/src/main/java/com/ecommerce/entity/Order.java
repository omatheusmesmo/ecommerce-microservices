package com.ecommerce.entity;

import com.ecommerce.valueobject.Money;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@SequenceGenerator(name = "orders_seq_gen", sequenceName = "orders_seq", allocationSize = 50)
public class Order extends PanacheEntity {

    @Column(nullable = false)
    public String customerName;

    @Column(nullable = false)
    public String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    public OrderStatus status;

    @Embedded
    @AttributeOverride(
            name = "amount",
            column = @Column(name = "total_amount", nullable = false, precision = 10, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "total_currency", nullable = false, length = 3))
    public Money totalAmount;

    @Embedded
    @AttributeOverride(
            name = "amount",
            column = @Column(name = "shipping_cost", nullable = false, precision = 10, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "shipping_currency", nullable = false, length = 3))
    public Money shippingCost;

    @Column(nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(nullable = false)
    public LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    public Long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
        this.status = OrderStatus.PENDING;
        this.totalAmount = Money.zero(Money.DEFAULT_CURRENCY);
        this.shippingCost = Money.zero(Money.DEFAULT_CURRENCY);
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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
        Money subtotal = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(Money::add)
                .orElse(Money.zero(this.shippingCost.currency()));
        this.totalAmount = subtotal.add(this.shippingCost);
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
