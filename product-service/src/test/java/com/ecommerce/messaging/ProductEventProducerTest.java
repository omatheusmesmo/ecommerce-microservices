package com.ecommerce.messaging;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.event.ProductCreatedEvent;
import com.ecommerce.event.ProductDeletedEvent;
import com.ecommerce.event.ProductUpdatedEvent;
import com.ecommerce.event.StockChangedEvent;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.service.ProductService;
import com.ecommerce.valueobject.Money;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.annotations.Merge;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProductEventProducerTest {

    @Inject
    ProductService productService;

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    TestEventConsumer testEventConsumer;

    private String categoryId;

    @BeforeEach
    public void setUp() {
        testEventConsumer.clearQueues();
        Category category = new Category("Test Category", null);
        categoryRepository.persist(category);
        categoryId = category.id.toString();
    }

    @Test
    public void shouldPublishProductCreatedEvent() {
        Product product =
                new Product("Test Product", "Description", new Money(new BigDecimal("10.00"), "BRL"), 10, categoryId);

        product = productService.create(product);

        Record<String, ProductCreatedEvent> received = testEventConsumer.pollCreated(5, SECONDS);
        assertNotNull(received);
        assertEquals(product.id.toString(), received.key());
        assertEquals("Test Product", received.value().name());
    }

    @Test
    public void shouldPublishProductUpdatedEvent() {
        Product product =
                new Product("Test Product", "Description", new Money(new BigDecimal("10.00"), "BRL"), 10, categoryId);
        productService.create(product);
        Product updatedProduct = new Product(
                "Updated Product", "Updated Description", new Money(new BigDecimal("15.00"), "BRL"), 3, categoryId);
        productService.update(product.id.toString(), updatedProduct);

        Record<String, ProductUpdatedEvent> received = testEventConsumer.pollUpdated(5, SECONDS);
        assertNotNull(received);
        assertEquals(product.id.toString(), received.key());
        assertEquals("Updated Product", received.value().name());
    }

    @Test
    public void shouldPublishProductDeletedEvent() {
        Product product =
                new Product("Test Product", "Description", new Money(new BigDecimal("10.00"), "BRL"), 10, categoryId);
        productService.create(product);

        productService.delete(product.id.toString());

        Record<String, ProductDeletedEvent> received = testEventConsumer.pollDeleted(5, SECONDS);
        assertNotNull(received);
        assertEquals(product.id.toString(), received.key());
        assertEquals("Test Product", received.value().name());
    }

    @Test
    public void shouldPublishStockChangedEvent() {
        Product product =
                new Product("Test Product", "Description", new Money(new BigDecimal("10.00"), "BRL"), 10, categoryId);
        product = productService.create(product);

        productService.decreaseStock(product.id.toString(), 5);

        Record<String, StockChangedEvent> received = testEventConsumer.pollStockChanged(5, SECONDS);
        assertNotNull(received);
        assertEquals(product.id.toString(), received.key());
        assertEquals(10, received.value().oldStock());
        assertEquals(5, received.value().newStock());
    }

    @ApplicationScoped
    public static class TestEventConsumer {

        private final BlockingQueue<Record<String, ProductCreatedEvent>> createdEvents = new LinkedBlockingQueue<>();
        private final BlockingQueue<Record<String, ProductUpdatedEvent>> updatedEvents = new LinkedBlockingQueue<>();
        private final BlockingQueue<Record<String, ProductDeletedEvent>> deletedEvents = new LinkedBlockingQueue<>();
        private final BlockingQueue<Record<String, StockChangedEvent>> stockChangedEvents = new LinkedBlockingQueue<>();

        public void clearQueues() {
            createdEvents.clear();
            updatedEvents.clear();
            deletedEvents.clear();
            stockChangedEvents.clear();
        }

        @Incoming("product-created")
        @Merge
        public CompletionStage<Void> consumeCreated(Message<ProductCreatedEvent> message) {
            ProductCreatedEvent event = message.getPayload();
            String key = event.productId();
            Record<String, ProductCreatedEvent> record = Record.of(key, event);
            createdEvents.add(record);
            return CompletableFuture.completedFuture(null);
        }

        @Incoming("product-updated")
        @Merge
        public CompletionStage<Void> consumeUpdated(Message<ProductUpdatedEvent> message) {
            ProductUpdatedEvent event = message.getPayload();
            String key = event.productId();
            Record<String, ProductUpdatedEvent> record = Record.of(key, event);
            updatedEvents.add(record);
            return CompletableFuture.completedFuture(null);
        }

        @Incoming("product-deleted")
        @Merge
        public CompletionStage<Void> consumeDeleted(Message<ProductDeletedEvent> message) {
            ProductDeletedEvent event = message.getPayload();
            String key = event.productId();
            Record<String, ProductDeletedEvent> record = Record.of(key, event);
            deletedEvents.add(record);
            return CompletableFuture.completedFuture(null);
        }

        @Incoming("stock-changed")
        @Merge
        public CompletionStage<Void> consumeStockChanged(Message<StockChangedEvent> message) {
            StockChangedEvent event = message.getPayload();
            String key = event.productId();
            Record<String, StockChangedEvent> record = Record.of(key, event);
            stockChangedEvents.add(record);
            return CompletableFuture.completedFuture(null);
        }

        public Record<String, ProductCreatedEvent> pollCreated(long timeout, TimeUnit unit) {
            try {
                return createdEvents.poll(timeout, unit);
            } catch (InterruptedException e) {
                return null;
            }
        }

        public Record<String, ProductUpdatedEvent> pollUpdated(long timeout, TimeUnit unit) {
            try {
                return updatedEvents.poll(timeout, unit);
            } catch (InterruptedException e) {
                return null;
            }
        }

        public Record<String, ProductDeletedEvent> pollDeleted(long timeout, TimeUnit unit) {
            try {
                return deletedEvents.poll(timeout, unit);
            } catch (InterruptedException e) {
                return null;
            }
        }

        public Record<String, StockChangedEvent> pollStockChanged(long timeout, TimeUnit unit) {
            try {
                return stockChangedEvents.poll(timeout, unit);
            } catch (InterruptedException e) {
                return null;
            }
        }
    }
}
