package com.learning.kafka.service;

import com.learning.kafka.dto.OrderRequest;
import com.learning.kafka.model.Order;
import com.learning.kafka.producer.OrderProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderProducer orderProducer;

    public Order createOrder(OrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        Order order = Order.createNew(
                request.getCustomerId(),
                request.getCustomerEmail(),
                request.getTotalAmount(),
                request.getItems(),
                request.getShippingAddress()
        );

        orderProducer.sendOrderCreatedAsync(order);

        log.info("Order created successfully: {}", order.getOrderId());
        return order;
    }

    public Order confirmOrder(Order order) {
        log.info("Confirming order: {}", order.getOrderId());
        orderProducer.sendOrderConfirmed(order.confirm());
        log.info("Order confirmed successfully: {}", order.getOrderId());
        return order;
    }

    public Order cancelOrder(Order order) {
        log.info("Canceling order: {}", order.getOrderId());
        orderProducer.sendOrderCancelled(order.cancel());
        log.info("Order cancelled successfully: {}", order.getOrderId());
        return order;
    }
}
