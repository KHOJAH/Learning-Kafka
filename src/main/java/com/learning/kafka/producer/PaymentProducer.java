package com.learning.kafka.producer;

import com.learning.kafka.model.Payment;
import com.learning.kafka.outbox.OutboxHelper;
import com.learning.kafka.service.PaymentEventPublisher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProducer implements PaymentEventPublisher {

    private final OutboxHelper outboxHelper;

    private static final String PAYMENT_PROCESSED_TOPIC = "payment-processed";
    private static final String PAYMENT_FAILED_TOPIC = "payment-failed";

    @Override
    @Transactional
    public void publishPaymentProcessed(Payment payment) {
        log.info("Saving payment processed event to outbox: {}", payment.getPaymentId());
        outboxHelper.saveOutboxEvent("PAYMENT_PROCESSED", payment.getPaymentId(), payment, PAYMENT_PROCESSED_TOPIC);
    }

    @Override
    @Transactional
    public void publishPaymentFailed(Payment payment) {
        log.info("Saving payment failed event to outbox: {}", payment.getPaymentId());
        outboxHelper.saveOutboxEvent("PAYMENT_FAILED", payment.getPaymentId(), payment, PAYMENT_FAILED_TOPIC);
    }

    public void sendPaymentProcessed(Payment payment) {
        publishPaymentProcessed(payment);
    }

    public void sendPaymentFailed(Payment payment) {
        publishPaymentFailed(payment);
    }
}
