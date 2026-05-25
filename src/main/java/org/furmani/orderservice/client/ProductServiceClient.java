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

    @Value("${product.service.base-url:http://localhost:8080/product-service}")
    private String productServiceBaseUrl;

    public Optional<ProductDetails> getProductDetails(Long productId) {
        if (productId == null) {
            return Optional.empty();
        }

        try {
            String normalizedBaseUrl = productServiceBaseUrl.endsWith("/")
                    ? productServiceBaseUrl.substring(0, productServiceBaseUrl.length() - 1)
                    : productServiceBaseUrl;

            ResponseEntity<String> response = restTemplate.getForEntity(
                    normalizedBaseUrl + "/product/" + productId,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isBlank()) {
                return Optional.empty();
            }

            String body = response.getBody();
            String productName = firstMatch(body, "productName", "name");
            String productDescription = firstMatch(body, "productDescription", "description");

            if (productName == null && productDescription == null) {
                return Optional.empty();
            }

            return Optional.of(ProductDetails.builder()
                    .productName(productName)
                    .productDescription(productDescription)
                    .build());
        } catch (RestClientException ex) {
            log.warn("Unable to fetch product details for productId {}", productId, ex);
            return Optional.empty();
        }
    }

    private String firstMatch(String body, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String token = "\"" + fieldName + "\"";
            int tokenIndex = body.indexOf(token);
            if (tokenIndex < 0) {
                continue;
            }

            int colonIndex = body.indexOf(':', tokenIndex + token.length());
            if (colonIndex < 0) {
                continue;
            }

            int firstQuoteIndex = body.indexOf('"', colonIndex + 1);
            if (firstQuoteIndex < 0) {
                continue;
            }

            int secondQuoteIndex = body.indexOf('"', firstQuoteIndex + 1);
            if (secondQuoteIndex < 0) {
                continue;
            }

            return body.substring(firstQuoteIndex + 1, secondQuoteIndex);
        }
        return null;
    }
}

