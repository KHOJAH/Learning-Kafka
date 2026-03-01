package com.learning.kafka.service;

import com.learning.kafka.exception.NonRetryableException;
import com.learning.kafka.model.Inventory;
import com.learning.kafka.model.Order;
import com.learning.kafka.producer.InventoryProducer;
import jakarta.transaction.Transactional;
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

    private final InventoryProducer inventoryProducer;
    private final Random random = new Random();
    private final Set<String> processedReservations = ConcurrentHashMap.newKeySet();

    @Transactional
    public void reserveInventoryAndPublish(Order order) {
        log.info("Reserving inventory for order: {}", order.getOrderId());

        Inventory inventory = Inventory.create(
                order.getOrderId(),
                order.getCorrelationId(),
                order.getItems(),
                1,
                "WAREHOUSE-001"
        );

        Inventory result = processReservation(inventory);

        if (result.getStatus() == Inventory.ReservationStatus.RESERVED) {
            log.info("Inventory reserved successfully: {}", result.getOrderId());
            inventoryProducer.publishInventoryReserved(result);
        } else if (result.getStatus() == Inventory.ReservationStatus.FAILED) {
            log.warn("Inventory reservation failed: {}", result.getOrderId());
            inventoryProducer.publishInventoryReleased(result);
        }

    }

    private Inventory processReservation(Inventory inventory) {
        if (processedReservations.contains(inventory.getReservationId())) {
            log.warn("Duplicate reservation request - ignoring: {}", inventory.getReservationId());
            throw new NonRetryableException("Duplicate reservation: " + inventory.getReservationId());
        }

        if (random.nextInt(100) < 90) {
            processedReservations.add(inventory.getReservationId());
            return inventory.reserve();
        } else {
            log.error("Out of stock for order: {}", inventory.getOrderId());
            return inventory.fail("Insufficient stock");
        }
    }

    @Transactional
    public Inventory releaseInventoryAndPublish(Order order) {
        log.info("Releasing inventory for order: {}", order.getOrderId());

        processedReservations.remove(order.getIdempotencyKey());

        Inventory inventory = Inventory.create(
                order.getOrderId(),
                order.getCorrelationId(),
                order.getItems(),
                1,
                "WAREHOUSE-001"
        );

        Inventory result = inventory.release();
        log.info("Inventory released: {}", result.getReservationId());

        inventoryProducer.publishInventoryReleased(result);
        return result;
    }

    public Inventory reserveInventory(Inventory inventory) {
        log.info("Reserving inventory (no publish): {}", inventory.getOrderId());
        return processReservation(inventory);
    }

    public Inventory releaseInventory(Order order) {
        log.info("Releasing inventory (no publish): {}", order.getOrderId());

        processedReservations.remove(order.getIdempotencyKey());

        Inventory inventory = Inventory.create(
                order.getOrderId(),
                order.getCorrelationId(),
                order.getItems(),
                1,
                "WAREHOUSE-001"
        );

        return inventory.release();
    }
}
