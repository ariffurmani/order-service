package org.furmani.orderservice.controller;

import lombok.RequiredArgsConstructor;
import org.furmani.orderservice.dto.CreateOrderRequest;
import org.furmani.orderservice.dto.OrderResponse;
import org.furmani.orderservice.dto.UpdateOrderStatusRequest;
import org.furmani.orderservice.response.ApiResponse;
import org.furmani.orderservice.service.OrderServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class OrderController {

    private final OrderServiceImpl orderService;

    /**
     * Create a new order
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse orderResponse = orderService.createOrder(request);
        ApiResponse<OrderResponse> response = ApiResponse.success(
                "Order created successfully",
                HttpStatus.CREATED.value(),
                orderResponse
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all orders
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        ApiResponse<List<OrderResponse>> response = ApiResponse.success(
                "Orders retrieved successfully",
                orders
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Get order by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long id) {
        OrderResponse order = orderService.getOrderById(id);
        ApiResponse<OrderResponse> response = ApiResponse.success(
                "Order retrieved successfully",
                order
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Get orders by customer ID
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByCustomerId(
            @PathVariable Long customerId) {
        List<OrderResponse> orders = orderService.getOrdersByCustomerId(customerId);
        ApiResponse<List<OrderResponse>> response = ApiResponse.success(
                "Orders retrieved successfully for customer",
                orders
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Update order status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse updatedOrder = orderService.updateOrderStatus(id, request.getOrderStatus());
        ApiResponse<OrderResponse> response = ApiResponse.success(
                "Order status updated successfully",
                updatedOrder
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Delete/Cancel order
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(
            @PathVariable Long id) {
        orderService.deleteOrder(id);
        ApiResponse<Void> response = ApiResponse.error(
                "Order deleted successfully",
                HttpStatus.NO_CONTENT.value()
        );
        return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
    }
}

