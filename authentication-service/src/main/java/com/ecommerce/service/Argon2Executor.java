package com.ecommerce.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.function.Supplier;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

@ApplicationScoped
public class Argon2Executor {

    @Bulkhead(value = 4)
    public <T> T execute(Supplier<T> operation) {
        return operation.get();
    }
}
