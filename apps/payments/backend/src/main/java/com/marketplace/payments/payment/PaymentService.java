package com.marketplace.payments.payment;

import com.marketplace.payments.messaging.OutboxService;
import com.marketplace.payments.payment.PaymentGateway.CaptureRequest;
import com.marketplace.payments.payment.PaymentGateway.CaptureResult;
import com.marketplace.payments.payment.PaymentGateway.RefundRequest;
import com.marketplace.payments.payment.PaymentGateway.RefundResult;
import com.marketplace.payments.payment.messaging.CapturePaymentCommand;
import com.marketplace.payments.payment.messaging.PaymentMessagingConfiguration;
import com.marketplace.payments.payment.messaging.PaymentResult;
import com.marketplace.payments.payment.messaging.RefundPaymentCommand;
import com.marketplace.payments.payment.repository.Payment;
import com.marketplace.payments.payment.repository.PaymentRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final String CAPTURE_COMMAND_EVENT_TYPE = "CapturePaymentCommand.v1";
    private static final String REFUND_COMMAND_EVENT_TYPE = "RefundPaymentCommand.v1";
    private static final String CAPTURED_EVENT_TYPE = "PaymentCapturedEvent.v1";
    private static final String FAILED_EVENT_TYPE = "PaymentFailedEvent.v1";
    private static final String REFUNDED_EVENT_TYPE = "PaymentRefundedEvent.v1";
    private static final String REFUND_FAILED_EVENT_TYPE = "PaymentRefundFailedEvent.v1";

    private final JdbcTemplate jdbcTemplate;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final OutboxService outboxService;

    public PaymentService(
            JdbcTemplate jdbcTemplate,
            PaymentRepository paymentRepository,
            PaymentGateway paymentGateway,
            OutboxService outboxService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.outboxService = outboxService;
    }

    @Transactional
    public void handle(CapturePaymentCommand command) {
        if (!claim(command.eventId(), CAPTURE_COMMAND_EVENT_TYPE)) {
            return;
        }

        Optional<Payment> existing = paymentRepository.findByOrderId(command.orderId());
        if (existing.isPresent()) {
            publishCaptureResult(existing.get());
            return;
        }

        Payment payment = new Payment(
                command.orderId(), command.customerId(), command.amount(), command.currency()
        );
        CaptureResult gatewayResult = paymentGateway.capture(new CaptureRequest(
                command.eventId(), payment.getPublicId(), payment.getOrderId(),
                payment.getAmount(), payment.getCurrency()
        ));
        if (gatewayResult.captured()) {
            payment.capture(gatewayResult.gatewayReference());
        } else {
            payment.fail(gatewayResult.failureReason());
        }

        paymentRepository.saveAndFlush(payment);
        publishCaptureResult(payment);
    }

    @Transactional
    public void handle(RefundPaymentCommand command) {
        if (!claim(command.eventId(), REFUND_COMMAND_EVENT_TYPE)) {
            return;
        }

        Payment payment = paymentRepository.findByOrderId(command.orderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Payment not found. Order ID=" + command.orderId()
                ));
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            publishRefundResult(payment, true, null);
            return;
        }
        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            publishRefundResult(
                    payment, false, "Only a captured payment can be refunded"
            );
            return;
        }

        RefundResult result = paymentGateway.refund(new RefundRequest(
                payment.getPublicId(), payment.getPublicId(), payment.getOrderId(),
                payment.getAmount(), payment.getCurrency(), payment.getGatewayReference()
        ));
        if (result.refunded()) {
            payment.refund(result.refundReference());
        } else {
            payment.recordRefundFailure(result.failureReason());
        }
        paymentRepository.saveAndFlush(payment);
        publishRefundResult(payment, result.refunded(), result.failureReason());
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByPublicId(UUID paymentId) {
        return paymentRepository.findByPublicId(paymentId);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    private boolean claim(UUID eventId, String eventType) {
        return jdbcTemplate.update("""
                INSERT INTO inbox_messages(event_id, event_type, processed_at)
                VALUES (?, ?, now())
                ON CONFLICT (event_id) DO NOTHING
                """, eventId, eventType) == 1;
    }

    private void publishCaptureResult(Payment payment) {
        boolean captured = payment.getStatus() == PaymentStatus.CAPTURED
                || payment.getStatus() == PaymentStatus.REFUNDED;
        publishResult(
                payment,
                captured ? "CAPTURED" : "FAILED",
                captured ? null : payment.getFailureReason(),
                captured ? CAPTURED_EVENT_TYPE : FAILED_EVENT_TYPE,
                captured
                        ? PaymentMessagingConfiguration.PAYMENT_CAPTURED_EVENT_ROUTING_KEY
                        : PaymentMessagingConfiguration.PAYMENT_FAILED_EVENT_ROUTING_KEY
        );
    }

    private void publishRefundResult(Payment payment, boolean refunded, String reason) {
        publishResult(
                payment,
                refunded ? "REFUNDED" : "REFUND_FAILED",
                refunded ? null : reason,
                refunded ? REFUNDED_EVENT_TYPE : REFUND_FAILED_EVENT_TYPE,
                refunded
                        ? PaymentMessagingConfiguration.PAYMENT_REFUNDED_EVENT_ROUTING_KEY
                        : PaymentMessagingConfiguration.PAYMENT_REFUND_FAILED_EVENT_ROUTING_KEY
        );
    }

    private void publishResult(
            Payment payment,
            String outcome,
            String reason,
            String eventType,
            String routingKey
    ) {
        PaymentResult result = new PaymentResult(
                UUID.randomUUID(), payment.getPublicId(), payment.getOrderId(), outcome,
                payment.getAmount(), payment.getCurrency(), reason, Instant.now()
        );
        outboxService.enqueue(result.eventId(), eventType, routingKey, result);
    }
}
