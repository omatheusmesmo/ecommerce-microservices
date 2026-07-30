package com.ecommerce.resource;

import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.valueobject.Money;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CompositeCacheKey;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
public class ProductRedisTest {

    @Inject
    CacheManager cacheManager;

    @Inject
    CategoryRepository categoryRepository;

    private String newCategoryId() {
        Category category = new Category("Test Category", null);
        categoryRepository.persist(category);
        return category.id.toString();
    }

    @Test
    public void findAll_populatesCache() {
        Cache productsCache = cacheManager.getCache("products-cache").orElseThrow();

        given()
                .when()
                .get("/products")
                .then()
                .statusCode(200);

        // findAll(page, size) is cached under a composite key of its arguments, not the no-arg default key
        Object cached = productsCache.get(new CompositeCacheKey(0, 20), k -> null).await().indefinitely();
        assertNotNull(cached);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void cacheInvalidatedAfterCreate() {
        given()
                .when()
                .get("/products")
                .then()
                .statusCode(200);

        Cache productsCache = cacheManager.getCache("products-cache").orElseThrow();
        Cache categoryCache = cacheManager.getCache("products-by-category").orElseThrow();
        Cache activeCache = cacheManager.getCache("products-active").orElseThrow();

        Product product = new Product("New Product", "Description", new Money(new BigDecimal("100.00"), "BRL"),5,newCategoryId());
        given()
                .body(product)
                .contentType(ContentType.JSON)
                .when()
                .post("/products")
                .then()
                .statusCode(201);

        assertThrows(IllegalArgumentException.class, () ->
                productsCache.get(productsCache.getDefaultKey(), k -> null).await().indefinitely());
        assertThrows(IllegalArgumentException.class, () ->
                categoryCache.get(categoryCache.getDefaultKey(), k -> null).await().indefinitely());
        assertThrows(IllegalArgumentException.class, () ->
                activeCache.get(activeCache.getDefaultKey(), k -> null).await().indefinitely());
    }

    @Test
    @TestSecurity(user = "seller", roles = "SELLER")
    public void cacheInvalidatedAfterUpdate() {
        Product product = new Product("Original Product", "Description", new Money(new BigDecimal("100.00"), "BRL"),5,newCategoryId());
        String id = given()
                .body(product)
                .contentType(ContentType.JSON)
                .when()
                .post("/products")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .when()
                .get("/products")
                .then()
                .statusCode(200);

        Cache productsCache = cacheManager.getCache("products-cache").orElseThrow();
        Cache categoryCache = cacheManager.getCache("products-by-category").orElseThrow();
        Cache activeCache = cacheManager.getCache("products-active").orElseThrow();

        Product updatedProduct = new Product("Updated Product", "Updated Description", new Money(new BigDecimal("150.00"), "BRL"),10,newCategoryId());
        given()
                .body(updatedProduct)
                .contentType(ContentType.JSON)
                .when()
                .put("/products/{id}", id)
                .then()
                .statusCode(200);

        assertThrows(IllegalArgumentException.class, () ->
                productsCache.get(productsCache.getDefaultKey(), k -> null).await().indefinitely());
        assertThrows(IllegalArgumentException.class, () ->
                categoryCache.get(categoryCache.getDefaultKey(), k -> null).await().indefinitely());
        assertThrows(IllegalArgumentException.class, () ->
                activeCache.get(activeCache.getDefaultKey(), k -> null).await().indefinitely());
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void cacheInvalidatedAfterDelete() {
        Product product = new Product("Delete Product", "Description", new Money(new BigDecimal("100.00"), "BRL"),5,newCategoryId());
        String id = given()
                .body(product)
                .contentType(ContentType.JSON)
                .when()
                .post("/products")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .when()
                .get("/products")
                .then()
                .statusCode(200);

        Cache productsCache = cacheManager.getCache("products-cache").orElseThrow();
        Cache categoryCache = cacheManager.getCache("products-by-category").orElseThrow();
        Cache activeCache = cacheManager.getCache("products-active").orElseThrow();

        given()
                .when()
                .delete("/products/{id}", id)
                .then()
                .statusCode(204);

        assertThrows(IllegalArgumentException.class, () ->
                productsCache.get(productsCache.getDefaultKey(), k -> null).await().indefinitely());
        assertThrows(IllegalArgumentException.class, () ->
                categoryCache.get(categoryCache.getDefaultKey(), k -> null).await().indefinitely());
        assertThrows(IllegalArgumentException.class, () ->
                activeCache.get(activeCache.getDefaultKey(), k -> null).await().indefinitely());
    }

}
