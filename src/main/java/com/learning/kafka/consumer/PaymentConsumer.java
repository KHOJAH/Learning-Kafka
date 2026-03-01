package com.learning.kafka.consumer;

import com.learning.kafka.model.Order;
import com.learning.kafka.model.Payment;
import com.learning.kafka.service.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.learning.kafka.model.Payment.PaymentStatus.COMPLETED;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final OrderEventPublisher orderEventPublisher;
    private final Set<String> processedKeys = ConcurrentHashMap.newKeySet();

    @KafkaListener(
            topics = "payment-processed",
            groupId = "payment-confirmation-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenPaymentProcessed(Payment payment, Acknowledgment ack) {
        log.info("Received payment processed event: {}", payment.getPaymentId());

        if (isDuplicate(payment.getPaymentId())) {
            log.warn("Duplicate message detected - skipping: {}", payment.getPaymentId());
            ack.acknowledge();
            return;
        }

        try {
            if (COMPLETED.equals(payment.getStatus())) {
                log.info("Payment completed successfully, triggering inventory reservation: {}", payment.getOrderId());

                Order order = buildOrderFromPayment(payment);
                orderEventPublisher.publishInventoryReservationRequest(order);
            }

            processedKeys.add(payment.getPaymentId());
            ack.acknowledge();
            log.info("Payment confirmation processed: {}", payment.getPaymentId());

        } catch (Exception e) {
            log.error("Payment confirmation processing failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "payment-failed",
            groupId = "payment-failure-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenPaymentFailed(Payment payment, Acknowledgment ack) {
        log.info("Received payment failed event: {}", payment.getPaymentId());

        if (isDuplicate(payment.getPaymentId())) {
            log.warn("Duplicate message detected - skipping: {}", payment.getPaymentId());
            ack.acknowledge();
            return;
        }

        try {
            log.info("Payment failed, failing order: {}", payment.getOrderId());

            Order order = buildOrderFromPayment(payment);
            orderEventPublisher.publishOrderFailed(order);

            processedKeys.add(payment.getPaymentId());
            ack.acknowledge();
            log.info("Payment failure handled: {}", payment.getPaymentId());

        } catch (Exception e) {
            log.error("Payment failure handling failed: {}", e.getMessage(), e);
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
    }

    private Order buildOrderFromPayment(Payment payment) {
        return Order.builder()
                .orderId(payment.getOrderId())
                .correlationId(payment.getCorrelationId())
                .totalAmount(payment.getAmount())
                .idempotencyKey("ORDER_" + payment.getOrderId())
                .build();
    }

    private boolean isDuplicate(String key) {
        return processedKeys.contains(key);
    }
}
