package com.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.util.Map;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(OrderServiceTimeoutTest.TinyTimeoutProfile.class)
class OrderServiceTimeoutTest {

    @Inject
    OrderService orderService;

    @Test
    void findAll_exceedingTimeout_throwsTimeoutException() {
        assertThrows(TimeoutException.class, () -> orderService.findAll());
    }

    public static class TinyTimeoutProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "com.ecommerce.service.OrderService/findAll/Timeout/value", "1",
                    "com.ecommerce.service.OrderService/findAll/Timeout/unit", "MILLIS");
        }
    }
}
