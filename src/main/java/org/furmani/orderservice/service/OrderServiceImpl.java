package org.furmani.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;

    public OrderResponse createOrder(CreateOrderRequest request) {
        log.debug("[OrderServiceImpl] createOrder() - START - Processing create order request for customerId: {}",
                request.getCustomerId());

        try {
            String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            log.debug("[OrderServiceImpl] createOrder() - Generated order number: {}", orderNumber);

            BigDecimal totalAmount = BigDecimal.ZERO;

            Order order = Order.builder()
                    .orderNumber(orderNumber)
                    .customerId(request.getCustomerId())
                    .totalAmount(totalAmount)
                    .orderStatus(OrderStatus.PENDING)
                    .build();

            log.debug("[OrderServiceImpl] createOrder() - Created order entity for customerId: {}",
                    request.getCustomerId());

            for (OrderItemRequest itemReq : request.getItems()) {
                BigDecimal itemTotal = itemReq.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
                totalAmount = totalAmount.add(itemTotal);

                log.debug("[OrderServiceImpl] createOrder() - Processing order item - productId: {}, quantity: {}, price: {}, itemTotal: {}",
                        itemReq.getProductId(), itemReq.getQuantity(), itemReq.getPrice(), itemTotal);

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
            log.info("[OrderServiceImpl] createOrder() - Order summary - orderNumber: {}, customerId: {}, items count: {}, totalAmount: {}",
                    orderNumber, request.getCustomerId(), request.getItems().size(), totalAmount);

            Order savedOrder = orderRepository.save(order);
            log.info("[OrderServiceImpl] createOrder() - SUCCESS - Order saved to database with ID: {}, OrderNumber: {}",
                    savedOrder.getId(), savedOrder.getOrderNumber());

            return mapToOrderResponse(savedOrder, false);
        } catch (Exception e) {
            log.error("[OrderServiceImpl] createOrder() - ERROR - Failed to create order for customerId: {}",
                    request.getCustomerId(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.debug("[OrderServiceImpl] getOrderById() - START - Fetching order ID: {}", id);

        try {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("[OrderServiceImpl] getOrderById() - WARN - Order not found with ID: {}", id);
                        return new OrderNotFoundException("Order not found with ID: " + id);
                    });

            log.info("[OrderServiceImpl] getOrderById() - SUCCESS - Order found with ID: {}, OrderNumber: {}, Status: {}",
                    id, order.getOrderNumber(), order.getOrderStatus());

            return mapToOrderResponse(order, true);
        } catch (Exception e) {
            log.error("[OrderServiceImpl] getOrderById() - ERROR - Exception while fetching order ID: {}", id, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        log.debug("[OrderServiceImpl] getAllOrders() - START - Fetching all orders from database");

        try {
            List<Order> ordersFromDb = orderRepository.findAll();
            log.info("[OrderServiceImpl] getAllOrders() - SUCCESS - Retrieved {} orders from database", ordersFromDb.size());

            if (ordersFromDb.isEmpty()) {
                log.warn("[OrderServiceImpl] getAllOrders() - WARN - No orders found in database");
            }

            List<OrderResponse> result = ordersFromDb
                    .stream()
                    .map(order -> mapToOrderResponse(order, false))
                    .collect(Collectors.toList());

            log.debug("[OrderServiceImpl] getAllOrders() - Mapped {} orders to OrderResponse objects", result.size());
            return result;
        } catch (Exception e) {
            log.error("[OrderServiceImpl] getAllOrders() - ERROR - Exception while fetching all orders", e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        log.debug("[OrderServiceImpl] getOrdersByCustomerId() - START - Fetching orders for customerId: {}", customerId);

        try {
            List<Order> orders = orderRepository.findByCustomerId(customerId);

            if (orders.isEmpty()) {
                log.warn("[OrderServiceImpl] getOrdersByCustomerId() - WARN - No orders found for customerId: {}", customerId);
                throw new OrderNotFoundException("No orders found for customer ID: " + customerId);
            }

            log.info("[OrderServiceImpl] getOrdersByCustomerId() - SUCCESS - Retrieved {} orders for customerId: {}",
                    orders.size(), customerId);
            log.debug("[OrderServiceImpl] getOrdersByCustomerId() - Order IDs: {}",
                    orders.stream().map(Order::getId).map(String::valueOf).collect(Collectors.joining(", ")));

            return orders.stream()
                    .map(order -> mapToOrderResponse(order, false))
                    .collect(Collectors.toList());
        } catch (OrderNotFoundException e) {
            log.error("[OrderServiceImpl] getOrdersByCustomerId() - ERROR - Order not found for customerId: {}", customerId);
            throw e;
        } catch (Exception e) {
            log.error("[OrderServiceImpl] getOrdersByCustomerId() - ERROR - Exception while fetching orders for customerId: {}",
                    customerId, e);
            throw e;
        }
    }

    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        log.debug("[OrderServiceImpl] updateOrderStatus() - START - Updating order ID: {} to status: {}", id, newStatus);

        try {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("[OrderServiceImpl] updateOrderStatus() - WARN - Order not found with ID: {}", id);
                        return new OrderNotFoundException("Order not found with ID: " + id);
                    });

            OrderStatus previousStatus = order.getOrderStatus();
            order.setOrderStatus(newStatus);
            log.debug("[OrderServiceImpl] updateOrderStatus() - Status changed from {} to {} for order ID: {}",
                    previousStatus, newStatus, id);

            Order updatedOrder = orderRepository.save(order);
            log.info("[OrderServiceImpl] updateOrderStatus() - SUCCESS - Order ID: {} status updated. Previous: {}, Current: {}",
                    id, previousStatus, newStatus);

            return mapToOrderResponse(updatedOrder, false);
        } catch (Exception e) {
            log.error("[OrderServiceImpl] updateOrderStatus() - ERROR - Failed to update order ID: {} to status: {}",
                    id, newStatus, e);
            throw e;
        }
    }

    public void deleteOrder(Long id) {
        log.debug("[OrderServiceImpl] deleteOrder() - START - Deleting order ID: {}", id);

        try {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("[OrderServiceImpl] deleteOrder() - WARN - Order not found with ID: {}", id);
                        return new OrderNotFoundException("Order not found with ID: " + id);
                    });

            log.debug("[OrderServiceImpl] deleteOrder() - About to delete order ID: {}, OrderNumber: {}, customerId: {}",
                    id, order.getOrderNumber(), order.getCustomerId());

            orderRepository.delete(order);
            log.info("[OrderServiceImpl] deleteOrder() - SUCCESS - Order ID: {} deleted successfully. OrderNumber: {}",
                    id, order.getOrderNumber());
        } catch (Exception e) {
            log.error("[OrderServiceImpl] deleteOrder() - ERROR - Failed to delete order ID: {}", id, e);
            throw e;
        }
    }

    private OrderResponse mapToOrderResponse(Order order, boolean enrichProductDetails) {
        log.debug("[OrderServiceImpl] mapToOrderResponse() - START - Mapping order ID: {} to response. enrichProductDetails: {}",
                order.getId(), enrichProductDetails);

        try {
            List<OrderItemResponse> items = order.getItems().stream()
                    .map(item -> mapToOrderItemResponse(item, enrichProductDetails))
                    .collect(Collectors.toList());

            log.debug("[OrderServiceImpl] mapToOrderResponse() - Mapped {} items for order ID: {}",
                    items.size(), order.getId());

            OrderResponse response = OrderResponse.builder()
                    .id(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .customerId(order.getCustomerId())
                    .items(items)
                    .totalAmount(order.getTotalAmount())
                    .orderStatus(order.getOrderStatus())
                    .createdAt(order.getCreatedAt())
                    .build();

            log.debug("[OrderServiceImpl] mapToOrderResponse() - SUCCESS - Order response created for order ID: {}",
                    order.getId());
            return response;
        } catch (Exception e) {
            log.error("[OrderServiceImpl] mapToOrderResponse() - ERROR - Failed to map order ID: {}",
                    order.getId(), e);
            throw e;
        }
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem item, boolean enrichProductDetails) {
        log.debug("[OrderServiceImpl] mapToOrderItemResponse() - START - Mapping order item ID: {}, productId: {}",
                item.getId(), item.getProductId());

        try {
            OrderItemResponse.OrderItemResponseBuilder builder = OrderItemResponse.builder()
                    .id(item.getId())
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .totalAmount(item.getTotalAmount());

            if (enrichProductDetails) {
                log.debug("[OrderServiceImpl] mapToOrderItemResponse() - Fetching product details for productId: {}",
                        item.getProductId());
                productServiceClient.getProductDetails(item.getProductId())
                        .ifPresent(productDetails -> {
                            log.debug("[OrderServiceImpl] mapToOrderItemResponse() - Product details found for productId: {}, productName: {}",
                                    item.getProductId(), productDetails.getProductName());
                            applyProductDetails(builder, productDetails);
                        });
            }

            OrderItemResponse response = builder.build();
            log.debug("[OrderServiceImpl] mapToOrderItemResponse() - SUCCESS - Order item response created for item ID: {}",
                    item.getId());
            return response;
        } catch (Exception e) {
            log.error("[OrderServiceImpl] mapToOrderItemResponse() - ERROR - Failed to map order item ID: {}",
                    item.getId(), e);
            throw e;
        }
    }

    private void applyProductDetails(OrderItemResponse.OrderItemResponseBuilder builder, ProductDetails productDetails) {
        log.debug("[OrderServiceImpl] applyProductDetails() - START - Applying product details. productName: {}",
                productDetails.getProductName());

        try {
            builder.productName(productDetails.getProductName())
                    .productDescription(productDetails.getProductDescription());

            log.debug("[OrderServiceImpl] applyProductDetails() - SUCCESS - Product details applied. productName: {}",
                    productDetails.getProductName());
        } catch (Exception e) {
            log.error("[OrderServiceImpl] applyProductDetails() - ERROR - Failed to apply product details for productName: {}",
                    productDetails.getProductName(), e);
            throw e;
        }
    }
}

