package com.ecommerce.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_items")
@SequenceGenerator(
        name = "cart_items_seq_gen",
        sequenceName = "cart_items_seq",
        allocationSize = 50
)
public class CartItem extends PanacheEntity {

    @Column(name = "product_id", nullable = false)
    public String productId;

    @Column(name = "product_name", nullable = false)
    public String productName;

    @Column(nullable = false)
    public Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    public BigDecimal unitPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    public Cart cart;

    public CartItem() {}

    public CartItem(String productId, String productName, Integer quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
