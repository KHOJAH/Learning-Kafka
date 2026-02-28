package com.learning.kafka.service;

import com.learning.kafka.exception.NonRetryableException;
import com.learning.kafka.model.Inventory;
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
    private final Random random = new Random();
    private final Set<String> processedReservations = ConcurrentHashMap.newKeySet();

    public void reserveInventoryAndPublish(Inventory inventory) {
        Inventory result = reserveInventory(inventory);

        if (result.getStatus() == Inventory.ReservationStatus.RESERVED) {
            log.info("Inventory reserved successfully: {}", result.getOrderId());
            publishInventoryReserved(result);
        } else {
            log.warn("Inventory reservation failed: {}", result.getOrderId());
            publishInventoryReleased(result);
        }
    }

    public void publishInventoryReserved(Inventory inventory) {
        log.info("Publishing inventory reserved event: {}", inventory.getReservationId());
        inventoryEventPublisher.publishInventoryReserved(inventory);
    }

    public void publishInventoryReleased(Inventory inventory) {
        log.info("Publishing inventory released event: {}", inventory.getReservationId());
        inventoryEventPublisher.publishInventoryReleased(inventory);
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
