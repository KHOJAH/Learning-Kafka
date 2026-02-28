package com.learning.kafka.service;

import com.learning.kafka.exception.NonRetryableException;
import com.learning.kafka.model.Inventory;
import com.learning.kafka.model.Notification;
import com.learning.kafka.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryEventPublisher inventoryEventPublisher;
    private final OrderEventPublisher orderEventPublisher;
    private final NotificationEventPublisher notificationEventPublisher;
    private final NotificationService notificationService;
    private final OrderService orderService;
    private final Random random = new Random();
    private final Set<String> processedReservations = ConcurrentHashMap.newKeySet();

    public void reserveInventoryAndPublish(Inventory inventory) {
        Inventory result = reserveInventory(inventory);

        if (result.getStatus() == Inventory.ReservationStatus.RESERVED) {
            log.info("Inventory reserved successfully, confirming order: {}", result.getOrderId());

            inventoryEventPublisher.publishInventoryReserved(result);

            Order order = Order.builder()
                    .orderId(result.getOrderId())
                    .correlationId(result.getCorrelationId())
                    .build();
            Order confirmedOrder = orderService.confirmOrder(order);

            Notification notification = notificationService.sendOrderConfirmation(confirmedOrder);
            notificationEventPublisher.publishEmailNotification(notification);
        } else {
            log.warn("Inventory reservation failed, releasing: {}", result.getOrderId());
            inventoryEventPublisher.publishInventoryReleased(result);
        }
    }

    public Inventory reserveInventory(Inventory inventory) {
        log.info("Reserving inventory for order: {}", inventory.getOrderId());

        if (processedReservations.contains(inventory.getReservationId())) {
            log.warn("Duplicate reservation request - ignoring: {}", inventory.getReservationId());
            throw new NonRetryableException("Duplicate reservation: " + inventory.getReservationId());
        }

        Inventory result = Inventory.create(
                inventory.getOrderId(),
                inventory.getCorrelationId(),
                inventory.getSku(),
                inventory.getQuantity(),
                inventory.getWarehouseId()
        );

        if (random.nextInt(100) < 90) {
            processedReservations.add(inventory.getReservationId());
            return result.reserve();
        } else {
            log.error("Out of stock for order: {}", inventory.getOrderId());
            return result.fail("Insufficient stock");
        }
    }

    public Inventory releaseInventory(Order order) {
        log.info("Releasing inventory for order: {}", order.getOrderId());
        processedReservations.remove(order.getIdempotencyKey());
        Inventory inventory = Inventory.create(order.getOrderId(),
                order.getCorrelationId(),
                order.getItems(),
                1,
                "WAREHOUSE-001");

        return inventory.release();
    }
}
