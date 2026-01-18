package com.ecommerce.service;

import com.ecommerce. entity.Product;
import com. ecommerce.entity.StockChangedReason;
import com.ecommerce.event. ProductCreatedEvent;
import com.ecommerce.event.ProductDeletedEvent;
import com.ecommerce.event.ProductUpdatedEvent;
import com.ecommerce.event.StockChangedEvent;
import com.ecommerce.messaging.ProductEventProducer;
import com.ecommerce.repository.ProductRepository;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject. Inject;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ProductService {

    private static final Logger LOG = Logger.getLogger(ProductService.class);

    @Inject
    ProductRepository productRepository;

    @Inject
    ProductEventProducer eventProducer;

    @CacheResult(cacheName = "products-cache")
    public List<Product> findAll(){
        LOG.info("Fetching all products from MongoDB (cache miss)");
        return productRepository.listAll();
    }

    @CacheResult(cacheName = "product-by-id")
    public Product findById(String id){
        LOG.infof("Fetching product %s from MongoDB (cache miss)", id);
        return productRepository.findById(new ObjectId(id));
    }

    @CacheResult(cacheName = "products-by-category")
    public List<Product> findByCategory(String category) {
        LOG.debugf("Searching products by category: %s (cache miss)", category);
        return productRepository.findByCategory(category);
    }

    @CacheResult(cacheName = "products-active")
    public List<Product> findActiveProducts() {
        LOG.debug("Searching active products (cache miss)");
        return productRepository.findActiveProducts();
    }

    @CacheInvalidate(cacheName = "products-cache")
    public Product create(Product product){
        LOG.infof("Creating new product: %s", product. name);
        product.createdAt = LocalDateTime. now();
        product.updatedAt = LocalDateTime.now();
        productRepository.persist(product);
        LOG.infof("Product created successfully with ID: %s", product.id);

        ProductCreatedEvent event = new ProductCreatedEvent(
                product.id.toString(),
                product.name,
                product.category,
                product.price,
                product.stock,
                product.createdAt
        );
        eventProducer.publishProductCreated(event);

        return product;
    }

    @CacheInvalidate(cacheName = "products-cache")
    @CacheInvalidate(cacheName = "product-by-id")
    @CacheInvalidate(cacheName = "products-by-category")
    @CacheInvalidate(cacheName = "products-active")
    public Product update(String id, Product updatedProduct) {
        LOG.infof("Updating product: %s", id);

        Product existing = findById(id);
        if (existing == null) {
            LOG.warnf("Product %s not found for update", id);
            return null;
        }

        Integer oldStock = existing.stock;

        existing.name = updatedProduct.name;
        existing.description = updatedProduct.description;
        existing.price = updatedProduct.price;
        existing.category = updatedProduct.category;
        existing.stock = updatedProduct.stock;
        existing.active = updatedProduct.active;
        existing.updatedAt = LocalDateTime.now();

        productRepository.update(existing);
        LOG.infof("Product updated:  id=%s", id);

        ProductUpdatedEvent event = new ProductUpdatedEvent(
                existing.id.toString(),
                existing.name,
                existing.category,
                existing. price,
                existing.stock,
                existing.updatedAt
        );
        eventProducer.publishProductUpdated(event);

        if (! oldStock.equals(existing.stock)) {
            StockChangedEvent stockEvent = new StockChangedEvent(
                    existing.id. toString(),
                    existing.name,
                    oldStock,
                    existing.stock,
                    StockChangedReason.ADJUSTMENT,
                    LocalDateTime.now()
            );
            eventProducer.publishStockChanged(stockEvent);
        }

        return existing;
    }

    @CacheInvalidate(cacheName = "products-cache")
    @CacheInvalidate(cacheName = "product-by-id")
    @CacheInvalidate(cacheName = "products-by-category")
    @CacheInvalidate(cacheName = "products-active")
    public boolean delete(String id) {
        LOG.infof("Deleting product: %s", id);

        Product product = findById(id);
        if (product == null) {
            LOG.warnf("Product %s not found for deletion", id);
            return false;
        }

        boolean deleted = productRepository.deleteById(new ObjectId(id));

        if (deleted) {
            LOG.infof("Product %s deleted successfully", id);

            ProductDeletedEvent event = new ProductDeletedEvent(
                    product.id.toString(),
                    product.name,
                    LocalDateTime.now()
            );
            eventProducer.publishProductDeleted(event);
        }

        return deleted;
    }

    @CacheInvalidate(cacheName = "product-by-id")
    @CacheInvalidate(cacheName = "products-cache")
    public void decreaseStock(String productId, Integer quantity) {
        Product product = findById(productId);

        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        if (product.stock < quantity) {
            throw new IllegalStateException("Insufficient stock for product:  " + product.name);
        }

        Integer oldStock = product.stock;
        product.stock -= quantity;
        product.updatedAt = LocalDateTime. now();

        productRepository.update(product);
        LOG.infof("Stock decreased for product %s: %d → %d", productId, oldStock, product.stock);

        StockChangedEvent event = new StockChangedEvent(
                product.id.toString(),
                product.name,
                oldStock,
                product.stock,
                StockChangedReason.PURCHASE,
                LocalDateTime.now()
        );
        eventProducer.publishStockChanged(event);
    }

    @CacheInvalidate(cacheName = "product-by-id")
    @CacheInvalidate(cacheName = "products-cache")
    public void increaseStock(String productId, Integer quantity) {
        Product product = findById(productId);

        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        Integer oldStock = product.stock;
        product.stock += quantity;
        product.updatedAt = LocalDateTime.now();

        productRepository.update(product);
        LOG.infof("Stock increased for product %s: %d → %d", productId, oldStock, product.stock);

        StockChangedEvent event = new StockChangedEvent(
                product. id.toString(),
                product. name,
                oldStock,
                product.stock,
                StockChangedReason.RESTOCK,
                LocalDateTime.now()
        );
        eventProducer.publishStockChanged(event);
    }
}