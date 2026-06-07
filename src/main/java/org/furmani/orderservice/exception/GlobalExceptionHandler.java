package org.furmani.orderservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.furmani.orderservice.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductNotFoundException(
            ProductNotFoundException ex) {
        log.error("[GlobalExceptionHandler] handleProductNotFoundException() - PRODUCT NOT FOUND: {}", ex.getMessage(), ex);
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), HttpStatus.NOT_FOUND.value());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ProductUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductUnavailableException(
            ProductUnavailableException ex) {
        log.warn("[GlobalExceptionHandler] handleProductUnavailableException() - PRODUCT UNAVAILABLE: {}", ex.getMessage(), ex);
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), HttpStatus.CONFLICT.value());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderNotFoundException(
            OrderNotFoundException ex) {
        log.error("[GlobalExceptionHandler] handleOrderNotFoundException() - ORDER NOT FOUND: {}", ex.getMessage(), ex);
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), HttpStatus.NOT_FOUND.value());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {
        log.warn("[GlobalExceptionHandler] handleValidationException() - VALIDATION FAILED: Request validation error");

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
            log.debug("[GlobalExceptionHandler] handleValidationException() - Field: {}, Error: {}",
                    error.getField(), error.getDefaultMessage());
        });

        log.debug("[GlobalExceptionHandler] handleValidationException() - Total validation errors: {}", errors.size());

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .message("Validation failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .data(errors)
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        log.warn("[GlobalExceptionHandler] handleIllegalArgumentException() - BAD REQUEST: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(
            IllegalStateException ex) {
        log.error("[GlobalExceptionHandler] handleIllegalStateException() - STATE ERROR: {}", ex.getMessage(), ex);
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), HttpStatus.BAD_GATEWAY.value());
        return new ResponseEntity<>(response, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(
            Exception ex) {
        log.error("[GlobalExceptionHandler] handleGeneralException() - UNEXPECTED ERROR: {}", ex.getMessage(), ex);
        ApiResponse<Void> response = ApiResponse.error(
                "An unexpected error occurred: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

