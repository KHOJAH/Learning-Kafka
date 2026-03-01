package com.learning.kafka.consumer;

import com.learning.kafka.metrics.KafkaMetricsBinder;
import com.learning.kafka.model.Order;
import com.learning.kafka.service.OrderService;
import com.learning.kafka.service.PaymentService;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.consumer.mode", havingValue = "standard", matchIfMissing = true)
public class OrderConsumer {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final KafkaMetricsBinder metricsBinder;
    private final Set<String> processedKeys = ConcurrentHashMap.newKeySet();

    @KafkaListener(
            topics = "order-created",
            groupId = "order-processor-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processOrderCreated(Order order, Acknowledgment ack) {
        log.info("Received order created event: {}", order.getOrderId());

        // Start processing timer
        Timer.Sample timerSample = metricsBinder.startProcessingTimer();

        if (isDuplicate(order.getIdempotencyKey())) {
            log.warn("Duplicate message detected - skipping: {}", order.getIdempotencyKey());
            metricsBinder.stopProcessingTimer(timerSample);
            ack.acknowledge();
            return;
        }

        try {
            Order processingOrder = orderService.processOrder(order);

            if (processingOrder.getStatus() == Order.OrderStatus.CANCELLED) {
                processedKeys.add(order.getIdempotencyKey());
                metricsBinder.stopProcessingTimer(timerSample);
                ack.acknowledge();
                return;
            }

            paymentService.processPayment(processingOrder);

            // Record successful order processing
            metricsBinder.incrementOrdersProcessed();

            processedKeys.add(order.getIdempotencyKey());
            metricsBinder.stopProcessingTimer(timerSample);
            ack.acknowledge();
            log.info("Order processed and payment initiated: {}", order.getOrderId());

        } catch (Exception e) {
            log.error("Order processing failed: {}", e.getMessage(), e);
            metricsBinder.stopProcessingTimer(timerSample);
            throw e;
        }
    }

    @KafkaListener(
            topics = "order-confirmed",
            groupId = "order-notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processOrderConfirmed(Order order) {
        log.info("Received order confirmed event for notification: {}", order.getOrderId());
    }

    @KafkaListener(
            topics = "order-cancelled",
            groupId = "order-cancellation-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processOrderCancelled(Order order, Acknowledgment ack) {
        log.info("Received order cancelled event: {}", order.getOrderId());

        if (isDuplicate(order.getIdempotencyKey())) {
            log.warn("Duplicate message detected - skipping: {}", order.getIdempotencyKey());
            ack.acknowledge();
            return;
        }

        try {
            processedKeys.add(order.getIdempotencyKey());
            ack.acknowledge();
            log.info("Order cancellation acknowledged: {}", order.getOrderId());

        } catch (Exception e) {
            log.error("Order cancellation handling failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "order-failed",
            groupId = "order-failure-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processOrderFailed(Order order, Acknowledgment ack) {
        log.info("Received order failed event: {}", order.getOrderId());

        if (isDuplicate(order.getIdempotencyKey())) {
            log.warn("Duplicate message detected - skipping: {}", order.getIdempotencyKey());
            ack.acknowledge();
            return;
        }

        try {
            processedKeys.add(order.getIdempotencyKey());
            ack.acknowledge();
            log.info("Order failure acknowledged: {}", order.getOrderId());

        } catch (Exception e) {
            log.error("Order failure handling failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    private boolean isDuplicate(String idempotencyKey) {
        return processedKeys.contains(idempotencyKey);
    }
}
