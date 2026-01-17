package com.ecommerce.entity;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@MongoEntity(collection = "products")
public class Product extends PanacheMongoEntity {

    public String name;
    public String description;
    public BigDecimal price;
    public Integer stock;
    public String category;
    public Boolean active = true;
    public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime updatedAt = LocalDateTime.now();

    public Product() {
    }

    public Product(String name, String description, BigDecimal price, Integer stock, String category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.category = category;
    }
}
