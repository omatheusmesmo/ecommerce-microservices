package com.ecommerce.exception;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProblemDetailsIntegrationTest {

    @Test
    void unmatchedRoute_returnsProblemJson404() {
        given().when()
                .get("/does-not-exist")
                .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body("status", equalTo(404))
                .body("instance", equalTo("/does-not-exist"));
    }

    @Test
    void unauthenticatedRequest_returnsProblemJson401() {
        given().when()
                .get("/orders")
                .then()
                .statusCode(401)
                .contentType("application/problem+json")
                .body("status", equalTo(401))
                .body("instance", equalTo("/orders"));
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void unknownOrder_returnsProblemJson404() {
        given().when()
                .get("/orders/999999")
                .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body("status", equalTo(404))
                .body("title", equalTo("Resource Not Found"))
                .body("instance", equalTo("/orders/999999"));
    }
}
