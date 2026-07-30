package com.ecommerce.consumer;

import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.valueobject.Money;
import com.ecommerce.event.OrderCreatedEvent;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class OrderEventDeadLetterQueueTest {

    @Inject
    ProductService productService;

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    ObjectMapper objectMapper;

    private String newCategoryId() {
        Category category = new Category("Test Category", null);
        categoryRepository.persist(category);
        return category.id.toString();
    }

    @Test
    public void malformedOrderEvent_isRoutedToDeadLetterQueue() {
        String marker = UUID.randomUUID().toString();
        String malformedPayload = "{not-valid-json marker=" + marker;

        try (KafkaProducer<String, String> producer = createProducer()) {
            producer.send(new ProducerRecord<>("outbox.event.Order", "dlq-test-key", malformedPayload));
        }

        assertTrue(waitForDlqMessage(marker), "Expected malformed message to be routed to the DLQ topic");
    }

    @Test
    public void orderCreatedEvent_insufficientStock_exhaustsRetriesAndRoutesToDeadLetterQueue() throws Exception {
        Product product = new Product("Low Stock Product", "Description", new Money(new BigDecimal("50.00"), "BRL"), 1, newCategoryId());
        Product created = productService.create(product);
        String productId = created.id.toString();

        OrderCreatedEvent.OrderItemEvent item = new OrderCreatedEvent.OrderItemEvent(
                productId, "Low Stock Product", 5, new Money(new BigDecimal("50.00"), "BRL"), new Money(new BigDecimal("250.00"), "BRL"));
        OrderCreatedEvent event = new OrderCreatedEvent(
                42L, "Customer", "customer@example.com", "CONFIRMED", new Money(new BigDecimal("250.00"), "BRL"), List.of(item), LocalDateTime.now());
        String eventJson = objectMapper.writeValueAsString(event);

        try (KafkaProducer<String, String> producer = createProducer()) {
            producer.send(new ProducerRecord<>("outbox.event.Order", productId, eventJson));
        }

        assertTrue(waitForDlqMessage(productId),
                "Expected an event that exhausts stock-decrease retries to be routed to the DLQ topic");

        Product unchanged = productService.findById(productId);
        assertEquals(1, unchanged.stock, "stock should remain untouched since the update never succeeded");
    }

    private boolean waitForDlqMessage(String marker) {
        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            consumer.subscribe(List.of("outbox.event.Order.product-service.dlq"));
            long deadline = System.currentTimeMillis() + 30_000;
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, String> record : records) {
                    if (record.value() != null && record.value().contains(marker)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private KafkaProducer<String, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    private KafkaConsumer<String, String> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(props);
    }
}
