package com.learning.kafka.producer;

import com.learning.kafka.model.Order;
import com.learning.kafka.service.OrderEventPublisher;
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
public class OrderProducer implements OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String ORDER_CREATED_TOPIC = "order-created";
    private static final String ORDER_CONFIRMED_TOPIC = "order-confirmed";
    private static final String ORDER_CANCELLED_TOPIC = "order-cancelled";
    private static final String INVENTORY_RESERVATION_TOPIC = "inventory-reservation";

    @Override
    public void publishOrderCreated(Order order) {
        sendOrderCreated(order);
    }

    @Override
    public void publishOrderConfirmed(Order order) {
        sendOrderConfirmed(order);
    }

    @Override
    public void publishOrderCancelled(Order order) {
        sendOrderCancelled(order);
    }

    @Override
    public void publishInventoryReservationRequest(Order order) {
        log.info("Sending inventory reservation request: {}", order.getOrderId());
        sendMessage(INVENTORY_RESERVATION_TOPIC, order.getOrderId(), order, order.getCorrelationId());
    }

    public void sendOrderCreated(Order order) {
        log.info("Sending order created event: {}", order.getOrderId());
        sendMessage(ORDER_CREATED_TOPIC, order.getOrderId(), order, order.getCorrelationId());
    }

    public void sendOrderConfirmed(Order order) {
        log.info("Sending order confirmed event: {}", order.getOrderId());
        sendMessage(ORDER_CONFIRMED_TOPIC, order.getOrderId(), order, order.getCorrelationId());
    }

    public void sendOrderCancelled(Order order) {
        log.info("Sending order cancelled event: {}", order.getOrderId());
        sendMessage(ORDER_CANCELLED_TOPIC, order.getOrderId(), order, order.getCorrelationId());
    }

    private void sendMessage(String topic, String key, Object payload, String correlationId) {
        Headers headers = new RecordHeaders()
                .add("correlationId", correlationId.getBytes(StandardCharsets.UTF_8));

        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(topic, null, key, payload, headers);

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(producerRecord);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Order event sent successfully - Topic: {}, Partition: {}, Offset: {}, CorrelationId: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        correlationId);
            } else {
                log.error("Failed to send order event: {}", ex.getMessage(), ex);
            }
        });
    }
}
