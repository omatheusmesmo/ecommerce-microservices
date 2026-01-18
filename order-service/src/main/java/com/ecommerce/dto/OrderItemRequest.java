package com.ecommerce.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record OrderItemRequest(

        @NotBlank(message = "Product ID is required")
        String productId,

        @NotBlank(message = "Product name is required")
        @Size(min = 3, max = 200, message = "Product name must be between 3 and 200 characters")
        String productName,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
        BigDecimal unitPrice
) {}