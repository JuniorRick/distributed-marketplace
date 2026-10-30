package com.marketplace.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "management.otlp.metrics.export.enabled=false",
    "management.tracing.export.otlp.enabled=false"
})
class GatewayApplicationTest {
    private static final Queue<ForwardedRequest> FORWARDED = new ConcurrentLinkedQueue<>();
    private static final List<HttpServer> UPSTREAMS = new ArrayList<>();
    private static final String CUSTOMER_ID = UUID.randomUUID().toString();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    int port;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @DynamicPropertySource
    static void upstreamProperties(DynamicPropertyRegistry registry) {
        for (String service : List.of("catalog", "cart", "orders", "customers", "catalog-ui", "cart-ui", "orders-ui")) {
            HttpServer server = upstream(service);
            String property = service.toUpperCase().replace('-', '_') + "_BASE_URL";
            registry.add(property, () -> "http://127.0.0.1:" + server.getAddress().getPort());
        }
    }

    @BeforeEach
    void prepareAuthentication() {
        FORWARDED.clear();
        when(jwtDecoder.decode("valid-token")).thenReturn(Jwt.withTokenValue("valid-token")
            .header("alg", "RS256").subject(CUSTOMER_ID).build());
        when(jwtDecoder.decode("invalid-token")).thenThrow(new BadJwtException("Invalid test token"));
    }

    @AfterAll
    static void stopUpstreams() {
        UPSTREAMS.forEach(server -> server.stop(0));
    }

    @ParameterizedTest
    @CsvSource({
        "GET, /api/carts/active",
        "POST, /api/carts",
        "POST, /api/carts/cart-a/items",
        "PATCH, /api/carts/cart-a/items/item-a",
        "DELETE, /api/carts/cart-a/items/item-a",
        "GET, /api/orders/order-a",
        "POST, /api/orders",
        "GET, /api/customers/me",
        "POST, /api/customers/me"
    })
    void requiresAuthenticationForCustomerOperations(String method, String path) throws Exception {
        assertThat(request(method, path, null, "").statusCode()).isEqualTo(401);
        assertThat(FORWARDED).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "GET, /api/carts/active, cart",
        "GET, /api/carts/cart-a, cart",
        "POST, /api/carts, cart",
        "POST, /api/carts/cart-a/items, cart",
        "PATCH, /api/carts/cart-a/items/item-a, cart",
        "DELETE, /api/carts/cart-a/items/item-a, cart",
        "GET, /api/orders/order-a, orders",
        "POST, /api/orders, orders",
        "GET, /api/customers/me, customers",
        "POST, /api/customers/me, customers"
    })
    void forwardsAuthenticatedOperationsAndBearerTokens(String method, String path, String service) throws Exception {
        String body = method.equals("GET") || method.equals("DELETE") ? "" : "{\"quantity\":2}";
        assertThat(request(method, path, "valid-token", body).statusCode()).isEqualTo(200);
        assertThat(FORWARDED).containsExactly(new ForwardedRequest(service, method, path, "Bearer valid-token", body));
    }

    @ParameterizedTest
    @CsvSource({
        "/api/products, catalog, /api/products",
        "/api/products/product-a, catalog, /api/products/product-a",
        "/catalog/products, catalog-ui, /products",
        "/catalog/assets/app.js, catalog-ui, /assets/app.js",
        "/cart/, cart-ui, /",
        "/orders/, orders-ui, /"
    })
    void servesPublicPagesAndCatalogWithoutAuthentication(String path, String service, String upstreamPath) throws Exception {
        assertThat(request("GET", path, null, "").statusCode()).isEqualTo(200);
        assertThat(FORWARDED).containsExactly(new ForwardedRequest(service, "GET", upstreamPath, null, ""));
    }

    @ParameterizedTest
    @CsvSource({
        "POST, /api/products",
        "DELETE, /api/products/product-a",
        "POST, /api/carts/cart-a/checkout",
        "GET, /api/customers/customer-a",
        "POST, /api/customers",
        "PUT, /api/customers/me",
        "GET, /api/inventory/items",
        "GET, /api/payments/by-order/order-a",
        "GET, /api/notifications/by-customer/customer-a",
        "GET, /actuator/env"
    })
    void deniesOperationsOutsideTheBrowserContract(String method, String path) throws Exception {
        assertThat(request(method, path, "valid-token", "").statusCode()).isEqualTo(403);
        assertThat(FORWARDED).isEmpty();
    }

    @Test
    void rejectsInvalidBearerTokensBeforeProxying() throws Exception {
        assertThat(request("GET", "/api/carts/active", "invalid-token", "").statusCode()).isEqualTo(401);
        assertThat(FORWARDED).isEmpty();
    }

    private HttpResponse<String> request(String method, String path, String token, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .method(method, HttpRequest.BodyPublishers.ofString(body));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpServer upstream(String service) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                try (exchange) {
                    FORWARDED.add(new ForwardedRequest(service, exchange.getRequestMethod(),
                        exchange.getRequestURI().getPath(), exchange.getRequestHeaders().getFirst("Authorization"),
                        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
                    byte[] response = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                }
            });
            server.start();
            UPSTREAMS.add(server);
            return server;
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not start gateway test upstream", exception);
        }
    }

    private record ForwardedRequest(String service, String method, String path, String authorization, String body) {}
}
