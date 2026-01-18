package com.ecommerce.service;

import com.ecommerce.dto.*;
import com.ecommerce. entity.Order;
import com. ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderStatus;
import com. ecommerce.messaging.OrderEventProducer;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce. repository.OrderItemRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject. Inject;
import jakarta.transaction. Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java. util. List;
import java.util.NoSuchElementException;

@ApplicationScoped
public class OrderService {

    private static final Logger LOG = Logger.getLogger(OrderService.class);

    @Inject
    OrderRepository orderRepository;

    @Inject
    OrderItemRepository orderItemRepository;

    @Inject
    OrderEventProducer eventProducer;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        LOG.infof("Creating order for customer: %s", request.customerName());

        Order order = new Order(request.customerName(), request.customerEmail());

        for (var itemReq : request.items()) {
            OrderItem item = new OrderItem(
                    itemReq.productId(),
                    itemReq.productName(),
                    itemReq.quantity(),
                    itemReq.unitPrice()
            );
            order.addItem(item);
        }

        order.calculateTotal();

        orderRepository.persist(order);

        LOG.infof("Order %d created with %d items, total: %s",
                order.id, order.getItems().size(), order.totalAmount);

        OrderResponse response = OrderResponse.from(order);

        OrderCreatedEvent event = OrderCreatedEvent. from(response);
        eventProducer. publishOrderCreated(event);

        return response;
    }

    public OrderResponse findById(Long id) {
        LOG.debugf("Finding order by ID: %d", id);

        return orderRepository.findByIdWithItems(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + id));
    }

    public List<OrderResponse> findAll() {
        LOG.debug("Listing all orders");

        return orderRepository.listAll().stream()
                .map(OrderResponse::fromWithoutItems)
                .toList();
    }

    public List<OrderResponse> findByStatus(OrderStatus status) {
        LOG.debugf("Finding orders by status: %s", status);

        return orderRepository.findByStatus(status).stream()
                .map(OrderResponse::fromWithoutItems)
                .toList();
    }

    public List<OrderResponse> findByCustomerEmail(String email) {
        LOG.debugf("Finding orders by customer email: %s", email);

        return orderRepository.findByCustomerEmail(email).stream()
                .map(OrderResponse::fromWithoutItems)
                .toList();
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        LOG.infof("Updating order %d status to:  %s", id, newStatus);

        Order order = orderRepository.findByIdOptional(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + id));

        OrderStatus oldStatus = order.status;

        order.status = newStatus;
        orderRepository.persist(order);

        LOG.infof("Order %d status updated from %s to %s", id, oldStatus, newStatus);

        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                order.id,
                oldStatus,
                newStatus,
                order.customerEmail,
                LocalDateTime.now()
        );
        eventProducer.publishOrderStatusChanged(event);

        return OrderResponse.fromWithoutItems(order);
    }

    @Transactional
    public void cancelOrder(Long id) {
        LOG.infof("Cancelling order:  %d", id);

        Order order = orderRepository.findByIdOptional(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + id));

        if (order.status == OrderStatus. DELIVERED) {
            throw new IllegalStateException("Cannot cancel a delivered order");
        }

        if (order.status == OrderStatus. CANCELLED) {
            throw new IllegalStateException("Order is already cancelled");
        }

        OrderStatus oldStatus = order.status;
        order.status = OrderStatus.CANCELLED;
        orderRepository.persist(order);

        LOG.infof("Order %d cancelled", id);

        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                order. id,
                oldStatus,
                OrderStatus.CANCELLED,
                order.customerEmail,
                LocalDateTime.now()
        );
        eventProducer.publishOrderStatusChanged(event);
    }
}