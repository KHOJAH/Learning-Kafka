package com.learning.kafka.producer;

import com.learning.kafka.model.Order;
import com.learning.kafka.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String PAYMENT_PROCESSED_TOPIC = "payment-processed";
    private static final String PAYMENT_FAILED_TOPIC = "payment-failed";
    private static final String ORDER_CONFIRMED_TOPIC = "order-confirmed";

    public void sendPaymentProcessed(Payment payment) {
        log.info("Sending payment processed event: {}", payment.getPaymentId());
        sendMessage(PAYMENT_PROCESSED_TOPIC, payment.getOrderId(), payment, payment.getPaymentId());
    }

    public void sendPaymentFailed(Payment payment) {
        log.info("Sending payment failed event: {}", payment.getPaymentId());
        sendMessage(PAYMENT_FAILED_TOPIC, payment.getOrderId(), payment, payment.getPaymentId());
    }

    public void sendPaymentProcessedTransactional(Payment payment, Order confirmedOrder) {
        log.info("Sending payment and order in transaction: paymentId={}", payment.getPaymentId());

        kafkaTemplate.executeInTransaction(operations -> {
            operations.send(PAYMENT_PROCESSED_TOPIC, payment.getOrderId(), payment);
            operations.send(ORDER_CONFIRMED_TOPIC, confirmedOrder.getOrderId(), confirmedOrder);
            return null;
        });

        log.info("Transaction completed successfully for paymentId={}", payment.getPaymentId());
    }

    private void sendMessage(String topic, String key, Object payload, String referenceId) {
        kafkaTemplate.send(topic, key, payload)
                .whenComplete(handleSendCompletion(referenceId));
    }

    private BiConsumer<SendResult<String, Object>, Throwable> handleSendCompletion(String referenceId) {
        return (result, ex) -> {
            if (ex == null) {
                log.info("Payment event sent successfully for reference [{}] - Partition: {}, Offset: {}",
                        referenceId,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send Payment event for reference [{}]: {}", referenceId, ex.getMessage(), ex);
            }
        };
    }
}
