package com.learning.kafka.consumer;

import com.learning.kafka.model.Inventory;
import com.learning.kafka.model.Order;
import com.learning.kafka.service.InventoryService;
import com.learning.kafka.service.NotificationEventPublisher;
import com.learning.kafka.service.NotificationService;
import com.learning.kafka.service.OrderService;
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
public class InventoryConsumer {

    private final OrderService orderService;
    private final NotificationService notificationService;
    private final NotificationEventPublisher notificationEventPublisher;
    private final InventoryService inventoryService;
    private final Set<String> processedKeys = ConcurrentHashMap.newKeySet();

    @KafkaListener(
            topics = "inventory-reservation",
            groupId = "inventory-reservation-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenInventoryReservation(Order order, Acknowledgment ack) {
        log.info("Received inventory reservation request: {}", order.getOrderId());

        if (isDuplicate(order.getIdempotencyKey())) {
            log.warn("Duplicate message detected - skipping: {}", order.getIdempotencyKey());
            ack.acknowledge();
            return;
        }

        try {
            inventoryService.reserveInventoryAndPublish(order);

            processedKeys.add(order.getIdempotencyKey());
            ack.acknowledge();
            log.info("Inventory reservation processed: {}", order.getOrderId());

        } catch (Exception e) {
            log.error("Inventory reservation failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "inventory-reserved",
            groupId = "order-confirmation-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processInventoryReserved(Inventory inventory, Acknowledgment ack) {
        log.info("Received inventory reserved event: {}", inventory.getReservationId());

        if (isDuplicate(inventory.getReservationId())) {
            log.warn("Duplicate message detected - skipping: {}", inventory.getReservationId());
            ack.acknowledge();
            return;
        }

        try {
            Order order = buildOrderFromInventory(inventory);

            Order confirmedOrder = orderService.confirmOrder(order);

            notificationService.sendOrderConfirmation(confirmedOrder);
            processedKeys.add(inventory.getReservationId());
            ack.acknowledge();

            log.info("Order confirmed and notification sent: {}", order.getOrderId());

        } catch (Exception e) {
            log.error("Order confirmation failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "inventory-released",
            groupId = "inventory-failure-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processInventoryReleased(Inventory inventory, Acknowledgment ack) {
        log.info("Received inventory released event: {}", inventory.getReservationId());

        if (isDuplicate(inventory.getReservationId())) {
            log.warn("Duplicate message detected - skipping: {}", inventory.getReservationId());
            ack.acknowledge();
            return;
        }

        try {
            Order order = buildOrderFromInventory(inventory);

            orderService.failOrder(order, inventory.getFailureReason());

            processedKeys.add(inventory.getReservationId());
            ack.acknowledge();

            log.info("Order failed due to inventory issue: {}", order.getOrderId());

        } catch (Exception e) {
            log.error("Inventory failure handling failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    private Order buildOrderFromInventory(Inventory inventory) {
        return Order.builder()
                .orderId(inventory.getOrderId())
                .correlationId(inventory.getCorrelationId())
                .build();
    }

    private boolean isDuplicate(String key) {
        return processedKeys.contains(key);
    }
}
