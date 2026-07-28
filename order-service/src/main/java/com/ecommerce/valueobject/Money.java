package com.ecommerce.valueobject;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

@Embeddable
public record Money(

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.00", message = "Amount must be 0 or greater")
        @Column(nullable = false, precision = 10, scale = 2)
        BigDecimal amount,

        @NotNull(message = "Currency is required")
        @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter ISO 4217 code")
        @Column(nullable = false, length = 3)
        String currency
) {

    public static final String DEFAULT_CURRENCY = "BRL";

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot combine different currencies: " + this.currency + " and " + other.currency);
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(BigDecimal multiplicand) {
        return new Money(this.amount.multiply(multiplicand), this.currency);
    }
}
