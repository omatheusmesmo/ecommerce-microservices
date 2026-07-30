package com.ecommerce.repository;

import com.ecommerce.entity.Category;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class CategoryRepository implements PanacheMongoRepository<Category> {

    public List<Category> findRoots() {
        return list("parentId", (Object) null);
    }

    public List<Category> findByParentId(String parentId) {
        return list("parentId", parentId);
    }
}
