package com.marketplace.payments.payment;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGateway {

    CaptureResult capture(CaptureRequest request);

    record CaptureRequest(
            UUID idempotencyKey,
            UUID paymentId,
            UUID orderId,
            BigDecimal amount,
            String currency
    ) {
    }

    record CaptureResult(boolean captured, String gatewayReference, String failureReason) {

        public static CaptureResult captured(String gatewayReference) {
            return new CaptureResult(true, gatewayReference, null);
        }

        public static CaptureResult failed(String reason) {
            return new CaptureResult(false, null, reason);
        }
    }
}
