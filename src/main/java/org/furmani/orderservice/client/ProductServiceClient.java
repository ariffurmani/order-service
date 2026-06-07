package org.furmani.orderservice.client;

import lombok.extern.slf4j.Slf4j;
import org.furmani.orderservice.dto.ProductDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@lombok.RequiredArgsConstructor
public class ProductServiceClient {

    private final RestTemplate restTemplate;

    @Value("${product.service.base-url:http://localhost:8080}")
    private String productServiceBaseUrl;

    public Optional<ProductDetails> getProductDetails(Long productId) {
        log.debug("[ProductServiceClient] getProductDetails() - START - Fetching product details for productId: {}", productId);

        if (productId == null) {
            log.warn("[ProductServiceClient] getProductDetails() - WARN - ProductId is null, returning empty Optional");
            return Optional.empty();
        }

        try {
            String normalizedBaseUrl = productServiceBaseUrl.endsWith("/")
                    ? productServiceBaseUrl.substring(0, productServiceBaseUrl.length() - 1)
                    : productServiceBaseUrl;

            String url = normalizedBaseUrl + "/product/" + productId;
            log.debug("[ProductServiceClient] getProductDetails() - Fetching from URL: {}", url);

            ResponseEntity<ProductDetails> response = restTemplate.getForEntity(url, ProductDetails.class);

            log.debug("[ProductServiceClient] getProductDetails() - Response received. Status Code: {}",
                    response.getStatusCode());

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("[ProductServiceClient] getProductDetails() - ERROR - Invalid successful response for productId: {}. Status: {}",
                        productId, response.getStatusCode());
                throw new IllegalStateException("Invalid product response for productId: " + productId);
            }

            ProductDetails details = response.getBody();

            log.info("[ProductServiceClient] getProductDetails() - SUCCESS - Product details fetched for productId: {}. productName: {}",
                    productId, details.getProductName());
            return Optional.of(details);
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("[ProductServiceClient] getProductDetails() - WARN - Product not found for productId: {}", productId);
            return Optional.empty();
        } catch (HttpClientErrorException ex) {
            log.error("[ProductServiceClient] getProductDetails() - ERROR - Product service returned HTTP {} for productId: {}",
                    ex.getStatusCode(), productId, ex);
            throw new IllegalStateException("Failed to fetch product details for productId: " + productId, ex);
        } catch (RestClientException ex) {
            log.error("[ProductServiceClient] getProductDetails() - ERROR - Unable to fetch product details for productId: {}. Exception: {}",
                    productId, ex.getMessage(), ex);
            throw new IllegalStateException("Failed to fetch product details for productId: " + productId, ex);
        }
    }

    public void decrementStock(Long productId, Integer quantity) {
        log.debug("[ProductServiceClient] decrementStock() - START - Decrementing stock for productId: {} by {}",
                productId, quantity);

        if (productId == null || quantity == null || quantity <= 0) {
            log.warn("[ProductServiceClient] decrementStock() - WARN - Invalid productId or quantity. productId: {}, quantity: {}",
                    productId, quantity);
            throw new IllegalArgumentException("Invalid productId or quantity");
        }

        try {
            String normalizedBaseUrl = productServiceBaseUrl.endsWith("/")
                    ? productServiceBaseUrl.substring(0, productServiceBaseUrl.length() - 1)
                    : productServiceBaseUrl;

            String url = normalizedBaseUrl + "/product/" + productId + "/stock/decrement";
            Map<String, Integer> body = new HashMap<>();
            body.put("quantity", quantity);

            log.debug("[ProductServiceClient] decrementStock() - Calling URL: {} with body: {}", url, body);
            ResponseEntity<Void> response = restTemplate.postForEntity(url, body, Void.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("[ProductServiceClient] decrementStock() - ERROR - Product service returned non-2xx status: {} for productId: {}",
                        response.getStatusCode(), productId);
                throw new IllegalStateException("Failed to decrement stock for productId: " + productId);
            }

            log.info("[ProductServiceClient] decrementStock() - SUCCESS - Decremented stock for productId: {} by {}",
                    productId, quantity);
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("[ProductServiceClient] decrementStock() - WARN - Product not found for productId: {}", productId);
            throw new IllegalStateException("Product not found: " + productId, ex);
        } catch (HttpClientErrorException ex) {
            log.error("[ProductServiceClient] decrementStock() - ERROR - Product service returned HTTP {} for productId: {}",
                    ex.getStatusCode(), productId, ex);
            throw new IllegalStateException("Failed to decrement stock for productId: " + productId, ex);
        } catch (RestClientException ex) {
            log.error("[ProductServiceClient] decrementStock() - ERROR - Unable to decrement stock for productId: {}. Exception: {}",
                    productId, ex.getMessage(), ex);
            throw new IllegalStateException("Failed to decrement stock for productId: " + productId, ex);
        }
    }

    public void incrementStock(Long productId, Integer quantity) {
        log.debug("[ProductServiceClient] incrementStock() - START - Incrementing stock for productId: {} by {}",
                productId, quantity);

        if (productId == null || quantity == null || quantity <= 0) {
            log.warn("[ProductServiceClient] incrementStock() - WARN - Invalid productId or quantity. productId: {}, quantity: {}",
                    productId, quantity);
            throw new IllegalArgumentException("Invalid productId or quantity");
        }

        try {
            String normalizedBaseUrl = productServiceBaseUrl.endsWith("/")
                    ? productServiceBaseUrl.substring(0, productServiceBaseUrl.length() - 1)
                    : productServiceBaseUrl;

            String url = normalizedBaseUrl + "/product/" + productId + "/stock/increment";
            Map<String, Integer> body = new HashMap<>();
            body.put("quantity", quantity);

            log.debug("[ProductServiceClient] incrementStock() - Calling URL: {} with body: {}", url, body);
            ResponseEntity<Void> response = restTemplate.postForEntity(url, body, Void.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("[ProductServiceClient] incrementStock() - ERROR - Product service returned non-2xx status: {} for productId: {}",
                        response.getStatusCode(), productId);
                throw new IllegalStateException("Failed to increment stock for productId: " + productId);
            }

            log.info("[ProductServiceClient] incrementStock() - SUCCESS - Incremented stock for productId: {} by {}",
                    productId, quantity);
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("[ProductServiceClient] incrementStock() - WARN - Product not found for productId: {}", productId);
            throw new IllegalStateException("Product not found: " + productId, ex);
        } catch (HttpClientErrorException ex) {
            log.error("[ProductServiceClient] incrementStock() - ERROR - Product service returned HTTP {} for productId: {}",
                    ex.getStatusCode(), productId, ex);
            throw new IllegalStateException("Failed to increment stock for productId: " + productId, ex);
        } catch (RestClientException ex) {
            log.error("[ProductServiceClient] incrementStock() - ERROR - Unable to increment stock for productId: {}. Exception: {}",
                    productId, ex.getMessage(), ex);
            throw new IllegalStateException("Failed to increment stock for productId: " + productId, ex);
        }
    }
}

