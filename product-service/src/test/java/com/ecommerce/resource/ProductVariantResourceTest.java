package com.ecommerce.resource;

import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.valueobject.Money;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestHTTPEndpoint(ProductResource.class)
class ProductVariantResourceTest {

    @Inject
    CategoryRepository categoryRepository;

    private String newCategoryId() {
        Category category = new Category("Test Category", null);
        categoryRepository.persist(category);
        return category.id.toString();
    }

    private String newProductId() {
        Product product = new Product("T-Shirt", "A shirt", new Money(new BigDecimal("50.00"), "BRL"), 0, newCategoryId());
        return given()
                .body(product)
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private String newSku() {
        return "SKU-" + UUID.randomUUID();
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void addVariant_returnsCreated_and_persists() {
        String productId = newProductId();
        ProductVariant variant = new ProductVariant(newSku(), Map.of("color", "Red", "size", "M"), null, 10);

        given()
                .body(variant)
                .contentType(ContentType.JSON)
                .when()
                .post("/{id}/variants", productId)
                .then()
                .statusCode(201)
                .body("sku", is(variant.sku()))
                .body("stock", is(10));

        given()
                .when()
                .get("/{id}/variants", productId)
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].sku", is(variant.sku()));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void addVariant_duplicateSkuAcrossProducts_returnsBadRequest() {
        String sku = newSku();
        String firstProductId = newProductId();
        ProductVariant variant = new ProductVariant(sku, Map.of("size", "M"), null, 5);

        given()
                .body(variant)
                .contentType(ContentType.JSON)
                .when()
                .post("/{id}/variants", firstProductId)
                .then()
                .statusCode(201);

        String secondProductId = newProductId();
        given()
                .body(variant)
                .contentType(ContentType.JSON)
                .when()
                .post("/{id}/variants", secondProductId)
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void addVariant_unknownProduct_returnsNotFound() {
        ProductVariant variant = new ProductVariant(newSku(), Map.of("size", "M"), null, 5);

        given()
                .body(variant)
                .contentType(ContentType.JSON)
                .when()
                .post("/{id}/variants", "000000000000000000000000")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "seller", roles = "SELLER")
    public void updateVariant_returnsUpdated() {
        String productId = newProductId();
        String sku = newSku();
        ProductVariant variant = new ProductVariant(sku, Map.of("size", "M"), null, 5);

        given()
                .body(variant)
                .contentType(ContentType.JSON)
                .when()
                .post("/{id}/variants", productId)
                .then()
                .statusCode(201);

        ProductVariant updated = new ProductVariant(sku, Map.of("size", "L"), new Money(new BigDecimal("60.00"), "BRL"), 20);

        given()
                .body(updated)
                .contentType(ContentType.JSON)
                .when()
                .put("/{id}/variants/{sku}", productId, sku)
                .then()
                .statusCode(200)
                .body("stock", is(20))
                .body("attributes.size", is("L"));
    }

    @Test
    @TestSecurity(user = "seller", roles = "SELLER")
    public void updateVariant_unknownSku_returnsNotFound() {
        String productId = newProductId();
        ProductVariant updated = new ProductVariant(newSku(), Map.of("size", "L"), null, 20);

        given()
                .body(updated)
                .contentType(ContentType.JSON)
                .when()
                .put("/{id}/variants/{sku}", productId, "does-not-exist")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void removeVariant_returnsNoContent_and_freesSku() {
        String productId = newProductId();
        String sku = newSku();
        ProductVariant variant = new ProductVariant(sku, Map.of("size", "M"), null, 5);

        given()
                .body(variant)
                .contentType(ContentType.JSON)
                .when()
                .post("/{id}/variants", productId)
                .then()
                .statusCode(201);

        given()
                .when()
                .delete("/{id}/variants/{sku}", productId, sku)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/{id}/variants", productId)
                .then()
                .statusCode(200)
                .body("$", hasSize(0));

        // SKU freed for reuse on another product now that it's been removed
        String otherProductId = newProductId();
        given()
                .body(variant)
                .contentType(ContentType.JSON)
                .when()
                .post("/{id}/variants", otherProductId)
                .then()
                .statusCode(201);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void removeVariant_unknownSku_returnsNotFound() {
        String productId = newProductId();

        given()
                .when()
                .delete("/{id}/variants/{sku}", productId, "does-not-exist")
                .then()
                .statusCode(404);
    }
}
