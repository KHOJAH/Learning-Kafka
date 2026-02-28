package com.learning.kafka.service;

import com.learning.kafka.exception.NonRetryableException;
import com.learning.kafka.model.Order;
import com.learning.kafka.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

import static com.learning.kafka.model.Payment.PaymentStatus.COMPLETED;
import static com.learning.kafka.model.Payment.PaymentStatus.FAILED;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentEventPublisher paymentEventPublisher;
    private final Random random = new Random();

    public Payment processPaymentAndPublish(Order order) {
        Payment payment = processPayment(order);

        if (COMPLETED.equals(payment.getStatus()))
            paymentEventPublisher.publishPaymentProcessed(payment);
        else if (FAILED.equals(payment.getStatus()))
            paymentEventPublisher.publishPaymentFailed(payment);

        return payment;
    }

    public Payment processPayment(Order order) {
        log.info("Processing payment for order: {}", order.getOrderId());

        Payment payment = Payment.create(order.getOrderId(),
                order.getCorrelationId(),
                order.getTotalAmount(),
                Payment.PaymentMethod.CREDIT_CARD);

        int outcome = random.nextInt(100);

        if (outcome < 70) {
            log.info("Payment successful: {}", payment.getPaymentId());
            return payment.complete();
        } else if (outcome < 90) {
            log.warn("Temporary payment failure - will retry: {}", payment.getPaymentId());
            return handlePaymentFailure(order, "Payment gateway temporarily unavailable");
            // We Can use the RetryableException or make it failed
//            throw new RetryableException("Payment gateway temporarily unavailable");
        } else {
            log.error("Permanent payment failure: {}", payment.getPaymentId());
            throw new NonRetryableException("Invalid payment method");
        }
    }

    public Payment handlePaymentFailure(Order order, String reason) {
        Payment payment = Payment.create(
                order.getOrderId(),
                order.getCorrelationId(),
                order.getTotalAmount(),
                Payment.PaymentMethod.CREDIT_CARD
        );
        return payment.fail(reason);
    }
}
