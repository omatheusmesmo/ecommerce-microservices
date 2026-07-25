package com.ecommerce.resource;

import static io.restassured.RestAssured.given;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestHTTPEndpoint(ProductResource.class)
@TestProfile(ProductResourceTimeoutTest.TinyTimeoutProfile.class)
class ProductResourceTimeoutTest {

    @Test
    void findAll_whenServiceTimesOut_returnsServiceUnavailable() {
        given()
                .when()
                .get()
                .then()
                .statusCode(503);
    }

    public static class TinyTimeoutProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "com.ecommerce.service.ProductService/findAll/Timeout/value", "1",
                    "com.ecommerce.service.ProductService/findAll/Timeout/unit", "MILLIS"
            );
        }
    }
}
