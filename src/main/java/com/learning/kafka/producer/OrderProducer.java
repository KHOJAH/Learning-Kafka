package com.learning.kafka.producer;

import com.learning.kafka.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String ORDER_CREATED_TOPIC = "order-created";
    private static final String ORDER_CONFIRMED_TOPIC = "order-confirmed";
    private static final String ORDER_CANCELLED_TOPIC = "order-cancelled";

    public void sendOrderCreated(Order order) {
        log.info("Sending order created event: {}", order.getOrderId());
        kafkaTemplate.send(ORDER_CREATED_TOPIC, order.getOrderId(), order);
    }

    public void sendOrderCreatedAsync(Order order) {
        processOrder("Sending order created event (async): {}", order, ORDER_CREATED_TOPIC, "Order created event sent successfully - Topic: {}, Partition: {}, Offset: {}");
    }

    public void sendOrderConfirmed(Order order) {
        processOrder("Sending order confirmed event: {}", order, ORDER_CONFIRMED_TOPIC, "Order confirmed event sent successfully - Topic: {}, Partition: {}, Offset: {}");
    }

    public void sendOrderCancelled(Order order) {
        processOrder("Sending order cancelled event: {}", order, ORDER_CANCELLED_TOPIC, "Order cancelled event sent successfully - Topic: {}, Partition: {}, Offset: {}");
    }

    private void processOrder(String s, Order order, String orderCancelledTopic, String s1) {
        MessageHeaders headers = new MessageHeaders(Map.of("correlationId", order.getCorrelationId()));
        GenericMessage<Order> orderMessage = new GenericMessage<>(order, headers);
        log.info(s, order.getOrderId());
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(orderCancelledTopic, order.getOrderId(), orderMessage);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info(s1,
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send order created event: {}", ex.getMessage(), ex);
            }
        });
    }
}
