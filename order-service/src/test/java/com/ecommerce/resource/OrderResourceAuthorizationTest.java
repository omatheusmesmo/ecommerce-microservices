package com.ecommerce.resource;

import com.ecommerce.dto.CreateOrderRequest;
import com.ecommerce.dto.OrderItemRequest;
import com.ecommerce.valueobject.Money;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@TestHTTPEndpoint(OrderResource.class)
class OrderResourceAuthorizationTest {

    private static final CreateOrderRequest VALID_ORDER = new CreateOrderRequest(
            "Jane Doe",
            "jane@example.com",
            List.of(new OrderItemRequest("prod-1", "Gaming Chair", 1, new Money(new BigDecimal("850.00"), "BRL"))),
            new Money(new BigDecimal("15.00"), "BRL")
    );

    @Test
    void createOrder_withoutAuth_isRejected() {
        given()
                .contentType(ContentType.JSON)
                .body(VALID_ORDER)
                .when()
                .post()
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void createOrder_asCustomer_isAllowed() {
        given()
                .contentType(ContentType.JSON)
                .body(VALID_ORDER)
                .when()
                .post()
                .then()
                .statusCode(201);
    }

    @Test
    void findAll_withoutAuth_isRejected() {
        given()
                .when()
                .get()
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void findAll_asCustomer_isForbidden() {
        given()
                .when()
                .get()
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ADMIN")
    void findAll_asAdmin_isAllowed() {
        given()
                .when()
                .get()
                .then()
                .statusCode(200);
    }

    @Test
    void findById_withoutAuth_isRejected() {
        given()
                .when()
                .get("/1")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void findById_asAnyAuthenticatedUser_passesAuthCheck() {
        given()
                .when()
                .get("/1")
                .then()
                // ownership scoping isn't in place yet (order has no user id, only a
                // free-text customer email) - this only asserts the auth gate is passed
                .statusCode(not(401));
    }

    @Test
    void findByStatus_withoutAuth_isRejected() {
        given()
                .when()
                .get("/status/PENDING")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void findByStatus_asCustomer_isForbidden() {
        given()
                .when()
                .get("/status/PENDING")
                .then()
                .statusCode(403);
    }

    @Test
    void findByCustomerEmail_withoutAuth_isRejected() {
        given()
                .when()
                .get("/customer/jane@example.com")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void findByCustomerEmail_asCustomer_isForbidden() {
        given()
                .when()
                .get("/customer/jane@example.com")
                .then()
                .statusCode(403);
    }

    @Test
    void updateStatus_withoutAuth_isRejected() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"status":"SHIPPED"}""")
                .when()
                .put("/1/status")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void updateStatus_asCustomer_isForbidden() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"status":"SHIPPED"}""")
                .when()
                .put("/1/status")
                .then()
                .statusCode(403);
    }

    @Test
    void cancelOrder_withoutAuth_isRejected() {
        given()
                .when()
                .patch("/1/cancel")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void cancelOrder_asCustomer_passesAuthCheck() {
        given()
                .when()
                .patch("/1/cancel")
                .then()
                .statusCode(not(401));
    }
}
