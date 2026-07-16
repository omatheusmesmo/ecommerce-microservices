package com.ecommerce.resource;

import com.ecommerce.entity.Product;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestHTTPEndpoint(ProductResource.class)
class ProductResourceAuthorizationTest {

    private static final Product VALID_PRODUCT =
            new Product("Gaming Chair", "A comfortable chair", new BigDecimal("850.00"), 10, "Furniture");

    @Test
    void create_withoutAuth_isRejected() {
        given()
                .body(VALID_PRODUCT)
                .contentType(ContentType.JSON)
                .when().post()
                .then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void create_asCustomer_isForbidden() {
        given()
                .body(VALID_PRODUCT)
                .contentType(ContentType.JSON)
                .when().post()
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "seller1", roles = "SELLER")
    void create_asSeller_isAllowed() {
        given()
                .body(VALID_PRODUCT)
                .contentType(ContentType.JSON)
                .when().post()
                .then().statusCode(201);
    }

    @Test
    void update_withoutAuth_isRejected() {
        given()
                .body(VALID_PRODUCT)
                .contentType(ContentType.JSON)
                .when().put("/000000000000000000000000")
                .then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void update_asCustomer_isForbidden() {
        given()
                .body(VALID_PRODUCT)
                .contentType(ContentType.JSON)
                .when().put("/000000000000000000000000")
                .then().statusCode(403);
    }

    @Test
    void delete_withoutAuth_isRejected() {
        given()
                .when().delete("/000000000000000000000000")
                .then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "seller1", roles = "SELLER")
    void delete_asSeller_isForbidden() {
        given()
                .when().delete("/000000000000000000000000")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ADMIN")
    void delete_asAdmin_passesAuthCheck() {
        given()
                .when().delete("/000000000000000000000000")
                .then().statusCode(404);
    }
}
