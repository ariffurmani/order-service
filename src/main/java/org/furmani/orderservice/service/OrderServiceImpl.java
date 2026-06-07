package org.furmani.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.furmani.orderservice.client.ProductServiceClient;
import org.furmani.orderservice.dto.CreateOrderRequest;
import org.furmani.orderservice.dto.OrderItemRequest;
import org.furmani.orderservice.dto.OrderItemResponse;
import org.furmani.orderservice.dto.OrderResponse;
import org.furmani.orderservice.dto.ProductDetails;
import org.furmani.orderservice.exception.ProductNotFoundException;
import org.furmani.orderservice.exception.ProductUnavailableException;
import org.furmani.orderservice.exception.OrderNotFoundException;
import org.furmani.orderservice.models.Order;
import org.furmani.orderservice.models.OrderItem;
import org.furmani.orderservice.models.OrderStatus;
import org.furmani.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
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

        // Keep outside try so catch-block compensation can access it.
        List<OrderItemRequest> decrementedItems = new ArrayList<>();

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
                ProductDetails productDetails = productServiceClient.getProductDetails(itemReq.getProductId())
                        .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + itemReq.getProductId()));

                if (productDetails.getPrice() == null) {
                    throw new ProductUnavailableException("Product price is unavailable for product ID: " + itemReq.getProductId());
                }

                Integer stockQuantity = productDetails.getStockQuantity();
                if (stockQuantity == null || stockQuantity <= 0) {
                    throw new ProductUnavailableException("Product is out of stock for product ID: " + itemReq.getProductId());
                }

                if (itemReq.getQuantity() > stockQuantity) {
                    throw new ProductUnavailableException("Requested quantity exceeds available stock for product ID: " + itemReq.getProductId());
                }

                // decrement stock in product service before finalizing order item
                log.debug("[OrderServiceImpl] createOrder() - Decrementing stock for productId: {}, quantity: {}",
                        itemReq.getProductId(), itemReq.getQuantity());
                productServiceClient.decrementStock(itemReq.getProductId(), itemReq.getQuantity());
                decrementedItems.add(itemReq);

                BigDecimal itemTotal = productDetails.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
                totalAmount = totalAmount.add(itemTotal);

                log.debug("[OrderServiceImpl] createOrder() - Processing order item - productId: {}, quantity: {}, price: {}, itemTotal: {}",
                        itemReq.getProductId(), itemReq.getQuantity(), productDetails.getPrice(), itemTotal);

                OrderItem item = OrderItem.builder()
                        .productId(itemReq.getProductId())
                        .quantity(itemReq.getQuantity())
                        .price(productDetails.getPrice())
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

            // attempt to compensate previously decremented stock if any
            try {
                if (!decrementedItems.isEmpty()) {
                    log.warn("[OrderServiceImpl] createOrder() - COMPENSATION - Attempting to restore stock for {} items", decrementedItems.size());
                    for (OrderItemRequest dec : decrementedItems) {
                        try {
                            productServiceClient.incrementStock(dec.getProductId(), dec.getQuantity());
                            log.info("[OrderServiceImpl] createOrder() - COMPENSATION - Restored stock for productId: {}, quantity: {}",
                                    dec.getProductId(), dec.getQuantity());
                        } catch (Exception compEx) {
                            log.error("[OrderServiceImpl] createOrder() - COMPENSATION - Failed to restore stock for productId: {}",
                                    dec.getProductId(), compEx);
                        }
                    }
                }
            } catch (Exception compOverall) {
                log.error("[OrderServiceImpl] createOrder() - COMPENSATION - Unexpected error during compensation", compOverall);
            }

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
                log.info("[OrderServiceImpl] getOrdersByCustomerId() - No orders found for customerId: {}", customerId);
                return List.of();
            }

            log.info("[OrderServiceImpl] getOrdersByCustomerId() - SUCCESS - Retrieved {} orders for customerId: {}",
                    orders.size(), customerId);
            log.debug("[OrderServiceImpl] getOrdersByCustomerId() - Order IDs: {}",
                    orders.stream().map(Order::getId).map(String::valueOf).collect(Collectors.joining(", ")));

            return orders.stream()
                    .map(order -> mapToOrderResponse(order, false))
                    .collect(Collectors.toList());
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
            validateStatusTransition(previousStatus, newStatus);

            order.setOrderStatus(newStatus);
            log.debug("[OrderServiceImpl] updateOrderStatus() - Status changed from {} to {} for order ID: {}",
                    previousStatus, newStatus, id);

            Order updatedOrder = orderRepository.save(order);

            if (previousStatus != OrderStatus.CANCELLED && newStatus == OrderStatus.CANCELLED) {
                restoreStockForOrder(order);
            }

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

            if (order.getOrderStatus() != OrderStatus.CANCELLED) {
                restoreStockForOrder(order);
            }

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
                try {
                    productServiceClient.getProductDetails(item.getProductId())
                            .ifPresent(productDetails -> {
                                log.debug("[OrderServiceImpl] mapToOrderItemResponse() - Product details found for productId: {}, productName: {}",
                                        item.getProductId(), productDetails.getProductName());
                                applyProductDetails(builder, productDetails);
                            });
                } catch (Exception ex) {
                    log.warn("[OrderServiceImpl] mapToOrderItemResponse() - WARN - Unable to enrich product details for productId: {}",
                            item.getProductId(), ex);
                }
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

    private void validateStatusTransition(OrderStatus previousStatus, OrderStatus newStatus) {
        if (previousStatus == OrderStatus.CANCELLED && newStatus != OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot change status of a cancelled order");
        }

        if (previousStatus == OrderStatus.DELIVERED && newStatus != OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Cannot change status of a delivered order");
        }
    }

    private void restoreStockForOrder(Order order) {
        log.warn("[OrderServiceImpl] restoreStockForOrder() - START - Restoring stock for order ID: {}, orderNumber: {}",
                order.getId(), order.getOrderNumber());

        List<OrderItem> restoredItems = new ArrayList<>();
        try {
            for (OrderItem item : order.getItems()) {
                productServiceClient.incrementStock(item.getProductId(), item.getQuantity());
                restoredItems.add(item);
                log.info("[OrderServiceImpl] restoreStockForOrder() - Restored stock for productId: {}, quantity: {}",
                        item.getProductId(), item.getQuantity());
            }
        } catch (Exception ex) {
            log.error("[OrderServiceImpl] restoreStockForOrder() - ERROR - Failed restoring stock. Attempting rollback for {} items",
                    restoredItems.size(), ex);

            for (OrderItem restoredItem : restoredItems) {
                try {
                    productServiceClient.decrementStock(restoredItem.getProductId(), restoredItem.getQuantity());
                } catch (Exception rollbackEx) {
                    log.error("[OrderServiceImpl] restoreStockForOrder() - ROLLBACK ERROR - Failed to rollback stock for productId: {}",
                            restoredItem.getProductId(), rollbackEx);
                }
            }

            throw new IllegalStateException("Failed to restore stock for order: " + order.getOrderNumber(), ex);
        }
    }
}

