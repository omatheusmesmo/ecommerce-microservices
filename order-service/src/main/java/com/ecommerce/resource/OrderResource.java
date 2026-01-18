package com.ecommerce.resource;

import com.ecommerce.dto.CreateOrderRequest;
import com.ecommerce.dto.OrderResponse;
import com.ecommerce. dto.UpdateOrderStatusRequest;
import com.ecommerce.entity.OrderStatus;
import com. ecommerce.service.OrderService;
import jakarta.inject. Inject;
import jakarta.validation. Valid;
import jakarta.ws. rs.*;
import jakarta.ws. rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging. Logger;

import java.util. List;

@Path("/orders")
@Produces(MediaType. APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    private static final Logger LOG = Logger.getLogger(OrderResource.class);

    @Inject
    OrderService orderService;

    @POST
    public Response createOrder(@Valid CreateOrderRequest request) {
        LOG.infof("POST /orders - Creating order for: %s", request.customerName());

        OrderResponse response = orderService.createOrder(request);

        return Response
                .status(Response. Status.CREATED)
                .entity(response)
                .build();
    }

    @GET
    public List<OrderResponse> findAll() {
        LOG.info("GET /orders - Listing all orders");
        return orderService. findAll();
    }

    @GET
    @Path("/{id}")
    public OrderResponse findById(@PathParam("id") Long id) {
        LOG.infof("GET /orders/%d", id);
        return orderService.findById(id);
    }

    @GET
    @Path("/status/{status}")
    public List<OrderResponse> findByStatus(@PathParam("status") OrderStatus status) {
        LOG.infof("GET /orders/status/%s", status);
        return orderService.findByStatus(status);
    }

    @GET
    @Path("/customer/{email}")
    public List<OrderResponse> findByCustomerEmail(@PathParam("email") String email) {
        LOG.infof("GET /orders/customer/%s", email);
        return orderService.findByCustomerEmail(email);
    }

    @PUT
    @Path("/{id}/status")
    public OrderResponse updateStatus(
            @PathParam("id") Long id,
            @Valid UpdateOrderStatusRequest request
    ) {
        LOG.infof("PUT /orders/%d/status - New status:  %s", id, request.status());
        return orderService.updateStatus(id, request.status());
    }

    @PATCH
    @Path("/{id}/cancel")
    public OrderResponse cancelOrder(@PathParam("id") Long id) {
        LOG.infof("PATCH /orders/%d/cancel - Cancelling order", id);

        orderService.cancelOrder(id);

        return orderService. findById(id);
    }
}