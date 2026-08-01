package com.ecommerce.entity;

import com.ecommerce.valueobject.Money;
import com.ecommerce.valueobject.StockLocation;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@MongoEntity(collection = "products")
public class Product extends PanacheMongoEntity {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    public String name;

    @Size(max = 500, message = "Description must be at most 500 characters")
    public String description;

    @NotNull(message = "Price is required")
    @Valid
    public Money price;

    @NotEmpty(message = "At least one stock location is required")
    @Valid
    public List<StockLocation> stockLocations = new ArrayList<>();

    @NotBlank(message = "Category is required")
    public String categoryId;

    public Boolean active = true;
    public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime updatedAt = LocalDateTime.now();

    @Valid
    public List<ProductVariant> variants = new ArrayList<>();

    public Product() {}

    public Product(String name, String description, Money price, Integer stock, String categoryId) {
        this(
                name,
                description,
                price,
                List.of(new StockLocation(StockLocation.DEFAULT_LOCATION_ID, stock, 0)),
                categoryId);
    }

    public Product(
            String name, String description, Money price, List<StockLocation> stockLocations, String categoryId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockLocations = new ArrayList<>(stockLocations);
        this.categoryId = categoryId;
    }

    public int totalOnHand() {
        return stockLocations.stream().mapToInt(StockLocation::quantityOnHand).sum();
    }

    public int totalReserved() {
        return stockLocations.stream().mapToInt(StockLocation::quantityReserved).sum();
    }

    public int totalAvailable() {
        return totalOnHand() - totalReserved();
    }
}
