package com.marketplace.cart.catalog;

import com.marketplace.cart.shared.NotFoundException;
import com.marketplace.cart.shared.ServiceUnavailableException;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(@Value("${catalog.base-url}") String catalogBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(catalogBaseUrl)
                .build();
    }

    public ProductSnapshot getProduct(UUID productId) {
        try {
            ProductSnapshot product = restClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .body(ProductSnapshot.class);

            if (product == null) {
                throw new ServiceUnavailableException("Catalog returned an empty product response");
            }
            return product;
        } catch (HttpClientErrorException.NotFound exception) {
            throw new NotFoundException("Product %s was not found in Catalog".formatted(productId));
        } catch (RestClientException exception) {
            throw new ServiceUnavailableException("Catalog is currently unavailable", exception);
        }
    }

    public record ProductSnapshot(
            UUID id,
            String sku,
            String name,
            String description,
            MoneySnapshot price,
            String status
    ) {
    }

    public record MoneySnapshot(BigDecimal amount, String currency) {
    }
}
