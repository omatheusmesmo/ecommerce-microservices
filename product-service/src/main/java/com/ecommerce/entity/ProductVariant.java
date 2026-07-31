package com.ecommerce.entity;

import com.ecommerce.valueobject.Money;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ProductVariant(

        @NotBlank(message = "SKU is required")
        String sku,

        Map<String, String> attributes,

        @Valid
        Money price,

        @NotNull(message = "Stock is required")
        @Min(value = 0, message = "Stock must be greater than or equal to zero")
        Integer stock
) {}
