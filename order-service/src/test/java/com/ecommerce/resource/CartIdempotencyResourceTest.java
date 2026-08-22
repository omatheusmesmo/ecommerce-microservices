package com.ecommerce.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import com.ecommerce.dto.CreateCartRequest;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestHTTPEndpoint(CartResource.class)
class CartIdempotencyResourceTest {

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void createCart_repeatedWithSameIdempotencyKey_replaysAsJson() {
        String idempotencyKey = UUID.randomUUID().toString();
        CreateCartRequest request = new CreateCartRequest(UUID.randomUUID() + "@example.com");

        Long firstId = given().contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");

        given().contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201)
                .header("Idempotent-Replayed", "true")
                .contentType(ContentType.JSON)
                .body("id", is(firstId.intValue()));
    }
}
