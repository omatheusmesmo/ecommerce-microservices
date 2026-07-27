package com.ecommerce.exception;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProblemDetailsIntegrationTest {

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
    void weakPassword_returnsProblemJson400() {
        given()
                .header("X-Forwarded-For", "198.51.100.90")
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"problem-%d@example.com","password":"short"}
                        """.formatted(System.nanoTime()))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("status", equalTo(400))
                .body("instance", equalTo("/auth/register"));
    }

    @Test
    void invalidCredentials_returnProblemJson401() {
        given()
                .header("X-Forwarded-For", "198.51.100.91")
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"nobody-%d@example.com","password":"Str0ngPassw0rd!"}
                        """.formatted(System.nanoTime()))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401)
                .contentType("application/problem+json")
                .body("status", equalTo(401))
                .body("title", equalTo("Authentication Failed"))
                .body("instance", equalTo("/auth/login"));
    }
}
