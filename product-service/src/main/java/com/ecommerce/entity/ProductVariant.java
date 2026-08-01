package com.ecommerce.entity;

import com.ecommerce.valueobject.Money;
import com.ecommerce.valueobject.StockLocation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

public record ProductVariant(
        @NotBlank(message = "SKU is required") String sku,

        Map<String, String> attributes,

        @Valid Money price,

        @NotEmpty(message = "At least one stock location is required") @Valid
        List<StockLocation> stockLocations) {

    public ProductVariant(String sku, Map<String, String> attributes, Money price, Integer stock) {
        this(sku, attributes, price, List.of(new StockLocation(StockLocation.DEFAULT_LOCATION_ID, stock, 0)));
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
