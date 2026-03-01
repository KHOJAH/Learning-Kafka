package com.learning.kafka.producer;

import com.learning.kafka.model.Order;
import com.learning.kafka.outbox.OutboxHelper;
import com.learning.kafka.service.OrderEventPublisher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProducer implements OrderEventPublisher {

    private final OutboxHelper outboxHelper;

    private static final String ORDER_CREATED_TOPIC = "order-created";
    private static final String ORDER_CONFIRMED_TOPIC = "order-confirmed";
    private static final String ORDER_CANCELLED_TOPIC = "order-cancelled";
    private static final String ORDER_FAILED_TOPIC = "order-failed";
    private static final String INVENTORY_RESERVATION_TOPIC = "inventory-reservation";

    @Override
    @Transactional
    public void publishOrderCreated(Order order) {
        log.info("Saving order created event to outbox: {}", order.getOrderId());
        outboxHelper.saveOutboxEvent("ORDER_CREATED", order.getOrderId(), order, ORDER_CREATED_TOPIC);
    }

    @Override
    @Transactional
    public void publishOrderConfirmed(Order order) {
        log.info("Saving order confirmed event to outbox: {}", order.getOrderId());
        outboxHelper.saveOutboxEvent("ORDER_CONFIRMED", order.getOrderId(), order, ORDER_CONFIRMED_TOPIC);
    }

    @Override
    @Transactional
    public void publishOrderCancelled(Order order) {
        log.info("Saving order cancelled event to outbox: {}", order.getOrderId());
        outboxHelper.saveOutboxEvent("ORDER_CANCELLED", order.getOrderId(), order, ORDER_CANCELLED_TOPIC);
    }

    @Override
    @Transactional
    public void publishOrderFailed(Order order) {
        log.info("Saving order failed event to outbox: {}", order.getOrderId());
        outboxHelper.saveOutboxEvent("ORDER_FAILED", order.getOrderId(), order, ORDER_FAILED_TOPIC);
    }

    @Override
    @Transactional
    public void publishInventoryReservationRequest(Order order) {
        log.info("Saving inventory reservation request to outbox: {}", order.getOrderId());
        outboxHelper.saveOutboxEvent("INVENTORY_RESERVATION_REQUEST", order.getOrderId(), order, INVENTORY_RESERVATION_TOPIC);
    }
}
