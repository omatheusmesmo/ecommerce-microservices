package com.ecommerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemRequest(
        @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity) {}
