package com.ecommerce.service;

import com.ecommerce.dto.CreateOrderRequest;
import com.ecommerce.dto.OrderItemRequest;
import com.ecommerce.dto.OrderResponse;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.OutboxEvent;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.valueobject.Money;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class OrderServiceTest {

    @Inject
    OrderService orderService;

    @Inject
    OrderRepository orderRepository;

    private CreateOrderRequest createOrderRequest() {
        return new CreateOrderRequest(
                "Jane Doe",
                "jane@example.com",
                List.of(new OrderItemRequest("prod-1", "Gaming Chair", 2, new Money(new BigDecimal("100.00"), "BRL"))),
                new Money(new BigDecimal("15.00"), "BRL"));
    }

    private Long persistOrder(OrderStatus status) {
        Order order = new Order("Jane Doe", "jane@example.com");
        order.status = status;
        orderRepository.persist(order);
        return order.id;
    }

    @Test
    @TestTransaction
    void createOrder_persistsOrderAndWritesMatchingOutboxEventAtomically() {
        long outboxCountBefore = OutboxEvent.count();

        OrderResponse response = orderService.createOrder(createOrderRequest());

        assertNotNull(response.id());
        assertEquals(new Money(new BigDecimal("215.00"), "BRL"), response.totalAmount());
        assertTrue(orderRepository.findByIdOptional(response.id()).isPresent());

        assertEquals(outboxCountBefore + 1, OutboxEvent.count());
        OutboxEvent event = OutboxEvent
                .find("aggregateType = ?1 and aggregateId = ?2", "Order", response.id().toString())
                .firstResult();
        assertNotNull(event);
        assertEquals("OrderCreated", event.eventType);
    }

    @Test
    void createOrder_outboxEventReachesKafka() {
        String marker = UUID.randomUUID().toString();
        CreateOrderRequest request = new CreateOrderRequest(
                "Kafka Flow Test " + marker,
                "kafka-flow@example.com",
                List.of(new OrderItemRequest("prod-1", "Gaming Chair", 1, new Money(new BigDecimal("50.00"), "BRL"))),
                new Money(new BigDecimal("5.00"), "BRL"));

        OrderResponse response = orderService.createOrder(request);

        Optional<String> message = waitForKafkaMessage("outbox.event.Order",
                value -> value.contains("\"orderId\":" + response.id()), 10);

        assertTrue(message.isPresent(), "expected the order-created outbox event to reach Kafka");
    }

    @Test
    @TestTransaction
    void findById_notFound_throwsNoSuchElementException() {
        assertThrows(NoSuchElementException.class, () -> orderService.findById(Long.MAX_VALUE));
    }

    @Test
    @TestTransaction
    void cancelOrder_pendingOrder_transitionsToCancelled() {
        Long id = persistOrder(OrderStatus.PENDING);

        orderService.cancelOrder(id);

        assertEquals(OrderStatus.CANCELLED, orderRepository.findById(id).status);
    }

    @Test
    @TestTransaction
    void cancelOrder_deliveredOrder_throwsIllegalStateException() {
        Long id = persistOrder(OrderStatus.DELIVERED);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.cancelOrder(id));
        assertEquals("Cannot transition order from DELIVERED to CANCELLED", ex.getMessage());
        assertEquals(OrderStatus.DELIVERED, orderRepository.findById(id).status);
    }

    @Test
    @TestTransaction
    void cancelOrder_alreadyCancelledOrder_throwsIllegalStateException() {
        Long id = persistOrder(OrderStatus.CANCELLED);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.cancelOrder(id));
        assertEquals("Cannot transition order from CANCELLED to CANCELLED", ex.getMessage());
    }

    @Test
    @TestTransaction
    void cancelOrder_notFound_throwsNoSuchElementException() {
        assertThrows(NoSuchElementException.class, () -> orderService.cancelOrder(Long.MAX_VALUE));
    }

    @Test
    @TestTransaction
    void updateStatus_updatesOrderAndReturnsNewStatus() {
        Long id = persistOrder(OrderStatus.PENDING);

        OrderResponse response = orderService.updateStatus(id, OrderStatus.CONFIRMED);

        assertEquals(OrderStatus.CONFIRMED, response.status());
        assertEquals(OrderStatus.CONFIRMED, orderRepository.findById(id).status);
    }

    @Test
    @TestTransaction
    void updateStatus_notFound_throwsNoSuchElementException() {
        assertThrows(NoSuchElementException.class,
                () -> orderService.updateStatus(Long.MAX_VALUE, OrderStatus.CONFIRMED));
    }

    @Test
    @TestTransaction
    void updateStatus_illegalTransition_throwsAndKeepsStatus() {
        Long id = persistOrder(OrderStatus.DELIVERED);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.updateStatus(id, OrderStatus.PENDING));
        assertEquals("Cannot transition order from DELIVERED to PENDING", ex.getMessage());
        assertEquals(OrderStatus.DELIVERED, orderRepository.findById(id).status);
    }

    @Test
    @TestTransaction
    void updateStatus_skippingStates_isRejected() {
        Long id = persistOrder(OrderStatus.PENDING);

        assertThrows(IllegalStateException.class,
                () -> orderService.updateStatus(id, OrderStatus.SHIPPED));
        assertEquals(OrderStatus.PENDING, orderRepository.findById(id).status);
    }

    @Test
    @TestTransaction
    void updateStatus_walksTheHappyPath_toDelivered() {
        Long id = persistOrder(OrderStatus.PENDING);

        orderService.updateStatus(id, OrderStatus.CONFIRMED);
        orderService.updateStatus(id, OrderStatus.SHIPPED);
        orderService.updateStatus(id, OrderStatus.DELIVERED);

        assertEquals(OrderStatus.DELIVERED, orderRepository.findById(id).status);
    }

    private Optional<String> waitForKafkaMessage(String topic, Predicate<String> predicate, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
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
