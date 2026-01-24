package com.ecommerce. dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(

        @NotBlank(message = "Customer name is required")
        @Size(min = 3, max = 100, message = "Customer name must be between 3 and 100 characters")
        String customerName,

        @NotBlank(message = "Customer email is required")
        @Email(message = "Invalid email format")
        String customerEmail,

        @NotEmpty(message = "Order must have at least one item")
        List<@Valid OrderItemRequest> items,

        @NotNull(message = "Shipping cost is required")
        @DecimalMin(value = "0.00", message = "Shipping cost must be 0 or greater")
        BigDecimal shippingCost
) {}