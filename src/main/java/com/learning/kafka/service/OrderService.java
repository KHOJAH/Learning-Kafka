package com.learning.kafka.service;

import com.learning.kafka.dto.OrderRequest;
import com.learning.kafka.model.Order;
import com.learning.kafka.producer.OrderProducer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderProducer orderProducer;

    @Transactional
    public Order createOrder(OrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        Order order = Order.createNew(
                request.getCustomerId(),
                request.getCustomerEmail(),
                request.getTotalAmount(),
                request.getItems(),
                request.getShippingAddress()
        );

        orderProducer.publishOrderCreated(order);

        log.info("Order created and event saved to outbox: {}", order.getOrderId());
        return order;
    }

    @Transactional
    public Order processOrder(Order order) {
        log.info("Processing order: {}", order.getOrderId());

        if (order.getTotalAmount().doubleValue() < 50) {
            log.info("Order amount too low, cancelling: {}", order.getOrderId());
            Order cancelled = order.cancel();
            orderProducer.publishOrderCancelled(cancelled);
            return cancelled;
        }

        Order processingOrder = order.transitionToProcessing();
        log.info("Order validated and ready for payment: {}", processingOrder.getOrderId());
        return processingOrder;
    }

    @Transactional
    public Order confirmOrder(Order order) {
        log.info("Confirming order: {}", order.getOrderId());
        Order confirmed = order.confirm();
        orderProducer.publishOrderConfirmed(confirmed);
        log.info("Order confirmed: {}", confirmed.getOrderId());
        return confirmed;
    }

    @Transactional
    public void cancelOrder(Order order) {
        log.info("Cancelling order: {}", order.getOrderId());
        Order cancelled = order.cancel();
        orderProducer.publishOrderCancelled(cancelled);
        log.info("Order cancelled successfully: {}", cancelled.getOrderId());
    }

    @Transactional
    public void failOrder(Order order, String reason) {
        log.info("Failing order: {} - Reason: {}", order.getOrderId(), reason);
        Order failed = order.fail(reason);
        orderProducer.publishOrderFailed(failed);
        log.info("Order failed: {}", failed.getOrderId());
    }
}
