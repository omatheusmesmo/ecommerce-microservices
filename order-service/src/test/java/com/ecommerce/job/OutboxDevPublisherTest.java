package com.ecommerce.job;

import com.ecommerce.entity.OutboxEvent;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class OutboxDevPublisherTest {

    @Test
    void unpublishedOutboxRows_arePublishedToKafkaAndDeleted() {
        String marker = UUID.randomUUID().toString();
        Long id = persistEvent(marker);

        Optional<String> message = waitForMessage("outbox.event.Order", s -> s.contains(marker), 10);

        assertTrue(message.isPresent(), "expected the outbox event to reach Kafka");
        awaitRowDeleted(id, 10);
    }

    @Transactional
    Long persistEvent(String marker) {
        OutboxEvent event = new OutboxEvent("Order", "42", "OrderCreated", "{\"marker\":\"" + marker + "\"}");
        event.persist();
        return event.id;
    }

    @Transactional
    OutboxEvent findById(Long id) {
        return OutboxEvent.findById(id);
    }

    private void awaitRowDeleted(Long id, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (findById(id) == null) {
                return;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        throw new AssertionError("outbox row " + id + " was not deleted in time");
    }

    private KafkaConsumer<String, String> createConsumer(String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaConsumer<>(props);
    }

    private Optional<String> waitForMessage(String topic, Predicate<String> predicate, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        try (KafkaConsumer<String, String> consumer = createConsumer("test-group-" + UUID.randomUUID())) {
            consumer.subscribe(Collections.singletonList(topic));
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));
                for (ConsumerRecord<String, String> record : records.records(topic)) {
                    if (predicate.test(record.value())) {
                        return Optional.of(record.value());
                    }
                }
            }
        }
        return Optional.empty();
    }
}
