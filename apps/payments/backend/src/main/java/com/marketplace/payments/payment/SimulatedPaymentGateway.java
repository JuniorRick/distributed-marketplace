package com.marketplace.payments.payment;

import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    private final Outcome outcome;
    private final RefundOutcome refundOutcome;

    public SimulatedPaymentGateway(
            @Value("${marketplace.payment.simulator-outcome:CAPTURED}") String outcome,
            @Value("${marketplace.payment.simulator-refund-outcome:REFUNDED}") String refundOutcome
    ) {
        this.outcome = Outcome.valueOf(outcome.toUpperCase(Locale.ROOT));
        this.refundOutcome = RefundOutcome.valueOf(refundOutcome.toUpperCase(Locale.ROOT));
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        if (refundOutcome == RefundOutcome.FAILED) {
            return RefundResult.failed("Refund was rejected by the simulated gateway");
        }
        return RefundResult.refunded("sim-refund-" + request.paymentId());
    }

    @Override
    public CaptureResult capture(CaptureRequest request) {
        if (outcome == Outcome.FAILED) {
            return CaptureResult.failed("Payment was declined by the simulated gateway");
        }
        return CaptureResult.captured("sim-" + UUID.randomUUID());
    }

    private enum Outcome {
        CAPTURED,
        FAILED
    }

    private enum RefundOutcome {
        REFUNDED,
        FAILED
    }
}
