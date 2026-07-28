package com.ecommerce.resource;

import com.ecommerce.entity.Product;
import com.ecommerce.valueobject.Money;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestHTTPEndpoint(ProductResource.class)
class ProductResourceTest {

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void create_returnsCreated_and_persists() {
        Product product = new Product("Gaming Chair", "A comfortable chair", new Money(new BigDecimal("850.00"), "BRL"), 10, "Furniture");

        given()
                .body(product)
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(201)
                .body("name", is("Gaming Chair"))
                .body("id", notNullValue());
    }

    @Test
    public void findAll_returnsList() {
        given()
                .when()
                .get()
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(0)))
                .body("name", everyItem(notNullValue()));
    }

    @Test
    public void findAll_respectsSizeLimit() {
        given()
                .when()
                .get("?size=1")
                .then()
                .statusCode(200)
                .body("$", hasSize(lessThanOrEqualTo(1)));
    }

    @Test
    public void findAll_rejectsSizeAboveMax() {
        given()
                .when()
                .get("?size=101")
                .then()
                .statusCode(400);
    }

    @Test
    public void findAll_rejectsZeroSize() {
        given()
                .when()
                .get("?size=0")
                .then()
                .statusCode(400);
    }

    @Test
    public void findAll_rejectsNegativePage() {
        given()
                .when()
                .get("?page=-1")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void findById_returnsProduct_whenExists() {
        Product product = new Product("Test Product", "Description", new Money(new BigDecimal("100.00"), "BRL"), 5, "Test");
        String id = given()
                .body(product)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer admin-token")
                .when()
                .post()
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .when()
                .get("/{id}", id)
                .then()
                .statusCode(200)
                .body("name", is("Test Product"));
    }

    @Test
    public void findById_returnsNotFound_whenNotExists() {

        given()
                .when()
                .get("/000000000000000000000000")
                .then()
                .statusCode(404);
    }

    @Test
    public void findById_malformedId_returnsBadRequest() {
        given()
                .when()
                .get("/not-a-valid-object-id")
                .then()
                .statusCode(400);
    }

    @Test
    public void findByCategory_returnsList(){
        given()
                .when()
                .get("/category/Furniture")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(0)));
    }

    @Test
    public void findByActive_returnsList(){
        given()
                .when()
                .get("/active")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(0)));
    }

    @Test
    @TestSecurity(user = "seller", roles = "SELLER")
    public void update_returnsUpdated_whenExists() {
        Product product = new Product("Old Name", "Description", new Money(new BigDecimal("100.00"), "BRL"), 5, "Test");
        String id = given()
                .body(product)
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(201)
                .extract().path("id");

        Product updatedProduct = new Product("New Name", "Updated Description", new Money(new BigDecimal("150.00"), "BRL"), 10, "Updated");

        given()
                .body(updatedProduct)
                .contentType(ContentType.JSON)
                .when()
                .put("/{id}", id)
                .then()
                .statusCode(200)
                .body("name", is("New Name"));
    }

    @Test
    @TestSecurity(user = "seller", roles = "SELLER")
    public void update_returnsNotFound_whenNotExists() {
        Product updatedProduct = new Product("Name", "Description", new Money(new BigDecimal("100.00"), "BRL"), 5, "Test");

        given()
                .body(updatedProduct)
                .contentType(ContentType.JSON)
                .when()
                .put("/000000000000000000000000")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "seller", roles = "SELLER")
    public void update_malformedId_returnsBadRequest() {
        Product updatedProduct = new Product("Name", "Description", new Money(new BigDecimal("100.00"), "BRL"), 5, "Test");

        given()
                .body(updatedProduct)
                .contentType(ContentType.JSON)
                .when()
                .put("/not-a-valid-object-id")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void delete_malformedId_returnsBadRequest() {
        given()
                .when()
                .delete("/not-a-valid-object-id")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void delete_returnsNoContent_whenExists() {
        Product product = new Product("To Delete", "Description", new Money(new BigDecimal("100.00"), "BRL"), 5, "Test");
        String id = given()
                .body(product)
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .when()
                .delete("/{id}", id)
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void delete_returnsNotFound_whenNotExists() {
        given()
                .when()
                .delete("/000000000000000000000000")
                .then()
                .statusCode(404);
    }
}
