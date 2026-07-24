package com.ecommerce.service;

import com.ecommerce.entity.Product;
import com.ecommerce.entity.StockChangedReason;
import com.ecommerce.event.ProductCreatedEvent;
import com.ecommerce.event.ProductDeletedEvent;
import com.ecommerce.event.ProductUpdatedEvent;
import com.ecommerce.event.StockChangedEvent;
import com.ecommerce.messaging.ProductEventProducer;
import com.ecommerce.repository.ProductRepository;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class ProductService {

    private static final Logger LOG = Logger.getLogger(ProductService.class);

    @Inject
    ProductRepository productRepository;

    @Inject
    ProductEventProducer eventProducer;

    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @CacheResult(cacheName = "products-cache")
    public List<Product> findAll(int page, int size) {
        LOG.info("Fetching all products from MongoDB (cache miss)");
        return productRepository.findAll(page, size);
    }

    // no per-item cache to avoid null-caching issues with Redis
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    public Product findById(String id) {
        LOG.infof("Fetching product %s from MongoDB", id);
        return productRepository.findById(new ObjectId(id));
    }

    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @CacheResult(cacheName = "products-by-category")
    public List<Product> findByCategory(String category, int page, int size) {
        LOG.debugf("Searching products by category: %s (cache miss)", category);
        return productRepository.findByCategory(category, page, size);
    }

    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @CacheResult(cacheName = "products-active")
    public List<Product> findActiveProducts(int page, int size) {
        LOG.debug("Searching active products (cache miss)");
        return productRepository.findActiveProducts(page, size);
    }

    @CacheInvalidateAll(cacheName = "products-cache")
    @CacheInvalidateAll(cacheName = "products-by-category")
    @CacheInvalidateAll(cacheName = "products-active")
    public Product create(Product product) {
        LOG.infof("Creating new product: %s", product.name);
        product.createdAt = LocalDateTime.now();
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

    // removed per-item cache invalidation (@CacheInvalidate) — keep list-level invalidations
    @CacheInvalidateAll(cacheName = "products-cache")
    @CacheInvalidateAll(cacheName = "products-by-category")
    @CacheInvalidateAll(cacheName = "products-active")
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
        LOG.infof("Product updated: id=%s", id);

        ProductUpdatedEvent event = new ProductUpdatedEvent(
                existing.id.toString(),
                existing.name,
                existing.category,
                existing.price,
                existing.stock,
                existing.updatedAt
        );
        eventProducer.publishProductUpdated(event);

        if (!Objects.equals(oldStock, existing.stock)) {
            StockChangedEvent stockEvent = new StockChangedEvent(
                    existing.id.toString(),
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

    @CacheInvalidateAll(cacheName = "products-cache")
    @CacheInvalidateAll(cacheName = "products-by-category")
    @CacheInvalidateAll(cacheName = "products-active")
    public boolean delete(String id) {
        LOG.infof("Deleting product: %s", id);
        Product product = productRepository.findById(new ObjectId(id));
        if (product == null) {
            LOG.warnf("Product %s not found for deletion", id);
            return false;
        }

        boolean deleted = productRepository.deleteById(new ObjectId(id));
        if (deleted) {
            ProductDeletedEvent event = new ProductDeletedEvent(
                    product.id.toString(),
                    product.name,
                    LocalDateTime.now()
            );
            eventProducer.publishProductDeleted(event);
            LOG.infof("Product %s deleted successfully", id);
        }
        return deleted;
    }

    @Retry(delay = 1000, abortOn = {IllegalArgumentException.class, IllegalStateException.class})
    @CacheInvalidateAll(cacheName = "products-cache")
    @CacheInvalidateAll(cacheName = "products-by-category")
    @CacheInvalidateAll(cacheName = "products-active")
    public void decreaseStock(String productId, Integer quantity) {
        long updateCount = productRepository.decreaseStock(productId, quantity);

        if (updateCount == 0) {
            throw new IllegalArgumentException("Insufficient stock or product not found: " + productId);
        }

        Product updated = productRepository.findById(new ObjectId(productId));
        if (updated == null) {
            throw new IllegalStateException("Product not found after stock decrease: " + productId);
        }

        StockChangedEvent event = new StockChangedEvent(
                updated.id.toString(),
                updated.name,
                updated.stock + quantity,
                updated.stock,
                StockChangedReason.PURCHASE,
                LocalDateTime.now()
        );
        eventProducer.publishStockChanged(event);
        LOG.infof("Stock decreased atomically for product %s: -%d", productId, quantity);
    }

    @Retry(delay = 1000, abortOn = {IllegalArgumentException.class, IllegalStateException.class})
    @CacheInvalidateAll(cacheName = "products-cache")
    @CacheInvalidateAll(cacheName = "products-by-category")
    @CacheInvalidateAll(cacheName = "products-active")
    public void increaseStock(String productId, Integer quantity) {
        long updatedCount = productRepository.increaseStock(productId, quantity);

        if (updatedCount == 0) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        Product updated = productRepository.findById(new ObjectId(productId));
        if (updated == null) {
            throw new IllegalStateException("Product not found after stock increase: " + productId);
        }

        StockChangedEvent event = new StockChangedEvent(
                updated.id.toString(),
                updated.name,
                updated.stock - quantity,
                updated.stock,
                StockChangedReason.RESTOCK,
                LocalDateTime.now()
        );
        eventProducer.publishStockChanged(event);
        LOG.infof("Stock increased atomically for product %s: +%d", productId, quantity);
    }
}
