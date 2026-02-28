package com.learning.kafka.service;

import com.learning.kafka.model.Order;

public interface OrderEventPublisher {

    void publishOrderCreated(Order order);

    void publishOrderConfirmed(Order order);

    void publishOrderCancelled(Order order);
}
