package com.ecommerce.service;

import com.ecommerce.entity.Product;
import com.ecommerce.repository.ProductRepository;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ProductService {

    private static final Logger LOG = Logger.getLogger(ProductService.class);

    @Inject
    ProductRepository productRepository;

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

    public List<Product> findByCategory(String category) {
        LOG.debugf("Searching products by category: %s", category);
        return productRepository.findByCategory(category);
    }

    public List<Product> findActiveProducts() {
        LOG.debug("Searching active products");
        return productRepository.findActiveProducts();
    }

    @CacheInvalidate(cacheName = "products-cache")
    public Product create(Product product){
        LOG.infof("Creating new product: %s", product.name);
        product.createdAt = LocalDateTime.now();
        product.updatedAt = LocalDateTime.now();
        productRepository.persist(product);
        LOG.infof("Product created successfully with ID: %s", product.id);
        return product;
    }

    @CacheInvalidate(cacheName = "products-cache")
    @CacheInvalidate(cacheName = "product-by-id")
    public Product update(String id, Product updatedProduct) {
        LOG.infof("Updating product: %s", id);
        Product product = productRepository.findById(new ObjectId(id));
        if (product != null) {
            product.name = updatedProduct.name;
            product.description = updatedProduct.description;
            product.price = updatedProduct.price;
            product.stock = updatedProduct.stock;
            product.category = updatedProduct.category;
            product.active = updatedProduct.active;
            product.updatedAt = LocalDateTime.now();
            productRepository.update(product);
            LOG.infof("Product updated successfully with ID: %s", id);
        }else{
            LOG.warnf("Product with ID %s not found", id);
        }
        return product;
    }

    @CacheInvalidate(cacheName = "products-cache")
    @CacheInvalidate(cacheName = "product-by-id")
    public boolean delete(String id) {
        LOG.infof("Deleting product: %s", id);
        boolean deleted = productRepository.deleteById(new ObjectId(id));
        if (deleted) {
            LOG.infof("Product %s deleted successfully", id);
        } else {
            LOG.warnf("Product %s not found for deletion", id);
        }
        return deleted;
    }
}
