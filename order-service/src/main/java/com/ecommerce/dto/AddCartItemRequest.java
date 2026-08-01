package com.ecommerce.dto;

import com.ecommerce.valueobject.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddCartItemRequest(
        @NotBlank(message = "Product ID is required") String productId,

        @NotBlank(message = "Product name is required")
        @Size(min = 3, max = 200, message = "Product name must be between 3 and 200 characters")
        String productName,

        @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "Unit price is required") @Valid Money unitPrice) {}
