package com.marketplace.payments.payment.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class PaymentMessagingConfiguration {

    public static final String EXCHANGE = "marketplace.payments.v1";
    public static final String DEAD_LETTER_EXCHANGE = "marketplace.payments.dlx.v1";
    public static final String REQUEST_QUEUE = "payments.capture-requests.v1";
    public static final String REQUEST_DEAD_LETTER_QUEUE = "payments.capture-requests.dlq.v1";
    public static final String REFUND_REQUEST_QUEUE = "payments.refund-requests.v1";
    public static final String REFUND_REQUEST_DEAD_LETTER_QUEUE = "payments.refund-requests.dlq.v1";
    public static final String CAPTURE_PAYMENT_COMMAND_ROUTING_KEY = "payment.command.capture.v1";
    public static final String REFUND_PAYMENT_COMMAND_ROUTING_KEY = "payment.command.refund.v1";
    public static final String PAYMENT_CAPTURED_EVENT_ROUTING_KEY = "payment.event.captured.v1";
    public static final String PAYMENT_FAILED_EVENT_ROUTING_KEY = "payment.event.failed.v1";
    public static final String PAYMENT_REFUNDED_EVENT_ROUTING_KEY = "payment.event.refunded.v1";
    public static final String PAYMENT_REFUND_FAILED_EVENT_ROUTING_KEY = "payment.event.refund-failed.v1";

    @Bean
    DirectExchange paymentExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange paymentDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue paymentCaptureRequestQueue() {
        return QueueBuilder.durable(REQUEST_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(CAPTURE_PAYMENT_COMMAND_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue paymentCaptureRequestDeadLetterQueue() {
        return QueueBuilder.durable(REQUEST_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Queue paymentRefundRequestQueue() {
        return QueueBuilder.durable(REFUND_REQUEST_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(REFUND_PAYMENT_COMMAND_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue paymentRefundRequestDeadLetterQueue() {
        return QueueBuilder.durable(REFUND_REQUEST_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding paymentCaptureRequestBinding() {
        return BindingBuilder.bind(paymentCaptureRequestQueue())
                .to(paymentExchange())
                .with(CAPTURE_PAYMENT_COMMAND_ROUTING_KEY);
    }

    @Bean
    Binding paymentCaptureRequestDeadLetterBinding() {
        return BindingBuilder.bind(paymentCaptureRequestDeadLetterQueue())
                .to(paymentDeadLetterExchange())
                .with(CAPTURE_PAYMENT_COMMAND_ROUTING_KEY);
    }

    @Bean
    Binding paymentRefundRequestBinding() {
        return BindingBuilder.bind(paymentRefundRequestQueue())
                .to(paymentExchange())
                .with(REFUND_PAYMENT_COMMAND_ROUTING_KEY);
    }

    @Bean
    Binding paymentRefundRequestDeadLetterBinding() {
        return BindingBuilder.bind(paymentRefundRequestDeadLetterQueue())
                .to(paymentDeadLetterExchange())
                .with(REFUND_PAYMENT_COMMAND_ROUTING_KEY);
    }
}
