package com.marketplace.payments.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketplace.payments.messaging.OutboxService;
import com.marketplace.payments.payment.PaymentGateway.CaptureResult;
import com.marketplace.payments.payment.PaymentGateway.RefundResult;
import com.marketplace.payments.payment.messaging.CapturePaymentCommand;
import com.marketplace.payments.payment.messaging.PaymentMessagingConfiguration;
import com.marketplace.payments.payment.messaging.RefundPaymentCommand;
import com.marketplace.payments.payment.repository.Payment;
import com.marketplace.payments.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private PaymentService service;

    @Test
    void capturesPaymentAndPublishesCapturedEvent() {
        CapturePaymentCommand command = command();
        prepareNewCommand(command);
        when(paymentGateway.capture(any())).thenReturn(CaptureResult.captured("gateway-123"));

        service.handle(command);

        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).saveAndFlush(payment.capture());
        assertThat(payment.getValue().getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        ArgumentCaptor<PaymentGateway.CaptureRequest> gatewayRequest = ArgumentCaptor.forClass(
                PaymentGateway.CaptureRequest.class
        );
        verify(paymentGateway).capture(gatewayRequest.capture());
        assertThat(gatewayRequest.getValue().idempotencyKey()).isEqualTo(command.eventId());
        verify(outboxService).enqueue(
                any(UUID.class),
                eq("PaymentCapturedEvent.v1"),
                eq(PaymentMessagingConfiguration.PAYMENT_CAPTURED_EVENT_ROUTING_KEY),
                any()
        );
    }

    @Test
    void recordsGatewayDeclineAndPublishesFailedEvent() {
        CapturePaymentCommand command = command();
        prepareNewCommand(command);
        when(paymentGateway.capture(any())).thenReturn(CaptureResult.failed("declined"));

        service.handle(command);

        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).saveAndFlush(payment.capture());
        assertThat(payment.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getValue().getFailureReason()).isEqualTo("declined");
        verify(outboxService).enqueue(
                any(UUID.class),
                eq("PaymentFailedEvent.v1"),
                eq(PaymentMessagingConfiguration.PAYMENT_FAILED_EVENT_ROUTING_KEY),
                any()
        );
    }

    @Test
    void ignoresAlreadyClaimedCommand() {
        service.handle(command());

        verifyNoInteractions(paymentRepository, paymentGateway, outboxService);
    }

    @Test
    void refundsCapturedPaymentAndPublishesRefundedEvent() {
        Payment payment = new Payment(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("59.80"), "USD"
        );
        payment.capture("capture-123");
        RefundPaymentCommand command = new RefundPaymentCommand(
                UUID.randomUUID(), payment.getOrderId(), "Inventory commit failed", Instant.now()
        );
        when(jdbcTemplate.update(
                anyString(), eq(command.eventId()), eq("RefundPaymentCommand.v1")
        )).thenReturn(1);
        when(paymentRepository.findByOrderId(payment.getOrderId())).thenReturn(java.util.Optional.of(payment));
        when(paymentGateway.refund(any())).thenReturn(RefundResult.refunded("refund-123"));
        when(paymentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(command);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundReference()).isEqualTo("refund-123");
        verify(outboxService).enqueue(
                any(UUID.class),
                eq("PaymentRefundedEvent.v1"),
                eq(PaymentMessagingConfiguration.PAYMENT_REFUNDED_EVENT_ROUTING_KEY),
                any()
        );
    }

    private void prepareNewCommand(CapturePaymentCommand command) {
        when(jdbcTemplate.update(anyString(), eq(command.eventId()), eq("CapturePaymentCommand.v1")))
                .thenReturn(1);
        when(paymentRepository.findByOrderId(command.orderId())).thenReturn(java.util.Optional.empty());
        when(paymentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static CapturePaymentCommand command() {
        return new CapturePaymentCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("59.80"),
                "USD",
                Instant.now()
        );
    }
}
