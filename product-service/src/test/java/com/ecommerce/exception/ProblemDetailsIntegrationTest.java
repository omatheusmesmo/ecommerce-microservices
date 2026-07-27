package com.ecommerce.exception;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProblemDetailsIntegrationTest {

    @Test
    void unknownProduct_returnsProblemJson404() {
        given()
                .when()
                .get("/products/000000000000000000000000")
                .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body("status", equalTo(404))
                .body("title", equalTo("Resource Not Found"))
                .body("detail", equalTo("Product 000000000000000000000000 was not found"))
                .body("instance", equalTo("/products/000000000000000000000000"));
    }

    @Test
    void unmatchedRoute_returnsProblemJson404() {
        given()
                .when()
                .get("/does-not-exist")
                .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body("status", equalTo(404))
                .body("instance", equalTo("/does-not-exist"));
    }

    @Test
    void invalidQueryParameter_returnsProblemJsonWithViolations() {
        given()
                .when()
                .get("/products?page=-1")
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("status", equalTo(400))
                .body("violations.field", hasItem(not(nullValue())));
    }
}
