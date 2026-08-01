package com.ecommerce.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AuthHardeningIntegrationTest {

    @Test
    void register_shortPassword_rejected() {
        register("198.51.100.40", "short-" + System.nanoTime() + "@example.com", "Ab1cdef")
                .statusCode(400);
    }

    @Test
    void register_passwordWithoutComplexity_rejected() {
        register("198.51.100.41", "weak-" + System.nanoTime() + "@example.com", "alllowercaseonly")
                .statusCode(400);
    }

    @Test
    void register_passwordWithoutSpecialCharacter_rejected() {
        register("198.51.100.43", "nospecial-" + System.nanoTime() + "@example.com", "NoSpecialChar1")
                .statusCode(400);
    }

    @Test
    void register_strongPassword_accepted() {
        register("198.51.100.42", "strong-" + System.nanoTime() + "@example.com", "Str0ngPassw0rd!")
                .statusCode(201);
    }

    @Test
    void corsPreflight_fromConfiguredOrigin_echoesAllowOrigin() {
        given().header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .when()
                .options("/auth/login")
                .then()
                .statusCode(anyOf(is(200), is(204)))
                .header("access-control-allow-origin", "http://localhost:3000");
    }

    private ValidatableResponse register(String ip, String email, String password) {
        return given().header("X-Forwarded-For", ip)
                .contentType(ContentType.JSON)
                .body("{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"fullName\":\"Test User\"}")
                .when()
                .post("/auth/register")
                .then();
    }
}
