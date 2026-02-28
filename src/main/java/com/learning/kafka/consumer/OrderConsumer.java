package com.learning.kafka.consumer;

import com.learning.kafka.model.Order;
import com.learning.kafka.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderService orderService;
    private final Set<String> processedKeys = ConcurrentHashMap.newKeySet();

    @KafkaListener(topics = "order-created", groupId = "order-processor-group", containerFactory = "kafkaListenerContainerFactory")
    public void processOrderCreated(Order order, Acknowledgment ack) {
        log.info("Received order created event: {}", order.getOrderId());
        log.info("Customer: {}, Amount: {}", order.getCustomerId(), order.getTotalAmount());

        if (processedKeys.contains(order.getIdempotencyKey())) {
            log.warn("Duplicate message detected - skipping: {}", order.getIdempotencyKey());
            ack.acknowledge();
            return;
        }

        try {
            orderService.processOrder(order);
            processedKeys.add(order.getIdempotencyKey());
            ack.acknowledge();
            log.info("Order processed successfully: {}", order.getOrderId());

        } catch (Exception e) {
            log.error("Order processing failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = "order-confirmed", groupId = "order-notification-group", containerFactory = "kafkaListenerContainerFactory")
    public void processOrderConfirmed(Order order) {
        log.info("Received order confirmed event: {}", order.getOrderId());
        log.info("Order confirmation will trigger notification: {}", order.getOrderId());
    }

    @KafkaListener(topics = "order-cancelled", groupId = "order-cancellation-group", containerFactory = "kafkaListenerContainerFactory")
    public void processOrderCancelled(Order order, Acknowledgment ack) {
        log.info("Received order cancelled event: {}", order.getOrderId());

        if (processedKeys.contains(order.getIdempotencyKey())) {
            log.warn("Duplicate message detected - skipping: {}", order.getIdempotencyKey());
            ack.acknowledge();
            return;
        }

        try {
            orderService.cancelOrder(order);

            processedKeys.add(order.getIdempotencyKey());
            ack.acknowledge();
            log.info("Order cancellation processed: {}", order.getOrderId());

        } catch (Exception e) {
            log.error("Order cancellation failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}
