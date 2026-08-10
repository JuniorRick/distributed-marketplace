package com.marketplace.payments.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marketplace.payments.payment.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentTest {

    @Test
    void capturesPendingPayment() {
        Payment payment = payment();

        payment.capture("gateway-123");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(payment.getGatewayReference()).isEqualTo("gateway-123");
        assertThat(payment.getProcessedAt()).isNotNull();
    }

    @Test
    void processedPaymentCannotBeProcessedAgain() {
        Payment payment = payment();
        payment.fail("declined");

        assertThatThrownBy(() -> payment.capture("gateway-123"))
                .isInstanceOf(IllegalStateException.class);
    }

    private static Payment payment() {
        return new Payment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("59.80"),
                "USD"
        );
    }
}
