package com.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateCartRequest(

        @NotBlank(message = "Customer email is required")
        @Email(message = "Invalid email format")
        String customerEmail
) {}
