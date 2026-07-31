package com.ecommerce.consumer;

import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.valueobject.Money;
import com.ecommerce.event.OrderCancelledEvent;
import com.ecommerce.event.OrderCreatedEvent;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class OrderEventConsumerTest {

    @Inject
    ProductService productService;

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    OrderEventConsumer orderEventConsumer;

    private String newCategoryId() {
        Category category = new Category("Test Category", null);
        categoryRepository.persist(category);
        return category.id.toString();
    }

    private KafkaProducer<String, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    @Test
    public void consumeOrderCreated_decreasesStock() throws Exception {
        Product product = new Product("Test Product", "Description", new Money(new BigDecimal("100.00"), "BRL"), 10, newCategoryId());
        Product created = productService.create(product);
        String productId = created.id.toString();

        OrderCreatedEvent.OrderItemEvent item = new OrderCreatedEvent.OrderItemEvent(productId, "Test Product", 2, new Money(new BigDecimal("100.00"), "BRL"), new Money(new BigDecimal("200.00"), "BRL"));
        OrderCreatedEvent event = new OrderCreatedEvent(1L, "Customer", "customer@example.com", "CONFIRMED", new Money(new BigDecimal("200.00"), "BRL"), List.of(item), LocalDateTime.now());
        String eventJson = objectMapper.writeValueAsString(event);

        try (KafkaProducer<String, String> producer = createProducer()) {
            ProducerRecord<String, String> record = new ProducerRecord<>("outbox.event.Order", productId, eventJson);
            producer.send(record);
        }

        await().atMost(30, TimeUnit.SECONDS).until(() -> productService.findById(productId).totalOnHand() == 8);
        Product updated = productService.findById(productId);
        assertEquals(8, updated.totalOnHand());
    }

    @Test
    public void consumeOrderCancelled_increasesStock() throws Exception {
        Product product = new Product("Test Product Cancel", "Description", new Money(new BigDecimal("100.00"), "BRL"), 10, newCategoryId());
        Product created = productService.create(product);
        String productId = created.id.toString();

        OrderCancelledEvent.OrderItem item = new OrderCancelledEvent.OrderItem(productId, 3);
        OrderCancelledEvent event = new OrderCancelledEvent(1L, "Customer", new Money(new BigDecimal("300.00"), "BRL"), List.of(item), LocalDateTime.now());
        String eventJson = objectMapper.writeValueAsString(event);

        try (KafkaProducer<String, String> producer = createProducer()) {
            ProducerRecord<String, String> record = new ProducerRecord<>("outbox.event.Order", productId, eventJson);
            producer.send(record);
        }

        await().atMost(30, TimeUnit.SECONDS).until(() -> productService.findById(productId).totalOnHand() == 13);
        Product updated = productService.findById(productId);
        assertEquals(13, updated.totalOnHand());
    }

    @Test
    public void onOrderEvent_isIdempotent_whenSameKafkaRecordRedelivered() throws Exception {
        Product product = new Product("Test Product Idempotent", "Description", new Money(new BigDecimal("100.00"), "BRL"), 10, newCategoryId());
        Product created = productService.create(product);
        String productId = created.id.toString();

        OrderCreatedEvent.OrderItemEvent item = new OrderCreatedEvent.OrderItemEvent(productId, "Test Product Idempotent", 2, new Money(new BigDecimal("100.00"), "BRL"), new Money(new BigDecimal("200.00"), "BRL"));
        OrderCreatedEvent event = new OrderCreatedEvent(999L, "Customer", "customer@example.com", "CONFIRMED", new Money(new BigDecimal("200.00"), "BRL"), List.of(item), LocalDateTime.now());
        String eventJson = objectMapper.writeValueAsString(event);

        Message<String> kafkaMessage = toRedeliveredKafkaMessage(eventJson, productId.hashCode());

        orderEventConsumer.onOrderEvent(kafkaMessage);
        orderEventConsumer.onOrderEvent(kafkaMessage);

        Product updated = productService.findById(productId);
        assertEquals(8, updated.totalOnHand());
    }

    private Message<String> toRedeliveredKafkaMessage(String payload, long offset) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("outbox.event.Order", 0, offset, "key", payload);
        IncomingKafkaRecordMetadata<String, String> metadata = new IncomingKafkaRecordMetadata<>(record, "order-events");
        return Message.of(payload, Metadata.of(metadata));
    }
}
