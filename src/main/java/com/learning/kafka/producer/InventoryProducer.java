package com.learning.kafka.producer;

import com.learning.kafka.model.Inventory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String INVENTORY_RESERVED_TOPIC = "inventory-reserved";
    private static final String INVENTORY_RELEASED_TOPIC = "inventory-released";

    public void sendInventoryReserved(Inventory inventory) {
        log.info("Sending inventory reserved event: {}", inventory.getInventoryId());

        kafkaTemplate.send(INVENTORY_RESERVED_TOPIC, inventory.getOrderId(), inventory)
                .whenComplete(handleSendResult(inventory.getInventoryId(), inventory.getStatus().name()));
    }

    public void sendInventoryReleased(Inventory inventory) {
        log.info("Sending inventory released event: {}", inventory.getInventoryId());

        kafkaTemplate.send(INVENTORY_RELEASED_TOPIC, inventory.getOrderId(), inventory)
                .whenComplete(handleSendResult(inventory.getInventoryId(), inventory.getStatus().name()));
    }

    private static BiConsumer<SendResult<String, Object>, Throwable> handleSendResult(String inventoryId, String status) {
        return (result, ex) -> {
            if (ex == null) {
                log.info("Inventory  event sent - Partition: {}, Offset: {}, inventoryId: {}, status: {}",
                        inventoryId,
                        status,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send inventory  event: {}", ex.getMessage(), ex);
            }
        };
    }
}
