package com.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.Test;

@QuarkusTest
class Argon2ExecutorBulkheadTest {

    private static final int LIMIT = 4;

    @Inject
    Argon2Executor argon2Executor;

    @Test
    void execute_rejectsCallsBeyondConcurrencyLimit() throws Exception {
        int total = 20;
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch saturated = new CountDownLatch(LIMIT);
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(total);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < total; i++) {
                futures.add(pool.submit(() -> {
                    try {
                        argon2Executor.execute(() -> {
                            saturated.countDown();
                            try {
                                release.await(5, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            return "ok";
                        });
                        accepted.incrementAndGet();
                    } catch (BulkheadException e) {
                        rejected.incrementAndGet();
                    }
                }));
            }

            saturated.await(5, TimeUnit.SECONDS);
            Thread.sleep(500);
            release.countDown();

            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertTrue(rejected.get() > 0, "bulkhead should reject calls beyond the concurrency limit");
        assertTrue(accepted.get() >= LIMIT, "bulkhead should admit at least its configured limit");
    }
}
