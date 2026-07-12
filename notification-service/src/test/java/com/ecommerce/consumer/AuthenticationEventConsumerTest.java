package com.ecommerce.consumer;

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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class AuthenticationEventConsumerTest {

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void malformedAuthenticationEvent_isRoutedToDeadLetterQueue() {
        String marker = UUID.randomUUID().toString();
        String malformedPayload = "{not-valid-json marker=" + marker;

        try (KafkaProducer<String, String> producer = createProducer()) {
            producer.send(new ProducerRecord<>("authentication-email", "dlq-test-key", malformedPayload));
        }

        assertTrue(waitForDlqMessage(marker, 30_000), "Expected malformed message to be routed to the DLQ topic");
    }

    @Test
    public void wellFormedTokenUrlEvent_isNotRoutedToDeadLetterQueue() throws Exception {
        String marker = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of(
                "userId", 1,
                "email", "user@example.com",
                "actionType", "ACTIVATE",
                "url", "https://example.com/activate/" + marker
        );
        String eventJson = objectMapper.writeValueAsString(event);

        try (KafkaProducer<String, String> producer = createProducer()) {
            producer.send(new ProducerRecord<>("authentication-email", marker, eventJson));
        }

        assertFalse(waitForDlqMessage(marker, 10_000), "Well-formed event should not be routed to the DLQ topic");
    }

    @Test
    public void wellFormedTokenConfirmationEvent_isNotRoutedToDeadLetterQueue() throws Exception {
        String marker = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of(
                "userId", 1,
                "email", marker + "@example.com",
                "actionType", "RESET"
        );
        String eventJson = objectMapper.writeValueAsString(event);

        try (KafkaProducer<String, String> producer = createProducer()) {
            producer.send(new ProducerRecord<>("authentication-email", marker, eventJson));
        }

        assertFalse(waitForDlqMessage(marker, 10_000), "Well-formed event should not be routed to the DLQ topic");
    }

    private boolean waitForDlqMessage(String marker, long timeoutMillis) {
        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            consumer.subscribe(List.of("authentication-email.dlq"));
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
