package com.ecommerce.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ecommerce.entity.Category;
import com.ecommerce.entity.ProcessedOrderEvent;
import com.ecommerce.entity.Product;
import com.ecommerce.event.OrderCreatedEvent;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.service.ProductService;
import com.ecommerce.valueobject.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.junit.jupiter.api.Test;

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
        props.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    @Test
    public void onOrderEvent_isIdempotent_whenSameKafkaRecordRedelivered() throws Exception {
        Product product = new Product(
                "Test Product Idempotent",
                "Description",
                new Money(new BigDecimal("100.00"), "BRL"),
                10,
                newCategoryId());
        Product created = productService.create(product);
        String productId = created.id.toString();

        OrderCreatedEvent.OrderItemEvent item = new OrderCreatedEvent.OrderItemEvent(
                productId,
                "Test Product Idempotent",
                2,
                new Money(new BigDecimal("100.00"), "BRL"),
                new Money(new BigDecimal("200.00"), "BRL"));
        OrderCreatedEvent event = new OrderCreatedEvent(
                999L,
                "Customer",
                "customer@example.com",
                "CONFIRMED",
                new Money(new BigDecimal("200.00"), "BRL"),
                List.of(item),
                LocalDateTime.now());
        String eventJson = objectMapper.writeValueAsString(event);

        Message<String> kafkaMessage = toRedeliveredKafkaMessage(eventJson, productId.hashCode());

        orderEventConsumer.onOrderEvent(kafkaMessage);
        orderEventConsumer.onOrderEvent(kafkaMessage);

        assertEquals(1, ProcessedOrderEvent.count("_id", eventIdOf(kafkaMessage)));
    }

    @Test
    public void onOrderEvent_isIdempotent_whenSameEventIdRedeliveredAtDifferentOffset() throws Exception {
        Product product = new Product(
                "Test Product Republished",
                "Description",
                new Money(new BigDecimal("100.00"), "BRL"),
                10,
                newCategoryId());
        Product created = productService.create(product);
        String productId = created.id.toString();

        OrderCreatedEvent.OrderItemEvent item = new OrderCreatedEvent.OrderItemEvent(
                productId,
                "Test Product Republished",
                2,
                new Money(new BigDecimal("100.00"), "BRL"),
                new Money(new BigDecimal("200.00"), "BRL"));
        OrderCreatedEvent event = new OrderCreatedEvent(
                998L,
                "Customer",
                "customer@example.com",
                "CONFIRMED",
                new Money(new BigDecimal("200.00"), "BRL"),
                List.of(item),
                LocalDateTime.now());
        String eventJson = objectMapper.writeValueAsString(event);
        String eventId = UUID.randomUUID().toString();

        orderEventConsumer.onOrderEvent(toKafkaMessage(eventJson, 1L, eventId));
        orderEventConsumer.onOrderEvent(toKafkaMessage(eventJson, 2L, eventId));

        assertEquals(1, ProcessedOrderEvent.count("_id", eventId));
    }

    private String eventIdOf(Message<String> kafkaMessage) {
        IncomingKafkaRecordMetadata<?, ?> metadata =
                kafkaMessage.getMetadata(IncomingKafkaRecordMetadata.class).orElseThrow();
        return new String(metadata.getHeaders().lastHeader("eventId").value(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private Message<String> toRedeliveredKafkaMessage(String payload, long offset) {
        return toKafkaMessage(payload, offset, UUID.randomUUID().toString());
    }

    private Message<String> toKafkaMessage(String payload, long offset, String eventId) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("outbox.event.Order", 0, offset, "key", payload);
        record.headers().add(new RecordHeader("eventId", eventId.getBytes(StandardCharsets.UTF_8)));
        IncomingKafkaRecordMetadata<String, String> metadata =
                new IncomingKafkaRecordMetadata<>(record, "order-events");
        return Message.of(payload, Metadata.of(metadata));
    }
}
