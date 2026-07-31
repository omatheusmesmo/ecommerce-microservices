package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import com.ecommerce.valueobject.StockLocation;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
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

    public List<Product> findByCategoryId(String categoryId, int page, int size){
        return find("categoryId", categoryId).page(Page.of(page, size)).list();
    }

    public List<Product> findActiveProducts(int page, int size){
        return find("active", true).page(Page.of(page, size)).list();
    }

    public List<Product> findByNameContaining(String name){
        return list("name like ?1", "%" + name + "%");
    }

    public boolean existsByVariantSku(String sku){
        return find("variants.sku", sku).firstResultOptional().isPresent();
    }

    /**
     * Atomically decreases {@code quantityOnHand} of the {@link StockLocation} identified by
     * {@link StockLocation#DEFAULT_LOCATION_ID}, only if enough quantity is available.
     *
     * @param productId the product's identifier
     * @param quantity  the quantity to subtract
     * @return the number of documents modified; {@code 0} if the product was not found or has insufficient stock
     */
    public long decreaseStock(String productId, Integer quantity){
        UpdateResult result = mongoCollection().updateOne(
                Filters.and(
                        Filters.eq("_id", new ObjectId(productId)),
                        Filters.elemMatch("stockLocations", Filters.and(
                                Filters.eq("locationId", StockLocation.DEFAULT_LOCATION_ID),
                                Filters.gte("quantityOnHand", quantity)
                        ))
                ),
                Updates.combine(
                        Updates.inc("stockLocations.$[loc].quantityOnHand", -quantity),
                        Updates.set("updatedAt", LocalDateTime.now())
                ),
                new UpdateOptions().arrayFilters(List.of(Filters.eq("loc.locationId", StockLocation.DEFAULT_LOCATION_ID)))
        );
        return result.getModifiedCount();
    }

    /**
     * Atomically increases {@code quantityOnHand} of the {@link StockLocation} identified by
     * {@link StockLocation#DEFAULT_LOCATION_ID}.
     *
     * @param productId the product's identifier
     * @param quantity  the quantity to add
     * @return the number of documents modified; {@code 0} if the product was not found
     */
    public long increaseStock(String productId, Integer quantity){
        UpdateResult result = mongoCollection().updateOne(
                Filters.and(
                        Filters.eq("_id", new ObjectId(productId)),
                        Filters.elemMatch("stockLocations", Filters.eq("locationId", StockLocation.DEFAULT_LOCATION_ID))
                ),
                Updates.combine(
                        Updates.inc("stockLocations.$[loc].quantityOnHand", quantity),
                        Updates.set("updatedAt", LocalDateTime.now())
                ),
                new UpdateOptions().arrayFilters(List.of(Filters.eq("loc.locationId", StockLocation.DEFAULT_LOCATION_ID)))
        );
        return result.getModifiedCount();
    }
}
