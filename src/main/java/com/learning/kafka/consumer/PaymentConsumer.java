package com.learning.kafka.consumer;

import com.learning.kafka.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    @KafkaListener(topics = "payment-processed", groupId = "payment-confirmation-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void listenPaymentProcessed(Payment payment, Acknowledgment ack) {
        log.info("Received payment processed event: {}", payment.getPaymentId());
        log.info("Order ID: {}, Amount: {}", payment.getOrderId(), payment.getAmount());

        try {
            // Payment already processed and events published by service
            // Consumer just acknowledges successful processing
            ack.acknowledge();
            log.info("Payment confirmation processed successfully: {}", payment.getPaymentId());

        } catch (Exception e) {
            log.error("Payment confirmation processing failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @DltHandler
    public void handleDlt(Payment payment, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Payment message sent to DLT after all retries from topic: {}", topic);
        log.error("Payment ID: {}", payment.getPaymentId());
        log.error("Order ID: {}", payment.getOrderId());
        log.error("Correlation ID: {}", payment.getCorrelationId());
        log.error("Amount: {}", payment.getAmount());
        log.error("Status: {}", payment.getStatus());

        // In production: alert operations team, store for manual review
        // The correlation ID helps trace the message across all services
    }

}
