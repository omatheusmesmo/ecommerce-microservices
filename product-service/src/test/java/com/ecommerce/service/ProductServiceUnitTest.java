package com.ecommerce.service;

import com.ecommerce.entity.Product;
import com.ecommerce.valueobject.Money;
import com.ecommerce.messaging.ProductEventProducer;
import com.ecommerce.repository.ProductRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ProductServiceUnitTest {

    @Inject
    ProductService productService;

    @InjectMock
    ProductRepository productRepository;

    @InjectMock
    ProductEventProducer eventProducer;

    @BeforeEach
    void setUp() {
        reset(productRepository, eventProducer);
    }

    @Test
    void findAll_returnsList(){
        Product p1 = new Product("A", "a", new Money(new BigDecimal("1.0"), "BRL"), 5, "cat");
        p1.id = new ObjectId();
        Product p2 = new Product("B", "b", new Money(new BigDecimal("2.0"), "BRL"), 3, "cat");
        p2.id = new ObjectId();
        when(productRepository.findAll(0, 20)).thenReturn(List.of(p1, p2));

        List<Product> result = productService.findAll(0, 20);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(productRepository, times(1)).findAll(0, 20);
    }

    @Test
    void findById_found_and_notFound(){
        String id = new ObjectId().toString();
        Product product = new Product("X", "desc", new Money(new BigDecimal("1.0"), "BRL"), 2, "c");
        product.id = new ObjectId(id);
        when(productRepository.findById(new ObjectId(id))).thenReturn(product);

        Product found = productService.findById(id);

        assertNotNull(found);
        assertEquals(id, found.id.toString());

        String notFoundId = "000000000000000000000000";
        when(productRepository.findById(new ObjectId(notFoundId))).thenReturn(null);
        assertNull(productService.findById(notFoundId));

        verify(productRepository, times(1)).findById(new ObjectId(id));
        verify(productRepository, times(1)).findById(new ObjectId(notFoundId));
    }

    @Test
    void findByCategory_and_findActiveProducts(){
        Product p = new Product("C", "d", new Money(new BigDecimal("3.0"), "BRL"), 1, "cat");
        p.id = new ObjectId();

        when(productRepository.findByCategory("cat", 0, 20)).thenReturn(List.of(p));
        when(productRepository.findActiveProducts(0, 20)).thenReturn(List.of(p));

        assertEquals(1, productService.findByCategory("cat", 0, 20).size());
        assertEquals(1, productService.findActiveProducts(0, 20).size());
    }

    @Test
    void create_publishesProductCreated_and_persists(){
        Product in = new Product("New", "n", new Money(new BigDecimal("4.0"), "BRL"), 10, "c");
        in.id = new ObjectId();
        doNothing().when(productRepository).persist(in);

        Product created = productService.create(in);

        assertNotNull(created.createdAt);
        assertNotNull(created.updatedAt);
        verify(productRepository, times(1)).persist(in);
        verify(eventProducer, times(1)).publishProductCreated(any());
    }

    @Test
    void update_notFound_returnsNull(){
        String id = new ObjectId().toString();
        when(productRepository.findById(any(ObjectId.class))).thenReturn(null);

        Product updated = productService.update(id, new Product());
        assertNull(updated);

        verify(eventProducer, never()).publishProductUpdated(any());
        verify(eventProducer, never()).publishStockChanged(any());
    }

    @Test
    void update_changesStock_and_publishesEvents(){
        String id = new ObjectId().toString();
        Product existing = new Product("E", "d", new Money(new BigDecimal("5.0"), "BRL"), 10, "c");
        existing.id = new ObjectId(id);
        existing.updatedAt = LocalDateTime.now().minusDays(1);

        Product payload = new Product("E", "d", new Money(new BigDecimal("5.0"), "BRL"), 8, "c");

        when(productRepository.findById(new ObjectId(id))).thenReturn(existing);
        doNothing().when(productRepository).update(any(Product.class));

        Product result = productService.update(id, payload);

        assertNotNull(result);
        verify(productRepository, times(1)).update(any(Product.class));
        verify(eventProducer, times(1)).publishProductUpdated(any());
        verify(eventProducer, times(1)).publishStockChanged(any());
    }

    @Test
    void delete_notFound_and_delete_success(){
        String id = new ObjectId().toString();
        when(productRepository.findById(new ObjectId(id))).thenReturn(null);
        assertFalse(productService.delete(id));
        verify(eventProducer,  never()).publishProductDeleted(any());

        Product product = new Product("D", "d", new Money(new BigDecimal("6.0"), "BRL"), 10, "c");
        product.id = new ObjectId(id);
        when(productRepository.findById(new ObjectId(id))).thenReturn(product);
        when(productRepository.deleteById(new ObjectId(id))).thenReturn(true);

        assertTrue(productService.delete(id));
        verify(eventProducer, times(1)).publishProductDeleted(any());

    }

    @Test
    void decreaseStock_success_publishesEvent() {
        String productId = "507f1f77bcf86cd799439011";
        int quantity = 2;

        Product updated = new Product("Test Product", "A test product",
                new Money(new BigDecimal("10.00"), "BRL"), 8, "Test Category");
        updated.id = new ObjectId(productId);

        when(productRepository.decreaseStock(productId, quantity)).thenReturn(1L);
        when(productRepository.findById(any(ObjectId.class))).thenReturn(updated);

        productService.decreaseStock(productId, quantity);

        verify(eventProducer, times(1)).publishStockChanged(any());
        verify(productRepository, times(1)).decreaseStock(productId, quantity);
    }

    @Test
    void decreaseStock_insufficientStock_throws(){
        String id = new ObjectId().toString();
        int quantity = 5;

        when(productRepository.decreaseStock(id, quantity)).thenReturn(0L);

        assertThrows(IllegalArgumentException.class, () -> productService.decreaseStock(id, quantity));
        // Business-rule failure must abort immediately, not exhaust @Retry (would be 4 calls)
        verify(productRepository, times(1)).decreaseStock(id, quantity);
        verify(eventProducer, never()).publishStockChanged(any());
    }

    @Test
    void decreaseStock_retries_on_failure_then_success(){
        String id = new ObjectId().toString();
        int quantity = 2;

        Product updated = new Product("Retry", "r", new Money(new BigDecimal("2.0"), "BRL"),8, "c");
        updated.id = new ObjectId(id);
        when(productRepository.findById(new ObjectId(id))).thenReturn(updated);

        AtomicInteger attempts = new AtomicInteger();
        doAnswer(invocation -> {
            int n = attempts.incrementAndGet();
            if (n < 3){
                throw new RuntimeException("simulated DB down");
            }
            return 1L;
        }).when(productRepository).decreaseStock(id, quantity);

        productService.decreaseStock(id, quantity);

        verify(productRepository, times(3)).decreaseStock(id, quantity);
        verify(eventProducer, times(1)).publishStockChanged(any());
    }

    @Test
    void decreaseStock_allRetriesExhausted_throwsAndNeverPublishes(){
        String id = new ObjectId().toString();
        int quantity = 2;

        when(productRepository.decreaseStock(id, quantity)).thenThrow(new RuntimeException("simulated DB down"));

        assertThrows(RuntimeException.class, () -> productService.decreaseStock(id, quantity));

        verify(productRepository, times(4)).decreaseStock(id, quantity);
        verify(eventProducer, never()).publishStockChanged(any());
    }

    @Test
    void increaseStock_success_and_notFound(){
        String id = new ObjectId().toString();
        int quantity = 3;

        Product updated = new Product("Inc","i", new Money(new BigDecimal("6.0"), "BRL"),15, "c");
        updated.id = new ObjectId(id);

        when(productRepository.increaseStock(id, quantity)).thenReturn(1L);
        when(productRepository.findById(new ObjectId(id))).thenReturn(updated);

        productService.increaseStock(id, quantity);

        verify(eventProducer, times(1)).publishStockChanged(any());
        verify(productRepository, times(1)).increaseStock(id, quantity);

        when(productRepository.increaseStock(id, quantity)).thenReturn(0L);
        assertThrows(IllegalArgumentException.class, () -> productService.increaseStock(id, quantity));
    }
}
