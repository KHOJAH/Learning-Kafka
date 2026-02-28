package com.learning.kafka.service;

import com.learning.kafka.dto.OrderRequest;
import com.learning.kafka.model.Order;
import com.learning.kafka.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.learning.kafka.model.Payment.PaymentStatus.COMPLETED;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderEventPublisher orderEventPublisher;
    private final PaymentService paymentService;

    public Order createOrder(OrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        Order order = Order.createNew(
                request.getCustomerId(),
                request.getCustomerEmail(),
                request.getTotalAmount(),
                request.getItems(),
                request.getShippingAddress()
        );

        orderEventPublisher.publishOrderCreated(order);

        log.info("Order created and event published: {}", order.getOrderId());
        return order;
    }

    public void processOrder(Order order) {
        log.info("Processing order: {}", order.getOrderId());

        try {
            if (order.getTotalAmount().doubleValue() < 50) {
                log.info("Order amount too low, cancelling: {}", order.getOrderId());
                return;
            }

            Payment payment = paymentService.processPaymentAndPublish(order);

            if (COMPLETED.equals(payment.getStatus())) {
                // Publish event to trigger inventory reservation via Kafka
                orderEventPublisher.publishInventoryReservationRequest(order);
            }

            log.info("Order processed successfully: {}", order.getOrderId());

        } catch (Exception e) {
            log.error("Order processing failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    public void cancelOrder(Order order) {
        log.info("Cancelling order: {}", order.getOrderId());

        try {
            Order cancelled = order.cancel();
            orderEventPublisher.publishOrderCancelled(cancelled);

            // Step 2: Release inventory (if needed)
            // In a full implementation, this would check if inventory was reserved
            // and call inventoryService.releaseInventoryAndPublish(order)

            // Step 3: Refund payment (if needed)
            // In a full implementation, this would check if payment was processed
            // and call paymentService.refundPaymentAndPublish(order)

            log.info("Order cancelled successfully: {}", order.getOrderId());

        } catch (Exception e) {
            log.error("Order cancellation failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    public Order confirmOrder(Order order) {
        log.info("Confirming order: {}", order.getOrderId());
        Order confirmed = order.confirm();
        orderEventPublisher.publishOrderConfirmed(confirmed);
        log.info("Order confirmed: {}", order.getOrderId());
        return confirmed;
    }
}
