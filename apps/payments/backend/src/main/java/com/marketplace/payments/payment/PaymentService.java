package com.marketplace.payments.payment;

import com.marketplace.payments.messaging.OutboxService;
import com.marketplace.payments.payment.PaymentGateway.CaptureRequest;
import com.marketplace.payments.payment.PaymentGateway.CaptureResult;
import com.marketplace.payments.payment.messaging.CapturePaymentCommand;
import com.marketplace.payments.payment.messaging.PaymentMessagingConfiguration;
import com.marketplace.payments.payment.messaging.PaymentResult;
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

    private static final String COMMAND_EVENT_TYPE = "CapturePaymentCommand.v1";
    private static final String CAPTURED_EVENT_TYPE = "PaymentCapturedEvent.v1";
    private static final String FAILED_EVENT_TYPE = "PaymentFailedEvent.v1";

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
        if (!claim(command.eventId()) || paymentRepository.existsByOrderId(command.orderId())) {
            return;
        }

        Payment payment = new Payment(
                command.orderId(),
                command.customerId(),
                command.amount(),
                command.currency()
        );
        CaptureResult gatewayResult = paymentGateway.capture(new CaptureRequest(
                command.eventId(),
                payment.getPublicId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getCurrency()
        ));
        if (gatewayResult.captured()) {
            payment.capture(gatewayResult.gatewayReference());
        } else {
            payment.fail(gatewayResult.failureReason());
        }

        paymentRepository.saveAndFlush(payment);
        publishResult(payment);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByPublicId(UUID paymentId) {
        return paymentRepository.findByPublicId(paymentId);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    private boolean claim(UUID eventId) {
        return jdbcTemplate.update("""
                INSERT INTO inbox_messages(event_id, event_type, processed_at)
                VALUES (?, ?, now())
                ON CONFLICT (event_id) DO NOTHING
                """, eventId, COMMAND_EVENT_TYPE) == 1;
    }

    private void publishResult(Payment payment) {
        boolean captured = payment.getStatus() == PaymentStatus.CAPTURED;
        PaymentResult result = new PaymentResult(
                UUID.randomUUID(),
                payment.getPublicId(),
                payment.getOrderId(),
                captured ? "CAPTURED" : "FAILED",
                payment.getAmount(),
                payment.getCurrency(),
                payment.getFailureReason(),
                Instant.now()
        );
        outboxService.enqueue(
                result.eventId(),
                captured ? CAPTURED_EVENT_TYPE : FAILED_EVENT_TYPE,
                captured
                        ? PaymentMessagingConfiguration.PAYMENT_CAPTURED_EVENT_ROUTING_KEY
                        : PaymentMessagingConfiguration.PAYMENT_FAILED_EVENT_ROUTING_KEY,
                result
        );
    }
}
