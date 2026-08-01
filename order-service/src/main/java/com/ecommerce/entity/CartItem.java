package com.ecommerce.entity;

import com.ecommerce.valueobject.Money;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "cart_items")
@SequenceGenerator(name = "cart_items_seq_gen", sequenceName = "cart_items_seq", allocationSize = 50)
public class CartItem extends PanacheEntity {

    @Column(nullable = false)
    public String productId;

    @Column(nullable = false)
    public String productName;

    @Column(nullable = false)
    public Integer quantity;

    @Embedded
    @AttributeOverride(
            name = "amount",
            column = @Column(name = "unit_price", nullable = false, precision = 10, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "unit_price_currency", nullable = false, length = 3))
    public Money unitPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    public Cart cart;

    public CartItem() {}

    public CartItem(String productId, String productName, Integer quantity, Money unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Money getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
