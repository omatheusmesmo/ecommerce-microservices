package com.ecommerce.consumer;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ReservationStatus;
import com.ecommerce.entity.StockReservation;
import com.ecommerce.event.ConfirmStockReservationCommand;
import com.ecommerce.event.ReleaseStockCommand;
import com.ecommerce.event.ReserveStockCommand;
import com.ecommerce.messaging.StockReplyProducer;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.ProductService;
import com.ecommerce.valueobject.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StockCommandConsumerTest {

    private static final String COMMAND_TOPIC = "outbox.event.OrderCommand";
    private static final String REPLY_TOPIC = "product.stock.replies";
    private static final AtomicLong ORDER_IDS = new AtomicLong(System.currentTimeMillis());

    @Inject
    ProductService productService;

    @Inject
    ProductRepository productRepository;

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void aReserveCommandHoldsStockAndAnswersStockReserved() throws Exception {
        String productId = newProduct(10);
        long orderId = ORDER_IDS.incrementAndGet();

        send(
                StockCommandConsumer.RESERVE_STOCK,
                orderId,
                new ReserveStockCommand(orderId, List.of(new ReserveStockCommand.Item(productId, 4))));

        awaitReservation(orderId, ReservationStatus.RESERVED);
        assertEquals(4, reload(productId).totalReserved());
        assertEquals(10, reload(productId).totalOnHand());

        assertTrue(
                replyArrived(orderId, StockReplyProducer.STOCK_RESERVED),
                "Expected a StockReserved reply for order " + orderId);
    }

    @Test
    public void aReserveCommandBeyondAvailabilityAnswersStockRejected() throws Exception {
        String productId = newProduct(2);
        long orderId = ORDER_IDS.incrementAndGet();

        send(
                StockCommandConsumer.RESERVE_STOCK,
                orderId,
                new ReserveStockCommand(orderId, List.of(new ReserveStockCommand.Item(productId, 5))));

        awaitReservation(orderId, ReservationStatus.REJECTED);
        assertEquals(0, reload(productId).totalReserved());

        assertTrue(
                replyArrived(orderId, StockReplyProducer.STOCK_REJECTED),
                "Expected a StockRejected reply for order " + orderId);
    }

    @Test
    public void aConfirmCommandConsumesTheReservationAndAnswersStockConfirmed() throws Exception {
        String productId = newProduct(10);
        long orderId = ORDER_IDS.incrementAndGet();

        send(
                StockCommandConsumer.RESERVE_STOCK,
                orderId,
                new ReserveStockCommand(orderId, List.of(new ReserveStockCommand.Item(productId, 3))));
        awaitReservation(orderId, ReservationStatus.RESERVED);

        send(StockCommandConsumer.CONFIRM_STOCK_RESERVATION, orderId, new ConfirmStockReservationCommand(orderId));
        awaitReservation(orderId, ReservationStatus.CONFIRMED);

        Product product = reload(productId);
        assertEquals(7, product.totalOnHand());
        assertEquals(0, product.totalReserved());

        assertTrue(
                replyArrived(orderId, StockReplyProducer.STOCK_CONFIRMED),
                "Expected a StockConfirmed reply for order " + orderId);
    }

    @Test
    public void aReleaseCommandGivesTheStockBackAndAnswersStockReleased() throws Exception {
        String productId = newProduct(10);
        long orderId = ORDER_IDS.incrementAndGet();

        send(
                StockCommandConsumer.RESERVE_STOCK,
                orderId,
                new ReserveStockCommand(orderId, List.of(new ReserveStockCommand.Item(productId, 3))));
        awaitReservation(orderId, ReservationStatus.RESERVED);

        send(StockCommandConsumer.RELEASE_STOCK, orderId, new ReleaseStockCommand(orderId));
        awaitReservation(orderId, ReservationStatus.RELEASED);

        Product product = reload(productId);
        assertEquals(10, product.totalOnHand());
        assertEquals(0, product.totalReserved());

        assertTrue(
                replyArrived(orderId, StockReplyProducer.STOCK_RELEASED),
                "Expected a StockReleased reply for order " + orderId);
    }

    @Test
    public void aCommandWithoutAnEventTypeHeaderIsRoutedToTheDeadLetterQueue() {
        String marker = UUID.randomUUID().toString();

        try (KafkaProducer<String, String> producer = createProducer()) {
            producer.send(
                    new ProducerRecord<>(COMMAND_TOPIC, "no-header", "{\"orderId\":1,\"marker\":\"" + marker + "\"}"));
        }

        assertTrue(dlqReceived(marker), "Expected a command with no eventType header to land in the stock-command DLQ");
    }

    private String newProduct(int stock) {
        Category category = new Category("Stock Command Category", null);
        categoryRepository.persist(category);
        Product product = new Product(
                "Stock Command Product",
                "Description",
                new Money(new BigDecimal("100.00"), "BRL"),
                stock,
                category.id.toString());
        return productService.create(product).id.toString();
    }

    private Product reload(String productId) {
        return productRepository.findById(new ObjectId(productId));
    }

    private void send(String eventType, long orderId, Object command) throws Exception {
        String json = objectMapper.writeValueAsString(command);
        try (KafkaProducer<String, String> producer = createProducer()) {
            ProducerRecord<String, String> record = new ProducerRecord<>(COMMAND_TOPIC, String.valueOf(orderId), json);
            record.headers()
                    .add(new RecordHeader(
                            "eventId", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)))
                    .add(new RecordHeader("eventType", eventType.getBytes(StandardCharsets.UTF_8)));
            producer.send(record).get();
        }
    }

    private void awaitReservation(long orderId, ReservationStatus expected) {
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            StockReservation reservation =
                    StockReservation.findByOrderId(orderId).orElse(null);
            assertNotNull(reservation, "No reservation stored for order " + orderId);
            assertEquals(expected, reservation.status);
        });
    }

    private boolean replyArrived(long orderId, String eventType) {
        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            consumer.subscribe(List.of(REPLY_TOPIC));
            long deadline = System.currentTimeMillis() + 30_000;
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, String> record : records) {
                    if (!String.valueOf(orderId).equals(record.key())) {
                        continue;
                    }
                    var header = record.headers().lastHeader("eventType");
                    if (header != null && eventType.equals(new String(header.value(), StandardCharsets.UTF_8))) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private boolean dlqReceived(String marker) {
        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            consumer.subscribe(List.of(COMMAND_TOPIC + ".product-service.dlq"));
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
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "stock-command-test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(props);
    }
}
