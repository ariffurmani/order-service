package org.furmani.orderservice.client;

import lombok.extern.slf4j.Slf4j;
import org.furmani.orderservice.dto.ProductDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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

            String url = normalizedBaseUrl + "/products/" + productId;
            log.debug("[ProductServiceClient] getProductDetails() - Fetching from URL: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            log.debug("[ProductServiceClient] getProductDetails() - Response received. Status Code: {}",
                    response.getStatusCode());

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isBlank()) {
                log.warn("[ProductServiceClient] getProductDetails() - WARN - Invalid response for productId: {}. Status: {}",
                        productId, response.getStatusCode());
                return Optional.empty();
            }

            String body = response.getBody();
            log.debug("[ProductServiceClient] getProductDetails() - Response body received for productId: {}", productId);
            log.debug("[ProductServiceClient] getProductDetails() - Response body content: {}", body);
            String productName = firstMatch(body, "productName", "name");
            String productDescription = firstMatch(body, "productDescription", "description");

            if (productName == null && productDescription == null) {
                log.warn("[ProductServiceClient] getProductDetails() - WARN - No product details found in response for productId: {}",
                        productId);
                return Optional.empty();
            }

            ProductDetails details = ProductDetails.builder()
                    .productName(productName)
                    .productDescription(productDescription)
                    .build();

            log.info("[ProductServiceClient] getProductDetails() - SUCCESS - Product details fetched for productId: {}. productName: {}",
                    productId, productName);
            return Optional.of(details);
        } catch (RestClientException ex) {
            log.error("[ProductServiceClient] getProductDetails() - ERROR - Unable to fetch product details for productId: {}. Exception: {}",
                    productId, ex.getMessage(), ex);
            return Optional.empty();
        } catch (Exception ex) {
            log.error("[ProductServiceClient] getProductDetails() - ERROR - Unexpected error while fetching product details for productId: {}",
                    productId, ex);
            return Optional.empty();
        }
    }

    private String firstMatch(String body, String... fieldNames) {
        log.debug("[ProductServiceClient] firstMatch() - START - Searching for fields: {}", String.join(", ", fieldNames));

        for (String fieldName : fieldNames) {
            String token = "\"" + fieldName + "\"";
            int tokenIndex = body.indexOf(token);
            if (tokenIndex < 0) {
                log.debug("[ProductServiceClient] firstMatch() - Field '{}' not found", fieldName);
                continue;
            }

            int colonIndex = body.indexOf(':', tokenIndex + token.length());
            if (colonIndex < 0) {
                log.debug("[ProductServiceClient] firstMatch() - Colon not found after field '{}'", fieldName);
                continue;
            }

            int firstQuoteIndex = body.indexOf('"', colonIndex + 1);
            if (firstQuoteIndex < 0) {
                log.debug("[ProductServiceClient] firstMatch() - Opening quote not found for field '{}'", fieldName);
                continue;
            }

            int secondQuoteIndex = body.indexOf('"', firstQuoteIndex + 1);
            if (secondQuoteIndex < 0) {
                log.debug("[ProductServiceClient] firstMatch() - Closing quote not found for field '{}'", fieldName);
                continue;
            }

            String result = body.substring(firstQuoteIndex + 1, secondQuoteIndex);
            log.debug("[ProductServiceClient] firstMatch() - SUCCESS - Found value for field '{}': {}", fieldName, result);
            return result;
        }

        log.debug("[ProductServiceClient] firstMatch() - No matching field found");
        return null;
    }
}

