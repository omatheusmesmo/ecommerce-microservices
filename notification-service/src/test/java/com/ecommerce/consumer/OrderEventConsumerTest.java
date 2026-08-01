package com.ecommerce.consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
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

@QuarkusTest
public class OrderEventConsumerTest {

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void malformedOrderEvent_isRoutedToDeadLetterQueue() {
        String marker = UUID.randomUUID().toString();
        String malformedPayload = "{not-valid-json marker=" + marker;

        try (KafkaProducer<String, String> producer = createProducer()) {
            producer.send(new ProducerRecord<>("outbox.event.Order", "dlq-test-key", malformedPayload));
        }

        assertTrue(waitForDlqMessage(marker, 30_000), "Expected malformed message to be routed to the DLQ topic");
    }

    @Test
    public void wellFormedOrderCreatedEvent_isNotRoutedToDeadLetterQueue() throws Exception {
        String marker = UUID.randomUUID().toString();
        Map<String, Object> item = Map.of(
                "productId",
                marker,
                "productName",
                "Test Product",
                "quantity",
                1,
                "unitPrice",
                money("10.00"),
                "subtotal",
                money("10.00"));
        Map<String, Object> event = Map.of(
                "orderId",
                1,
                "customerName",
                "Customer",
                "customerEmail",
                "customer@example.com",
                "status",
                "CONFIRMED",
                "totalAmount",
                money("10.00"),
                "shippingCost",
                money("0.00"),
                "items",
                List.of(item),
                "createdAt",
                LocalDateTime.now().toString());
        String eventJson = objectMapper.writeValueAsString(event);

        try (KafkaProducer<String, String> producer = createProducer()) {
            producer.send(new ProducerRecord<>("outbox.event.Order", marker, eventJson));
        }

        assertFalse(waitForDlqMessage(marker, 10_000), "Well-formed event should not be routed to the DLQ topic");
    }

    @Test
    public void wellFormedOrderStatusChangedEvent_isDispatchedAndNotRoutedToDeadLetterQueue() throws Exception {
        String marker = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of(
                "orderId",
                1,
                "customerEmail",
                marker + "@example.com",
                "oldStatus",
                "PENDING",
                "newStatus",
                "CONFIRMED",
                "changedAt",
                LocalDateTime.now().toString());
        String eventJson = objectMapper.writeValueAsString(event);

        try (KafkaProducer<String, String> producer = createProducer()) {
            producer.send(new ProducerRecord<>("outbox.event.Order", marker, eventJson));
        }

        assertFalse(
                waitForDlqMessage(marker, 10_000),
                "Well-formed OrderStatusChanged event should be dispatched to that branch, not the DLQ");
    }

    @Test
    public void doubleEncodedOrderCreatedEvent_isParsedAndNotRoutedToDeadLetterQueue() throws Exception {
        String marker = UUID.randomUUID().toString();
        Map<String, Object> item = Map.of(
                "productId",
                marker,
                "productName",
                "Test Product",
                "quantity",
                1,
                "unitPrice",
                money("10.00"),
                "subtotal",
                money("10.00"));
        Map<String, Object> event = Map.of(
                "orderId",
                2,
                "customerName",
                "Customer",
                "customerEmail",
                "customer@example.com",
                "status",
                "CONFIRMED",
                "totalAmount",
                money("10.00"),
                "shippingCost",
                money("0.00"),
                "items",
                List.of(item),
                "createdAt",
                LocalDateTime.now().toString());
        String innerJson = objectMapper.writeValueAsString(event);
        String doubleEncodedJson = objectMapper.writeValueAsString(innerJson);

        try (KafkaProducer<String, String> producer = createProducer()) {
            producer.send(new ProducerRecord<>("outbox.event.Order", marker, doubleEncodedJson));
        }

        assertFalse(
                waitForDlqMessage(marker, 10_000),
                "Double-encoded event should be unwrapped and parsed, not routed to the DLQ");
    }

    @Test
    public void unknownEventShape_isRoutedToDeadLetterQueue() throws Exception {
        String marker = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of("orderId", 3, "marker", marker);
        String eventJson = objectMapper.writeValueAsString(event);

        try (KafkaProducer<String, String> producer = createProducer()) {
            producer.send(new ProducerRecord<>("outbox.event.Order", marker, eventJson));
        }

        assertTrue(
                waitForDlqMessage(marker, 30_000),
                "A well-formed but unrecognized event shape should be routed to the DLQ");
    }

    private Map<String, Object> money(String amount) {
        return Map.of("amount", new BigDecimal(amount), "currency", "BRL");
    }

    private boolean waitForDlqMessage(String marker, long timeoutMillis) {
        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            consumer.subscribe(List.of("outbox.event.Order.dlq"));
            long deadline = System.currentTimeMillis() + timeoutMillis;
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
        props.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    private KafkaConsumer<String, String> createConsumer() {
        Properties props = new Properties();
        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(props);
    }
}
