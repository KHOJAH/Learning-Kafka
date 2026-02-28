package com.learning.kafka.producer;

import com.learning.kafka.model.Inventory;
import com.learning.kafka.service.InventoryEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryProducer implements InventoryEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String INVENTORY_RESERVED_TOPIC = "inventory-reserved";
    private static final String INVENTORY_RELEASED_TOPIC = "inventory-released";

    public void sendInventoryReserved(Inventory inventory) {
        log.info("Sending inventory reserved event: {}", inventory.getReservationId());
        sendMessage(INVENTORY_RESERVED_TOPIC, inventory.getOrderId(), inventory,
            inventory.getReservationId(), inventory.getStatus().name());
    }

    @Override
    public void publishInventoryReserved(Inventory inventory) {
        sendInventoryReserved(inventory);
    }

    public void sendInventoryReleased(Inventory inventory) {
        log.info("Sending inventory released event: {}", inventory.getReservationId());
        sendMessage(INVENTORY_RELEASED_TOPIC, inventory.getOrderId(), inventory,
            inventory.getReservationId(), inventory.getStatus().name());
    }

    @Override
    public void publishInventoryReleased(Inventory inventory) {
        sendInventoryReleased(inventory);
    }

    private void sendMessage(String topic, String key, Object payload, String referenceId, String status) {
        kafkaTemplate.send(topic, key, payload)
                .whenComplete(handleSendCompletion(referenceId, status, topic));
    }

    private BiConsumer<SendResult<String, Object>, Throwable> handleSendCompletion(String referenceId, String status, String topic) {
        return (result, ex) -> {
            if (ex == null) {
                log.info("Inventory event sent successfully - Topic: {}, Partition: {}, Offset: {}, ReservationId: {}, Status: {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        referenceId,
                        status);
            } else {
                log.error("Failed to send inventory event: {}", ex.getMessage(), ex);
            }
        };
    }
}
