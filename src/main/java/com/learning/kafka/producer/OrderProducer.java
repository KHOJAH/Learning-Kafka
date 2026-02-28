package com.learning.kafka.producer;

import com.learning.kafka.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
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

    private void processOrder(String logMessage, Order order, String topic, String successMessage) {
        Headers headers = new RecordHeaders()
                .add("correlationId", order.getCorrelationId().getBytes(StandardCharsets.UTF_8));

        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(topic, null, order.getOrderId(), order, headers);

        log.info(logMessage, order.getOrderId());
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(producerRecord);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info(successMessage,
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send order event: {}", ex.getMessage(), ex);
            }
        });
    }
}
