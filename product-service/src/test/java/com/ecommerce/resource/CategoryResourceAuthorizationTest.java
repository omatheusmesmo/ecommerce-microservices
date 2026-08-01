package com.ecommerce.resource;

import static io.restassured.RestAssured.given;

import com.ecommerce.entity.Category;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestHTTPEndpoint(CategoryResource.class)
class CategoryResourceAuthorizationTest {

    private static final Category VALID_CATEGORY = new Category("Electronics", null);

    @Test
    void create_withoutAuth_isRejected() {
        given().body(VALID_CATEGORY)
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void create_asCustomer_isForbidden() {
        given().body(VALID_CATEGORY)
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "seller1", roles = "SELLER")
    void create_asSeller_isAllowed() {
        given().body(VALID_CATEGORY)
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(201);
    }

    @Test
    void update_withoutAuth_isRejected() {
        given().body(VALID_CATEGORY)
                .contentType(ContentType.JSON)
                .when()
                .put("/000000000000000000000000")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer1", roles = "CUSTOMER")
    void update_asCustomer_isForbidden() {
        given().body(VALID_CATEGORY)
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
