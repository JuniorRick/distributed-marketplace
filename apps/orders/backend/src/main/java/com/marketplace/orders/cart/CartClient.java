package com.marketplace.orders.cart;

import com.marketplace.orders.shared.ConflictException;
import com.marketplace.orders.shared.NotFoundException;
import com.marketplace.orders.shared.ServiceUnavailableException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CartClient {

    private final RestClient restClient;

    public CartClient(@Value("${cart.base-url}") String cartBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(cartBaseUrl)
                .build();
    }

    public CartSnapshot getCart(UUID cartId) {
        return requestCart(restClient.get().uri("/api/carts/{id}", cartId), cartId);
    }

    public CartSnapshot checkout(UUID cartId) {
        return requestCart(restClient.post().uri("/api/carts/{id}/checkout", cartId), cartId);
    }

    private CartSnapshot requestCart(RestClient.RequestHeadersSpec<?> request, UUID cartId) {
        try {
            CartSnapshot cart = request.retrieve().body(CartSnapshot.class);
            if (cart == null) {
                throw new ServiceUnavailableException("Cart returned an empty response", null);
            }
            return cart;
        } catch (HttpClientErrorException.NotFound exception) {
            throw new NotFoundException("Cart %s was not found".formatted(cartId));
        } catch (HttpClientErrorException.Conflict exception) {
            throw new ConflictException("Cart %s cannot be checked out".formatted(cartId));
        } catch (RestClientException exception) {
            throw new ServiceUnavailableException("Cart is currently unavailable", exception);
        }
    }

    public record CartSnapshot(
            UUID id,
            UUID customerId,
            String status,
            List<CartItemSnapshot> items,
            MoneySnapshot subtotal
    ) {
    }

    public record CartItemSnapshot(
            UUID id,
            UUID productId,
            String productSku,
            String productName,
            MoneySnapshot unitPrice,
            Integer quantity,
            MoneySnapshot lineTotal
    ) {
    }

    public record MoneySnapshot(BigDecimal amount, String currency) {
    }
}
