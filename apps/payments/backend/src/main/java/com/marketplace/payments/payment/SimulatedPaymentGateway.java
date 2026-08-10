package com.marketplace.payments.payment;

import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    private final Outcome outcome;

    public SimulatedPaymentGateway(
            @Value("${marketplace.payment.simulator-outcome:CAPTURED}") String outcome
    ) {
        this.outcome = Outcome.valueOf(outcome.toUpperCase(Locale.ROOT));
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
}
