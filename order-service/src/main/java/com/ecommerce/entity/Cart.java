package com.ecommerce.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@SequenceGenerator(
        name = "carts_seq_gen",
        sequenceName = "carts_seq",
        allocationSize = 50
)
public class Cart extends PanacheEntity {

    @Column(nullable = false)
    public String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    public CartStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal totalAmount;

    @Column(nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(nullable = false)
    public LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    public Long version;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartItem> items = new ArrayList<>();

    public Cart() {
        this.status = CartStatus.ACTIVE;
        this.totalAmount = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Cart(String customerEmail) {
        this();
        this.customerEmail = customerEmail;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void addItem(CartItem item) {
        items.add(item);
        item.cart = this;
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        item.cart = null;
    }

    public void calculateTotal() {
        this.totalAmount = items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
