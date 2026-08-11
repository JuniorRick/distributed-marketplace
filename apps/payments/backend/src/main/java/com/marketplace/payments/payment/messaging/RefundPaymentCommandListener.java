package com.marketplace.payments.payment.messaging;

import com.marketplace.payments.payment.PaymentService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class RefundPaymentCommandListener {

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    public RefundPaymentCommandListener(ObjectMapper objectMapper, PaymentService paymentService) {
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
    }

    @RabbitListener(queues = PaymentMessagingConfiguration.REFUND_REQUEST_QUEUE)
    public void handle(String payload) throws JacksonException {
        paymentService.handle(objectMapper.readValue(payload, RefundPaymentCommand.class));
    }
}
