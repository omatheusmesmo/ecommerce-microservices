package com.ecommerce.entity;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@MongoEntity(collection = "categories")
public class Category extends PanacheMongoEntity {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    public String name;

    public String parentId;

    public LocalDateTime createdAt = LocalDateTime.now();

    public Category() {}

    public Category(String name, String parentId) {
        this.name = name;
        this.parentId = parentId;
    }
}
