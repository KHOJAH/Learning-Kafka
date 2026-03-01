package com.learning.kafka.producer;

import com.learning.kafka.model.Inventory;
import com.learning.kafka.outbox.OutboxHelper;
import com.learning.kafka.service.InventoryEventPublisher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryProducer implements InventoryEventPublisher {

    private final OutboxHelper outboxHelper;

    private static final String INVENTORY_RESERVED_TOPIC = "inventory-reserved";
    private static final String INVENTORY_RELEASED_TOPIC = "inventory-released";

    @Override
    @Transactional
    public void publishInventoryReserved(Inventory inventory) {
        log.info("Saving inventory reserved event to outbox: {}", inventory.getReservationId());
        outboxHelper.saveOutboxEvent("INVENTORY_RESERVED", inventory.getReservationId(), inventory, INVENTORY_RESERVED_TOPIC);
    }

    @Override
    @Transactional
    public void publishInventoryReleased(Inventory inventory) {
        log.info("Saving inventory released event to outbox: {}", inventory.getReservationId());
        outboxHelper.saveOutboxEvent("INVENTORY_RELEASED", inventory.getReservationId(), inventory, INVENTORY_RELEASED_TOPIC);
    }

    public void sendInventoryReserved(Inventory inventory) {
        publishInventoryReserved(inventory);
    }

    public void sendInventoryReleased(Inventory inventory) {
        publishInventoryReleased(inventory);
    }
}
