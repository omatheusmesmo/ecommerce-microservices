package com.ecommerce.filter;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@TestProfile(ProductReadRateLimitFilterTest.TinyLimitProfile.class)
class ProductReadRateLimitFilterTest {

    private static final int LIMIT = 3;

    @Test
    void listEndpoint_isRateLimited_afterLimitFromSameIp() {
        String ip = "203.0.113.40";

        for (int i = 0; i < LIMIT; i++) {
            given()
                    .header("X-Forwarded-For", ip)
                    .when()
                    .get("/products")
                    .then()
                    .statusCode(not(429));
        }

        given()
                .header("X-Forwarded-For", ip)
                .when()
                .get("/products")
                .then()
                .statusCode(429)
                .contentType("application/problem+json")
                .body("status", equalTo(429))
                .body("title", equalTo("Too Many Requests"))
                .body("detail", equalTo("Too many requests, try again later."))
                .body("instance", equalTo("/products"));
    }

    @Test
    void tracksDifferentIps_independently() {
        String exhaustedIp = "203.0.113.41";
        String freshIp = "203.0.113.42";

        for (int i = 0; i < LIMIT; i++) {
            given()
                    .header("X-Forwarded-For", exhaustedIp)
                    .when()
                    .get("/products")
                    .then()
                    .statusCode(not(429));
        }
        given()
                .header("X-Forwarded-For", exhaustedIp)
                .when()
                .get("/products")
                .then()
                .statusCode(429);

        given()
                .header("X-Forwarded-For", freshIp)
                .when()
                .get("/products")
                .then()
                .statusCode(not(429));
    }

    @Test
    void singleProductLookup_isNotRateLimited() {
        String ip = "203.0.113.43";

        // /products/{id} is a keyed single-doc read, outside the list-endpoint rate limit
        for (int i = 0; i < LIMIT + 2; i++) {
            given()
                    .header("X-Forwarded-For", ip)
                    .when()
                    .get("/products/000000000000000000000000")
                    .then()
                    .statusCode(not(429));
        }
    }

    public static class TinyLimitProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("product.ratelimit.reads-per-minute", String.valueOf(LIMIT));
        }
    }
}
