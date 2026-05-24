package org.furmani.orderservice.service;

import org.furmani.orderservice.dto.CreateOrderRequest;
import org.furmani.orderservice.dto.OrderResponse;
import org.furmani.orderservice.models.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);
    OrderResponse getOrderById(Long id);
    List<OrderResponse> getAllOrders();
    List<OrderResponse> getOrdersByCustomerId(Long customerId);
    OrderResponse updateOrderStatus(Long id, OrderStatus newStatus);
    void deleteOrder(Long id);
//    OrderResponse mapToOrderResponse(Order order);
}

