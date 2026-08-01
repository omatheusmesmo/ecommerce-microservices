package com.ecommerce.service;

import com.ecommerce.dto.CreateOrderRequest;
import com.ecommerce.dto.OrderResponse;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.OutboxEvent;
import com.ecommerce.event.OrderCreatedEvent;
import com.ecommerce.event.OrderStatusChangedEvent;
import com.ecommerce.repository.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrderService {

    private static final Logger LOG = Logger.getLogger(OrderService.class);

    @Inject
    OrderRepository orderRepository;

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        LOG.infof("Creating order for customer: %s", request.customerName());

        Order order = new Order(request.customerName(), request.customerEmail());
        order.shippingCost = request.shippingCost();

        for (var itemReq : request.items()) {
            OrderItem item =
                    new OrderItem(itemReq.productId(), itemReq.productName(), itemReq.quantity(), itemReq.unitPrice());
            order.addItem(item);
        }

        order.calculateTotal();

        orderRepository.persist(order);

        LOG.infof(
                "Order %d created with %d items, total: %s",
                order.id, order.getItems().size(), order.totalAmount);

        OrderResponse response = OrderResponse.from(order);

        OrderCreatedEvent event = OrderCreatedEvent.from(response);

        OutboxEvent outboxEvent = null;
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            LOG.debugf("Serialized OrderCreatedEvent: %s", eventJson);

            outboxEvent = new OutboxEvent("Order", order.id.toString(), "OrderCreated", eventJson);
            outboxEvent.persist();

            LOG.infof(
                    "Event persisted to outbox: aggregate_type=%s, event_type=%s, aggregate_id=%s",
                    "Order", "OrderCreated", order.id);
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Failed to serialize OrderCreatedEvent to JSON for order: %d", order.id);
        }

        return response;
    }

    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    public OrderResponse findById(Long id) {
        LOG.debugf("Finding order by ID: %d", id);

        return orderRepository
                .findByIdWithItems(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + id));
    }

    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    public List<OrderResponse> findAll() {
        LOG.debug("Listing all orders");

        return orderRepository.listAll().stream()
                .map(OrderResponse::fromWithoutItems)
                .toList();
    }

    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    public List<OrderResponse> findByStatus(OrderStatus status) {
        LOG.debugf("Finding orders by status: %s", status);

        return orderRepository.findByStatus(status).stream()
                .map(OrderResponse::fromWithoutItems)
                .toList();
    }

    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    public List<OrderResponse> findByCustomerEmail(String email) {
        LOG.debugf("Finding orders by customer email: %s", email);

        return orderRepository.findByCustomerEmail(email).stream()
                .map(OrderResponse::fromWithoutItems)
                .toList();
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        LOG.infof("Updating order %d status to:  %s", id, newStatus);

        Order order = orderRepository
                .findByIdOptional(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + id));

        return applyTransition(order, newStatus);
    }

    @Transactional
    public void cancelOrder(Long id) {
        LOG.infof("Cancelling order:  %d", id);

        Order order = orderRepository
                .findByIdOptional(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + id));

        applyTransition(order, OrderStatus.CANCELLED);
    }

    private OrderResponse applyTransition(Order order, OrderStatus newStatus) {
        OrderStatus oldStatus = order.status;

        if (!oldStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException("Cannot transition order from " + oldStatus + " to " + newStatus);
        }

        order.status = newStatus;
        orderRepository.persist(order);

        LOG.infof("Order %d status updated from %s to %s", order.id, oldStatus, newStatus);

        OrderStatusChangedEvent event =
                new OrderStatusChangedEvent(order.id, oldStatus, newStatus, order.customerEmail, LocalDateTime.now());

        OutboxEvent outboxEvent = null;
        try {
            outboxEvent = new OutboxEvent(
                    "Order", order.id.toString(), "OrderStatusChanged", objectMapper.writeValueAsString(event));
            outboxEvent.persist();
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Cannot serialize event to JSON");
        }

        return OrderResponse.fromWithoutItems(order);
    }
}
