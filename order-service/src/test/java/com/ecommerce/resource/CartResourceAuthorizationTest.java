package com.ecommerce.resource;

import com.ecommerce.dto.AddCartItemRequest;
import com.ecommerce.dto.CreateCartRequest;
import com.ecommerce.dto.UpdateCartItemRequest;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@TestHTTPEndpoint(CartResource.class)
class CartResourceAuthorizationTest {

    private static final CreateCartRequest VALID_CART = new CreateCartRequest("jane@example.com");
    private static final AddCartItemRequest VALID_ITEM =
            new AddCartItemRequest("prod-1", "Gaming Chair", 1, new BigDecimal("850.00"));

    @Test
    void createCart_withoutAuth_isRejected() {
        given()
                .contentType(ContentType.JSON)
                .body(VALID_CART)
                .when()
                .post()
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void createCart_asCustomer_isAllowed() {
        given()
                .contentType(ContentType.JSON)
                .body(VALID_CART)
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
                // ownership scoping isn't in place yet (cart has no user id, only a
                // free-text customer email) - this only asserts the auth gate is passed
                .statusCode(not(401));
    }

    @Test
    void findActiveByCustomerEmail_withoutAuth_isRejected() {
        given()
                .when()
                .get("/customer/jane@example.com")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void findActiveByCustomerEmail_asCustomer_isForbidden() {
        given()
                .when()
                .get("/customer/jane@example.com")
                .then()
                .statusCode(403);
    }

    @Test
    void addItem_withoutAuth_isRejected() {
        given()
                .contentType(ContentType.JSON)
                .body(VALID_ITEM)
                .when()
                .post("/1/items")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void addItem_asCustomer_passesAuthCheck() {
        given()
                .contentType(ContentType.JSON)
                .body(VALID_ITEM)
                .when()
                .post("/1/items")
                .then()
                .statusCode(not(401));
    }

    @Test
    void updateItemQuantity_withoutAuth_isRejected() {
        given()
                .contentType(ContentType.JSON)
                .body(new UpdateCartItemRequest(3))
                .when()
                .put("/1/items/1")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void updateItemQuantity_asCustomer_passesAuthCheck() {
        given()
                .contentType(ContentType.JSON)
                .body(new UpdateCartItemRequest(3))
                .when()
                .put("/1/items/1")
                .then()
                .statusCode(not(401));
    }

    @Test
    void removeItem_withoutAuth_isRejected() {
        given()
                .when()
                .delete("/1/items/1")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void removeItem_asCustomer_passesAuthCheck() {
        given()
                .when()
                .delete("/1/items/1")
                .then()
                .statusCode(not(401));
    }

    @Test
    void abandonCart_withoutAuth_isRejected() {
        given()
                .when()
                .patch("/1/abandon")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void abandonCart_asCustomer_passesAuthCheck() {
        given()
                .when()
                .patch("/1/abandon")
                .then()
                .statusCode(not(401));
    }
}
