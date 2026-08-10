package com.marketplace.payments.payment.messaging;

import com.marketplace.payments.payment.PaymentService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class CapturePaymentCommandListener {

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    public CapturePaymentCommandListener(ObjectMapper objectMapper, PaymentService paymentService) {
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
    }

    @RabbitListener(queues = PaymentMessagingConfiguration.REQUEST_QUEUE)
    public void handle(String payload) throws JacksonException {
        paymentService.handle(objectMapper.readValue(payload, CapturePaymentCommand.class));
    }
}
