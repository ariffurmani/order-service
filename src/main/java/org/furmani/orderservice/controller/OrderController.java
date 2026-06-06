package org.furmani.orderservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.furmani.orderservice.dto.CreateOrderRequest;
import org.furmani.orderservice.dto.OrderResponse;
import org.furmani.orderservice.dto.UpdateOrderStatusRequest;
import org.furmani.orderservice.response.ApiResponse;
import org.furmani.orderservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class OrderController {

    private final OrderService orderService;

    /**
     * Create a new order
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        log.info("[OrderController] createOrder() - START - Creating new order. customerId: {}, items count: {}",
                request.getCustomerId(), request.getItems().size());
        log.debug("[OrderController] createOrder() - Request details: {}", request);

        try {
            OrderResponse orderResponse = orderService.createOrder(request);
            log.info("[OrderController] createOrder() - SUCCESS - Order created with ID: {}, Order Number: {}",
                    orderResponse.getId(), orderResponse.getOrderNumber());

            ApiResponse<OrderResponse> response = ApiResponse.success(
                    "Order created successfully",
                    HttpStatus.CREATED.value(),
                    orderResponse
            );
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("[OrderController] createOrder() - ERROR - Failed to create order for customerId: {}",
                    request.getCustomerId(), e);
            throw e;
        }
    }

    /**
     * Get all orders
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        log.info("[OrderController] getAllOrders() - START - Fetching all orders");

        try {
            List<OrderResponse> orders = orderService.getAllOrders();
            log.info("[OrderController] getAllOrders() - SUCCESS - Retrieved {} orders", orders.size());
            log.debug("[OrderController] getAllOrders() - Order IDs: {}",
                    orders.stream().map(OrderResponse::getId).toList());

            ApiResponse<List<OrderResponse>> response = ApiResponse.success(
                    "Orders retrieved successfully",
                    orders
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("[OrderController] getAllOrders() - ERROR - Failed to retrieve orders", e);
            throw e;
        }
    }

    /**
     * Get order by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long id) {
        log.info("[OrderController] getOrderById() - START - Fetching order ID: {}", id);

        try {
            OrderResponse order = orderService.getOrderById(id);
            log.info("[OrderController] getOrderById() - SUCCESS - Order retrieved. Order Number: {}, Status: {}",
                    order.getOrderNumber(), order.getOrderStatus());
            log.debug("[OrderController] getOrderById() - Order Total Amount: {}", order.getTotalAmount());

            ApiResponse<OrderResponse> response = ApiResponse.success(
                    "Order retrieved successfully",
                    order
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("[OrderController] getOrderById() - ERROR - Failed to retrieve order ID: {}", id, e);
            throw e;
        }
    }

    /**
     * Get orders by customer ID
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByCustomerId(
            @PathVariable Long customerId) {
        log.info("[OrderController] getOrdersByCustomerId() - START - Fetching orders for customer ID: {}", customerId);

        try {
            List<OrderResponse> orders = orderService.getOrdersByCustomerId(customerId);
            log.info("[OrderController] getOrdersByCustomerId() - SUCCESS - Retrieved {} orders for customer ID: {}",
                    orders.size(), customerId);
            log.debug("[OrderController] getOrdersByCustomerId() - Order Numbers: {}",
                    orders.stream().map(OrderResponse::getOrderNumber).toList());

            ApiResponse<List<OrderResponse>> response = ApiResponse.success(
                    "Orders retrieved successfully for customer",
                    orders
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("[OrderController] getOrdersByCustomerId() - ERROR - Failed to retrieve orders for customer ID: {}",
                    customerId, e);
            throw e;
        }
    }

    /**
     * Update order status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        log.info("[OrderController] updateOrderStatus() - START - Updating order ID: {} to status: {}",
                id, request.getOrderStatus());

        try {
            OrderResponse updatedOrder = orderService.updateOrderStatus(id, request.getOrderStatus());
            log.info("[OrderController] updateOrderStatus() - SUCCESS - Order ID: {} status updated to: {}",
                    id, updatedOrder.getOrderStatus());
            log.debug("[OrderController] updateOrderStatus() - Updated Order Number: {}",
                    updatedOrder.getOrderNumber());

            ApiResponse<OrderResponse> response = ApiResponse.success(
                    "Order status updated successfully",
                    updatedOrder
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("[OrderController] updateOrderStatus() - ERROR - Failed to update order ID: {} to status: {}",
                    id, request.getOrderStatus(), e);
            throw e;
        }
    }

    /**
     * Delete/Cancel order
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(
            @PathVariable Long id) {
        log.info("[OrderController] deleteOrder() - START - Deleting order ID: {}", id);

        try {
            orderService.deleteOrder(id);
            log.info("[OrderController] deleteOrder() - SUCCESS - Order ID: {} deleted successfully", id);

            ApiResponse<Void> response = ApiResponse.error(
                    "Order deleted successfully",
                    HttpStatus.NO_CONTENT.value()
            );
            return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            log.error("[OrderController] deleteOrder() - ERROR - Failed to delete order ID: {}", id, e);
            throw e;
        }
    }
}

