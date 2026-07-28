package com.ecommerce.valueobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyTest {

    @Test
    void equalAmountAndCurrency_areEqual() {
        Money a = new Money(new BigDecimal("10.00"), "BRL");
        Money b = new Money(new BigDecimal("10.00"), "BRL");

        assertEquals(a, b);
    }

    @Test
    void defaultCurrency_isBRL() {
        assertEquals("BRL", Money.DEFAULT_CURRENCY);
    }
}
