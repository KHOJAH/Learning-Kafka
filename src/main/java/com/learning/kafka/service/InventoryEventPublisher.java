package com.learning.kafka.service;

import com.learning.kafka.model.Inventory;

public interface InventoryEventPublisher {

    void publishInventoryReserved(Inventory inventory);

    void publishInventoryReleased(Inventory inventory);
}
