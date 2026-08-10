package com.marketplace.orders.payment;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentMessagingConfiguration {

    public static final String EXCHANGE = "marketplace.payments.v1";
    public static final String DEAD_LETTER_EXCHANGE = "marketplace.payments.dlx.v1";
    public static final String RESULT_QUEUE = "orders.payment-results.v1";
    public static final String RESULT_DEAD_LETTER_QUEUE = "orders.payment-results.dlq.v1";
    public static final String CAPTURE_PAYMENT_COMMAND_ROUTING_KEY = "payment.command.capture.v1";
    public static final String PAYMENT_CAPTURED_EVENT_ROUTING_KEY = "payment.event.captured.v1";
    public static final String PAYMENT_FAILED_EVENT_ROUTING_KEY = "payment.event.failed.v1";

    @Bean
    DirectExchange paymentExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange paymentDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue paymentResultQueue() {
        return QueueBuilder.durable(RESULT_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .build();
    }

    @Bean
    Queue paymentResultDeadLetterQueue() {
        return QueueBuilder.durable(RESULT_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding paymentCapturedBinding() {
        return BindingBuilder.bind(paymentResultQueue())
                .to(paymentExchange())
                .with(PAYMENT_CAPTURED_EVENT_ROUTING_KEY);
    }

    @Bean
    Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentResultQueue())
                .to(paymentExchange())
                .with(PAYMENT_FAILED_EVENT_ROUTING_KEY);
    }

    @Bean
    Binding paymentResultDeadLetterCapturedBinding() {
        return BindingBuilder.bind(paymentResultDeadLetterQueue())
                .to(paymentDeadLetterExchange())
                .with(PAYMENT_CAPTURED_EVENT_ROUTING_KEY);
    }

    @Bean
    Binding paymentResultDeadLetterFailedBinding() {
        return BindingBuilder.bind(paymentResultDeadLetterQueue())
                .to(paymentDeadLetterExchange())
                .with(PAYMENT_FAILED_EVENT_ROUTING_KEY);
    }
}
