package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ProductRepository implements PanacheMongoRepository<Product> {

    public List<Product> findByCategory(String category){
        return list("category", category);
    }

    public List<Product> findActiveProducts(){
        return list("active", true);
    }

    public List<Product> findByNameContaining(String name){
        return list("name like ?1", "%" + name + "%");
    }
}
