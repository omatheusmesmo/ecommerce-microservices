package com.ecommerce.valueobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void zero_createsZeroAmountInGivenCurrency() {
        Money money = Money.zero("BRL");

        assertEquals(BigDecimal.ZERO, money.amount());
        assertEquals("BRL", money.currency());
    }

    @Test
    void add_sameCurrency_sumsAmounts() {
        Money a = new Money(new BigDecimal("10.00"), "BRL");
        Money b = new Money(new BigDecimal("5.50"), "BRL");

        assertEquals(new Money(new BigDecimal("15.50"), "BRL"), a.add(b));
    }

    @Test
    void add_differentCurrencies_throwsIllegalArgumentException() {
        Money brl = new Money(new BigDecimal("10.00"), "BRL");
        Money usd = new Money(new BigDecimal("10.00"), "USD");

        assertThrows(IllegalArgumentException.class, () -> brl.add(usd));
    }

    @Test
    void multiply_scalesAmountAndKeepsCurrency() {
        Money unitPrice = new Money(new BigDecimal("10.00"), "BRL");

        assertEquals(new Money(new BigDecimal("30.00"), "BRL"), unitPrice.multiply(BigDecimal.valueOf(3)));
    }
}
