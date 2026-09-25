package com.marketplace.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import tools.jackson.databind.JsonNode;

@Execution(ExecutionMode.SAME_THREAD)
class CheckoutE2E {

    private static ComposeEnvironment environment;
    private static MarketplaceApi marketplace;
    private static boolean failed;

    @BeforeAll
    static void startMarketplace() {
        environment = ComposeEnvironment.create();
        try {
            environment.start();
            marketplace = new MarketplaceApi(environment);
        } catch (RuntimeException | AssertionError exception) {
            failed = true;
            environment.captureLogs();
            try {
                environment.close();
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    @AfterAll
    static void stopMarketplace() {
        if (failed) {
            environment.captureLogs();
        }
        environment.close();
    }

    @Test
    void successfulCheckoutEndsAsConfirmed() {
        try {
            environment.restartPayments("CAPTURED", "REFUNDED");

            UUID productId = marketplace.createProduct("E2E-" + UUID.randomUUID());
            marketplace.replenish(productId, 5);

            UUID customerId = marketplace.createCustomer();
            UUID cartId = marketplace.createCart(customerId);
            marketplace.addCartItem(cartId, productId, 2);

            JsonNode createdOrder = marketplace.createOrder(cartId);
            UUID orderId = UUID.fromString(createdOrder.get("id").asString());

            Awaitility.await("order to complete the checkout saga")
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    JsonNode order = marketplace.getOrder(orderId);
                    assertThat(order.get("status").asString()).isEqualTo("CONFIRMED");
                    assertThat(order.get("sourceCartId").asString()).isEqualTo(cartId.toString());
                    assertThat(order.get("customerId").asString()).isEqualTo(customerId.toString());
                    assertThat(order.get("items")).hasSize(1);
                    assertThat(order.get("items").get(0).get("quantity").asInt()).isEqualTo(2);
                });
        } catch (RuntimeException | AssertionError exception) {
            failed = true;
            throw exception;
        }
    }

    @Test
    void insufficientInventoryCheckoutEndsAsRejected() {
        try {
            environment.restartPayments("CAPTURED", "REFUNDED");

            UUID productId = marketplace.createProduct("E2E-" + UUID.randomUUID());
            marketplace.replenish(productId, 1);

            UUID customerId = marketplace.createCustomer();
            UUID cartId = marketplace.createCart(customerId);
            marketplace.addCartItem(cartId, productId, 2);

            JsonNode createdOrder = marketplace.createOrder(cartId);
            UUID orderId = UUID.fromString(createdOrder.get("id").asString());

            Awaitility.await("order to complete as REJECTED")
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    JsonNode order = marketplace.getOrder(orderId);
                    assertThat(order.get("status").asString()).isEqualTo("REJECTED");
                    assertThat(order.get("sourceCartId").asString()).isEqualTo(cartId.toString());
                    assertThat(order.get("customerId").asString()).isEqualTo(customerId.toString());
                    assertThat(order.get("items")).hasSize(1);
                    assertThat(order.get("items").get(0).get("quantity").asInt()).isEqualTo(2);
                });
        } catch (RuntimeException | AssertionError exception) {
            failed = true;
            throw exception;
        }
    }

    @Test
    void paymentFailureReleasesInventory() {
        try {
            environment.restartPayments("FAILED", "REFUNDED");

            UUID productId = marketplace.createProduct("E2E-" + UUID.randomUUID());
            marketplace.replenish(productId, 1);

            UUID customerId = marketplace.createCustomer();
            UUID cartId = marketplace.createCart(customerId);
            marketplace.addCartItem(cartId, productId, 1);

            JsonNode createdOrder = marketplace.createOrder(cartId);
            UUID orderId = UUID.fromString(createdOrder.get("id").asString());

            Awaitility.await("order to complete as REJECTED")
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    JsonNode order = marketplace.getOrder(orderId);
                    assertThat(order.get("status").asString()).isEqualTo("REJECTED");
                    assertThat(order.get("sourceCartId").asString()).isEqualTo(cartId.toString());
                    assertThat(order.get("customerId").asString()).isEqualTo(customerId.toString());
                    assertThat(order.get("items")).hasSize(1);
                    assertThat(order.get("items").get(0).get("quantity").asInt()).isEqualTo(1);
                    assertThat(order.get("failureReason").asString()).containsIgnoringCase("Payment was declined");
                });
        } catch (RuntimeException | AssertionError exception) {
            failed = true;
            throw exception;
        }
    }

    @Test
    void duplicateCheckoutReturnsTheExistingOrder() {
        try {
            environment.restartPayments("CAPTURED", "REFUNDED");

            UUID productId = marketplace.createProduct("E2E-" + UUID.randomUUID());
            marketplace.replenish(productId, 2);

            UUID customerId = marketplace.createCustomer();
            UUID cartId = marketplace.createCart(customerId);
            marketplace.addCartItem(cartId, productId, 1);

            JsonNode firstResponse = marketplace.createOrder(cartId);
            JsonNode duplicateResponse = marketplace.createOrder(cartId, 200, 202);
            UUID orderId = UUID.fromString(firstResponse.get("id").asString());

            assertThat(duplicateResponse.get("id").asString()).isEqualTo(orderId.toString());

            Awaitility.await("the single order to complete the checkout saga")
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    JsonNode order = marketplace.getOrder(orderId);
                    assertThat(order.get("status").asString()).isEqualTo("CONFIRMED");
                    assertThat(order.get("sourceCartId").asString()).isEqualTo(cartId.toString());
                });
        } catch (RuntimeException | AssertionError exception) {
            failed = true;
            throw exception;
        }
    }

    @Test
    void checkoutRecoversAfterRabbitMqRestart() {
        try {
            environment.restartPayments("CAPTURED", "REFUNDED");

            UUID productId = marketplace.createProduct("E2E-" + UUID.randomUUID());
            marketplace.replenish(productId, 2);

            UUID customerId = marketplace.createCustomer();
            UUID cartId = marketplace.createCart(customerId);
            marketplace.addCartItem(cartId, productId, 1);

            JsonNode createdOrder;
            environment.stopRabbitMq();
            try {
                createdOrder = marketplace.createOrder(cartId);
            } finally {
                environment.startRabbitMq();
            }

            UUID orderId = UUID.fromString(createdOrder.get("id").asString());
            Awaitility.await("outbox delivery to recover after RabbitMQ restarts")
                .atMost(Duration.ofSeconds(90))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    JsonNode order = marketplace.getOrder(orderId);
                    assertThat(order.get("status").asString()).isEqualTo("CONFIRMED");
                    assertThat(order.get("sourceCartId").asString()).isEqualTo(cartId.toString());
                });
        } catch (RuntimeException | AssertionError exception) {
            failed = true;
            throw exception;
        }
    }

}
