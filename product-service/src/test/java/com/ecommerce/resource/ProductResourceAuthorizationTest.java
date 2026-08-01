package com.ecommerce.resource;

import static io.restassured.RestAssured.given;

import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.valueobject.Money;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestHTTPEndpoint(ProductResource.class)
class ProductResourceAuthorizationTest {

    @Inject
    CategoryRepository categoryRepository;

    private Product validProduct;

    @BeforeEach
    void setUp() {
        Category category = new Category("Furniture", null);
        categoryRepository.persist(category);
        validProduct = new Product(
                "Gaming Chair",
                "A comfortable chair",
                new Money(new BigDecimal("850.00"), "BRL"),
                10,
                category.id.toString());
    }

    @Test
    void create_withoutAuth_isRejected() {
        given().body(validProduct)
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void create_asCustomer_isForbidden() {
        given().body(validProduct)
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "seller1", roles = "SELLER")
    void create_asSeller_isAllowed() {
        given().body(validProduct)
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(201);
    }

    @Test
    void update_withoutAuth_isRejected() {
        given().body(validProduct)
                .contentType(ContentType.JSON)
                .when()
                .put("/000000000000000000000000")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void update_asCustomer_isForbidden() {
        given().body(validProduct)
                .contentType(ContentType.JSON)
                .when()
                .put("/000000000000000000000000")
                .then()
                .statusCode(403);
    }

    @Test
    void delete_withoutAuth_isRejected() {
        given().when().delete("/000000000000000000000000").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "seller1", roles = "SELLER")
    void delete_asSeller_isForbidden() {
        given().when().delete("/000000000000000000000000").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ADMIN")
    void delete_asAdmin_passesAuthCheck() {
        given().when().delete("/000000000000000000000000").then().statusCode(404);
    }
}
