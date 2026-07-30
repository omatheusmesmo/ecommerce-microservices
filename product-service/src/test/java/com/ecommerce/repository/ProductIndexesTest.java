package com.ecommerce.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ProductIndexesTest {

    @Inject
    ProductRepository productRepository;

    @Test
    void liquibaseCreatesIndexesOnCategoryAndActive() {
        Set<String> indexedFields = new HashSet<>();
        for (Document index : productRepository.mongoCollection().listIndexes()) {
            Document key = index.get("key", Document.class);
            indexedFields.addAll(key.keySet());
        }

        assertTrue(indexedFields.contains("categoryId"), "expected an index covering 'categoryId'");
        assertTrue(indexedFields.contains("active"), "expected an index covering 'active'");
    }
}
