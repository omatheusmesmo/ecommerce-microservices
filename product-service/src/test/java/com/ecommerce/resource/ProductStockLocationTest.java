package com.ecommerce.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.service.ProductService;
import com.ecommerce.valueobject.Money;
import com.ecommerce.valueobject.StockLocation;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestHTTPEndpoint(ProductResource.class)
class ProductStockLocationTest {

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    ProductService productService;

    private String newCategoryId() {
        Category category = new Category("Test Category", null);
        categoryRepository.persist(category);
        return category.id.toString();
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void create_withMultipleStockLocations_persistsEachLocation() {
        Product product = new Product(
                "Warehouse Widget",
                "d",
                new Money(new BigDecimal("10.00"), "BRL"),
                List.of(new StockLocation("WAREHOUSE-SP", 20, 5), new StockLocation("WAREHOUSE-RJ", 10, 0)),
                newCategoryId());

        given().body(product)
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(201)
                .body("stockLocations", hasSize(2))
                .body("stockLocations.find { it.locationId == 'WAREHOUSE-SP' }.quantityOnHand", is(20))
                .body("stockLocations.find { it.locationId == 'WAREHOUSE-SP' }.quantityReserved", is(5))
                .body("stockLocations.find { it.locationId == 'WAREHOUSE-RJ' }.quantityOnHand", is(10));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void create_reservedExceedsOnHand_returnsBadRequest() {
        Product product = new Product(
                "Overbooked Widget",
                "d",
                new Money(new BigDecimal("10.00"), "BRL"),
                List.of(new StockLocation("WAREHOUSE-SP", 5, 10)),
                newCategoryId());

        given().body(product).contentType(ContentType.JSON).when().post().then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void create_withNoStockLocations_returnsBadRequest() {
        Product product = new Product(
                "No Stock Widget", "d", new Money(new BigDecimal("10.00"), "BRL"), List.of(), newCategoryId());

        given().body(product).contentType(ContentType.JSON).when().post().then().statusCode(400);
    }

    @Test
    public void totalOnHandReservedAndAvailable_sumAcrossLocations() {
        Product product = new Product(
                "Aggregate Widget",
                "d",
                new Money(new BigDecimal("10.00"), "BRL"),
                List.of(new StockLocation("WAREHOUSE-SP", 20, 5), new StockLocation("WAREHOUSE-RJ", 10, 2)),
                newCategoryId());

        Product created = productService.create(product);

        assertEquals(30, created.totalOnHand());
        assertEquals(7, created.totalReserved());
        assertEquals(23, created.totalAvailable());
    }
}
