package com.ecommerce.valueobject;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record Money(
        @NotNull(message = "Amount is required") @DecimalMin(value = "0.00", message = "Amount must be 0 or greater")
        BigDecimal amount,

        @NotNull(message = "Currency is required")
        @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter ISO 4217 code")
        String currency) {

    public static final String DEFAULT_CURRENCY = "BRL";
}
