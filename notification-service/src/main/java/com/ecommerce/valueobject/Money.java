package com.ecommerce.valueobject;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.math.BigDecimal;

@RegisterForReflection
public record Money(BigDecimal amount, String currency) {}
