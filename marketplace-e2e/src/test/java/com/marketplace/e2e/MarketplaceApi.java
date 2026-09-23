package com.marketplace.e2e;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class MarketplaceApi {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final ComposeEnvironment environment;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    MarketplaceApi(ComposeEnvironment environment) {
        this.environment = environment;
    }

    UUID createProduct(String sku) {
        JsonNode response = post(
                environment.apiUri("catalog", "/api/products"),
                Map.of(
                        "sku", sku,
                        "name", "E2E Product",
                        "description", "Product created by the successful checkout E2E test",
                        "priceAmount", "19.99",
                        "currency", "EUR"
                ),
                201
        );
        return UUID.fromString(response.get("id").asText());
    }

    void replenish(UUID productId, int quantity) {
        post(
                environment.apiUri("inventory", "/api/inventory/" + productId + "/replenishments"),
                Map.of("quantity", quantity),
                200
        );
    }

    UUID createCart(UUID customerId) {
        JsonNode response = post(
                environment.apiUri("cart", "/api/carts"),
                Map.of("customerId", customerId.toString()),
                201
        );
        return UUID.fromString(response.get("id").asText());
    }

    void addCartItem(UUID cartId, UUID productId, int quantity) {
        post(
                environment.apiUri("cart", "/api/carts/" + cartId + "/items"),
                Map.of("productId", productId.toString(), "quantity", quantity),
                200
        );
    }

    JsonNode createOrder(UUID cartId) {
        return createOrder(cartId, 202);
    }

    JsonNode createOrder(UUID cartId, int... expectedStatuses) {
        return post(
                environment.apiUri("orders", "/api/orders"),
                Map.of("cartId", cartId.toString()),
                expectedStatuses
        );
    }

    JsonNode getOrder(UUID orderId) {
        return send(
                HttpRequest.newBuilder(environment.apiUri("orders", "/api/orders/" + orderId))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build(),
                200
        );
    }

    private JsonNode post(URI uri, Map<String, ?> body, int... expectedStatuses) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
                .build();
        return send(request, expectedStatuses);
    }

    private JsonNode send(HttpRequest request, int... expectedStatuses) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            boolean expected = java.util.Arrays.stream(expectedStatuses)
                    .anyMatch(status -> status == response.statusCode());
            if (!expected) {
                throw new AssertionError(
                        request.method() + " " + request.uri() + " returned " + response.statusCode()
                                + ", expected one of " + java.util.Arrays.toString(expectedStatuses)
                                + ": " + response.body()
                );
            }
            return JSON.readTree(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("Request failed: " + request.method() + " " + request.uri(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Request interrupted: " + request.method() + " " + request.uri(), exception);
        }
    }
}
