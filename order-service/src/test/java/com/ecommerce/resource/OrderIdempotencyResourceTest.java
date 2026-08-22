package com.ecommerce.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.ecommerce.dto.CreateOrderRequest;
import com.ecommerce.dto.OrderItemRequest;
import com.ecommerce.valueobject.Address;
import com.ecommerce.valueobject.Money;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestHTTPEndpoint(OrderResource.class)
class OrderIdempotencyResourceTest {

    private static final Address SHIPPING_ADDRESS =
            new Address("Av. Paulista", "1000", "Apto 42", "São Paulo", "SP", "01310-100", "BR");

    private CreateOrderRequest newOrderRequest() {
        return newOrderRequest("Jane Doe");
    }

    private CreateOrderRequest newOrderRequest(String customerName) {
        return new CreateOrderRequest(
                customerName,
                "jane@example.com",
                List.of(new OrderItemRequest("prod-1", "Gaming Chair", 1, new Money(new BigDecimal("100.00"), "BRL"))),
                new Money(new BigDecimal("10.00"), "BRL"),
                SHIPPING_ADDRESS,
                null);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void createOrder_repeatedWithSameIdempotencyKey_returnsSameOrder() {
        String idempotencyKey = UUID.randomUUID().toString();
        CreateOrderRequest request = newOrderRequest();

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

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void createOrder_firstCall_isNotMarkedAsReplayed() {
        given().contentType(ContentType.JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body(newOrderRequest())
                .when()
                .post()
                .then()
                .statusCode(201)
                .header("Idempotent-Replayed", nullValue());
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void createOrder_sameIdempotencyKeyWithDifferentPayload_isRejected() {
        String idempotencyKey = UUID.randomUUID().toString();

        given().contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(newOrderRequest("Jane Doe"))
                .when()
                .post()
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(newOrderRequest("John Smith"))
                .when()
                .post()
                .then()
                .statusCode(422)
                .contentType("application/problem+json")
                .body("status", is(422));
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void createOrder_withoutIdempotencyKey_alwaysCreatesNewOrder() {
        CreateOrderRequest request = newOrderRequest();

        Long firstId = given().contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");

        Long secondId = given().contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");

        assertNotEquals(firstId, secondId);
    }
}
