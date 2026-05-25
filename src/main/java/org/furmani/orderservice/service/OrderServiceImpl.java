package org.furmani.orderservice.service;

import lombok.RequiredArgsConstructor;
import org.furmani.orderservice.client.ProductServiceClient;
import org.furmani.orderservice.dto.CreateOrderRequest;
import org.furmani.orderservice.dto.OrderItemRequest;
import org.furmani.orderservice.dto.OrderItemResponse;
import org.furmani.orderservice.dto.OrderResponse;
import org.furmani.orderservice.dto.ProductDetails;
import org.furmani.orderservice.exception.OrderNotFoundException;
import org.furmani.orderservice.models.Order;
import org.furmani.orderservice.models.OrderItem;
import org.furmani.orderservice.models.OrderStatus;
import org.furmani.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;

    public OrderResponse createOrder(CreateOrderRequest request) {
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        BigDecimal totalAmount = BigDecimal.ZERO;

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customerId(request.getCustomerId())
                .totalAmount(totalAmount)
                .orderStatus(OrderStatus.PENDING)
                .build();

        for (OrderItemRequest itemReq : request.getItems()) {
            BigDecimal itemTotal = itemReq.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem item = OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .quantity(itemReq.getQuantity())
                    .price(itemReq.getPrice())
                    .totalAmount(itemTotal)
                    .order(order)
                    .build();

            order.getItems().add(item);
        }

        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);
        return mapToOrderResponse(savedOrder, false);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + id));
        return mapToOrderResponse(order, true);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(order -> mapToOrderResponse(order, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        if (orders.isEmpty()) {
            throw new OrderNotFoundException("No orders found for customer ID: " + customerId);
        }
        return orders.stream()
                .map(order -> mapToOrderResponse(order, false))
                .collect(Collectors.toList());
    }

    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + id));

        order.setOrderStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);
        return mapToOrderResponse(updatedOrder, false);
    }

    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + id));
        orderRepository.delete(order);
    }

    private OrderResponse mapToOrderResponse(Order order, boolean enrichProductDetails) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> mapToOrderItemResponse(item, enrichProductDetails))
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .items(items)
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem item, boolean enrichProductDetails) {
        OrderItemResponse.OrderItemResponseBuilder builder = OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .totalAmount(item.getTotalAmount());

        if (enrichProductDetails) {
            productServiceClient.getProductDetails(item.getProductId())
                    .ifPresent(productDetails -> applyProductDetails(builder, productDetails));
        }

        return builder.build();
    }

    private void applyProductDetails(OrderItemResponse.OrderItemResponseBuilder builder, ProductDetails productDetails) {
        builder.productName(productDetails.getProductName())
                .productDescription(productDetails.getProductDescription());
    }
}

