package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ProductRepository implements PanacheMongoRepository<Product> {

    public List<Product> findAll(int page, int size){
        return findAll().page(Page.of(page, size)).list();
    }

    public List<Product> findByCategory(String category, int page, int size){
        return find("category", category).page(Page.of(page, size)).list();
    }

    public List<Product> findActiveProducts(int page, int size){
        return find("active", true).page(Page.of(page, size)).list();
    }

    public List<Product> findByNameContaining(String name){
        return list("name like ?1", "%" + name + "%");
    }

    public long decreaseStock(String productId, Integer quantity){
        UpdateResult result = mongoCollection().updateOne(
                Filters.and(
                        Filters.eq("_id", new ObjectId(productId)),
                        Filters.gte("stock", quantity)
                ),
                Updates.combine(
                        Updates.inc("stock", -quantity),
                        Updates.set("updatedAt", LocalDateTime.now())
                )
        );
        return result.getModifiedCount();
    }

    public long increaseStock(String productId, Integer quantity){
        UpdateResult result = mongoCollection().updateOne(
                Filters.eq("_id", new ObjectId(productId)),
                Updates.combine(
                        Updates.inc("stock", quantity),
                        Updates.set("updatedAt", LocalDateTime.now())
                )
        );
        return result.getModifiedCount();
    }
}
