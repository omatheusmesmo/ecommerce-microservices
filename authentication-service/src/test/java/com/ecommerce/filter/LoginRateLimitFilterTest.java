package com.ecommerce.filter;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class LoginRateLimitFilterTest {

    private static final String LOGIN_BODY = "{\"email\":\"nobody@example.com\",\"password\":\"wrong-password\"}";

    @Test
    void login_isRateLimited_afterTenAttemptsFromSameIp() {
        String ip = "203.0.113.10";

        for (int i = 0; i < 10; i++) {
            given().header("X-Forwarded-For", ip)
                    .contentType(ContentType.JSON)
                    .body(LOGIN_BODY)
                    .when()
                    .post("/auth/login")
                    .then()
                    .statusCode(not(429));
        }

        given().header("X-Forwarded-For", ip)
                .contentType(ContentType.JSON)
                .body(LOGIN_BODY)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(429)
                .contentType("application/problem+json")
                .body("status", equalTo(429))
                .body("title", equalTo("Too Many Requests"))
                .body("detail", equalTo("Too many requests, try again later."))
                .body("instance", equalTo("/auth/login"));
    }

    @Test
    void register_isRateLimited_afterFiveAttemptsFromSameIp() {
        String ip = "203.0.113.20";

        for (int i = 0; i < 5; i++) {
            given().header("X-Forwarded-For", ip)
                    .contentType(ContentType.JSON)
                    .body(registerBody("rate-limit-test-" + i + "@example.com"))
                    .when()
                    .post("/auth/register")
                    .then()
                    .statusCode(not(429));
        }

        given().header("X-Forwarded-For", ip)
                .contentType(ContentType.JSON)
                .body(registerBody("one-too-many@example.com"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(429);
    }

    @Test
    void login_tracksDifferentIps_independently() {
        String exhaustedIp = "203.0.113.30";
        String freshIp = "203.0.113.31";

        for (int i = 0; i < 10; i++) {
            given().header("X-Forwarded-For", exhaustedIp)
                    .contentType(ContentType.JSON)
                    .body(LOGIN_BODY)
                    .when()
                    .post("/auth/login")
                    .then()
                    .statusCode(not(429));
        }
        given().header("X-Forwarded-For", exhaustedIp)
                .contentType(ContentType.JSON)
                .body(LOGIN_BODY)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(429);

        given().header("X-Forwarded-For", freshIp)
                .contentType(ContentType.JSON)
                .body(LOGIN_BODY)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(not(429));
    }

    private String registerBody(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"Passw0rd!23\",\"fullName\":\"Test User\"}";
    }
}
