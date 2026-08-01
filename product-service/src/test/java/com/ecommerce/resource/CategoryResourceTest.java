package com.ecommerce.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.ecommerce.entity.Category;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestHTTPEndpoint(CategoryResource.class)
class CategoryResourceTest {

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void create_returnsCreated_and_persists() {
        Category category = new Category("Electronics", null);

        given().body(category)
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(201)
                .body("name", is("Electronics"))
                .body("id", notNullValue());
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void create_withUnknownParent_returnsNotFound() {
        Category category = new Category("Orphan", "000000000000000000000000");

        given().body(category)
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(404);
    }

    @Test
    public void findRoots_returnsList() {
        given().when().get().then().statusCode(200).body("$", hasSize(greaterThanOrEqualTo(0)));
    }

    @Test
    public void findById_returnsNotFound_whenNotExists() {
        given().when().get("/000000000000000000000000").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void findChildren_returnsDirectChildrenOnly() {
        String electronicsId = createCategory("Electronics", null);
        String computersId = createCategory("Computers", electronicsId);
        createCategory("Laptops", computersId);

        given().when()
                .get("/{id}/children", electronicsId)
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].name", is("Computers"));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void findBreadcrumb_returnsRootToLeafPath() {
        String electronicsId = createCategory("Electronics", null);
        String computersId = createCategory("Computers", electronicsId);
        String laptopsId = createCategory("Laptops", computersId);

        given().when()
                .get("/{id}/breadcrumb", laptopsId)
                .then()
                .statusCode(200)
                .body("name", contains("Electronics", "Computers", "Laptops"));
    }

    @Test
    @TestSecurity(user = "seller", roles = "SELLER")
    public void update_returnsUpdated_whenExists() {
        String id = createCategory("Old Name", null);
        Category updated = new Category("New Name", null);

        given().body(updated)
                .contentType(ContentType.JSON)
                .when()
                .put("/{id}", id)
                .then()
                .statusCode(200)
                .body("name", is("New Name"));
    }

    @Test
    @TestSecurity(user = "seller", roles = "SELLER")
    public void update_toOwnDescendant_returnsBadRequest() {
        String parentId = createCategory("Parent", null);
        String childId = createCategory("Child", parentId);
        Category reparented = new Category("Parent", childId);

        given().body(reparented)
                .contentType(ContentType.JSON)
                .when()
                .put("/{id}", parentId)
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void delete_returnsNoContent_whenNoChildren() {
        String id = createCategory("To Delete", null);

        given().when().delete("/{id}", id).then().statusCode(204);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void delete_withChildren_returnsBadRequest() {
        String parentId = createCategory("Parent With Child", null);
        createCategory("Child", parentId);

        given().when().delete("/{id}", parentId).then().statusCode(400);
    }

    private String createCategory(String name, String parentId) {
        Category category = new Category(name, parentId);
        return given().body(category)
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }
}
