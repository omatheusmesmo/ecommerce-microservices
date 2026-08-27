package com.ecommerce.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ecommerce.entity.OutboxEvent;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(DebeziumOutboxRoutingTest.ComposeProfile.class)
class DebeziumOutboxRoutingTest {

    private static final String TOPIC = "outbox.event.Order";
    private static final String CONNECTOR = "order-service-outbox-connector";
    private static final Path CONNECTOR_CONFIG = Path.of("debezium-connector-config.json");

    public static class ComposeProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.compose.devservices.files", "src/test/resources/compose-devservices.yml",
                    "quarkus.scheduler.enabled", "false");
        }
    }

    @ConfigProperty(name = "outbox.test.connect-port")
    int connectPort;

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrapServers;

    @Test
    void routedEventCarriesTheHeadersConsumersRouteOn() throws Exception {
        registerConnector();

        String aggregateId = UUID.randomUUID().toString();
        UUID eventId = persistOutboxEvent(aggregateId);

        ConsumerRecord<String, String> record = consumeOne();

        assertEquals(
                eventId.toString(),
                headerValue(record, "eventId"),
                "EventRouter must place outbox.event_id on the eventId header");
        assertEquals(
                "OrderCreated",
                headerValue(record, "eventType"),
                "EventRouter must place outbox.event_type on the eventType header");
        assertEquals(aggregateId, record.key());
        assertTrue(record.value().contains(aggregateId));
    }

    private String headerValue(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        assertNotNull(header, "missing " + name + " header");
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private UUID persistOutboxEvent(String aggregateId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            OutboxEvent event =
                    new OutboxEvent("Order", aggregateId, "OrderCreated", "{\"orderId\":\"" + aggregateId + "\"}");
            event.persist();
            return event.eventId;
        });
    }

    private void registerConnector() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(URI.create(connectUrl() + "/connectors"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(renderConnectorConfig())));

        assertEquals(201, response.statusCode(), "connector registration failed: " + response.body());
        awaitTaskRunning();
    }

    private String renderConnectorConfig() throws IOException {
        return Files.readString(CONNECTOR_CONFIG)
                .replace("${POSTGRES_HOSTNAME}", "postgres")
                .replace("${POSTGRES_PORT}", "5432")
                .replace("${POSTGRES_USER}", "quarkus")
                .replace("${POSTGRES_PASSWORD}", "quarkus")
                .replace("${POSTGRES_DB}", "quarkus");
    }

    private void awaitTaskRunning() throws Exception {
        URI statusUri = URI.create(connectUrl() + "/connectors/" + CONNECTOR + "/status");
        String body = "";
        for (int attempt = 0; attempt < 90; attempt++) {
            body = send(HttpRequest.newBuilder(statusUri).GET()).body();
            if (body.contains("\"tasks\":[{\"id\":0,\"state\":\"RUNNING\"")) {
                return;
            }
            if (body.contains("\"state\":\"FAILED\"")) {
                throw new IllegalStateException("connector or task failed: " + body);
            }
            Thread.sleep(1000);
        }
        throw new IllegalStateException("connector task did not reach RUNNING: " + body);
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String connectUrl() {
        return "http://localhost:" + connectPort;
    }

    private ConsumerRecord<String, String> consumeOne() {
        Properties properties = new Properties();
        properties.putAll(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "outbox-routing-test",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        "org.apache.kafka.common.serialization.StringDeserializer",
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        "org.apache.kafka.common.serialization.StringDeserializer"));

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(List.of(TOPIC));
            long deadline = System.currentTimeMillis() + Duration.ofMinutes(1).toMillis();
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
                if (!records.isEmpty()) {
                    return records.iterator().next();
                }
            }
        }
        throw new IllegalStateException("no record published to " + TOPIC);
    }
}
