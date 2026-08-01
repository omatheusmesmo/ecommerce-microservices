package com.ecommerce.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class DeadLetterQueueConsumerTest {

    @Inject
    DeadLetterQueueConsumer consumer;

    @Inject
    MeterRegistry registry;

    private final Logger logger = Logger.getLogger(DeadLetterQueueConsumer.class.getName());
    private final CopyOnWriteArrayList<LogRecord> captured = new CopyOnWriteArrayList<>();
    private final Handler handler = new Handler() {
        @Override
        public void publish(LogRecord record) {
            captured.add(record);
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    };

    @BeforeEach
    void attachHandler() {
        logger.addHandler(handler);
    }

    @AfterEach
    void detachHandler() {
        logger.removeHandler(handler);
    }

    @Test
    public void orderEventsDlqMessage_isConsumedAndLogged() {
        String marker = UUID.randomUUID().toString();

        try (KafkaProducer<String, String> producer = createProducer()) {
            producer.send(new ProducerRecord<>("outbox.event.Order.dlq", "key", "payload-" + marker));
        }

        assertTrue(
                waitForLogContaining(marker, 10_000),
                "Expected DeadLetterQueueConsumer to log the order-events DLQ payload");
    }

    @Test
    public void authenticationEmailDlqMessage_isConsumedAndLogged() {
        String marker = UUID.randomUUID().toString();

        try (KafkaProducer<String, String> producer = createProducer()) {
            producer.send(new ProducerRecord<>("authentication-email.dlq", "key", "payload-" + marker));
        }

        assertTrue(
                waitForLogContaining(marker, 10_000),
                "Expected DeadLetterQueueConsumer to log the authentication-email DLQ payload");
    }

    @Test
    public void onDeadLetterMessage_incrementsDlqCounterBySource() {
        double before = registry.counter("notification.dlq.messages", "source", "order-events")
                .count();

        consumer.onDeadLetterMessage("payload-" + UUID.randomUUID());

        double after = registry.counter("notification.dlq.messages", "source", "order-events")
                .count();
        assertEquals(before + 1, after);
    }

    private boolean waitForLogContaining(String marker, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            for (LogRecord record : captured) {
                if (record.getMessage() != null && record.getMessage().contains(marker)) {
                    return true;
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
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
}
