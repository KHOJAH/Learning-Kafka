package com.learning.kafka.service;

import com.learning.kafka.exception.NonRetryableException;
import com.learning.kafka.exception.RetryableException;
import com.learning.kafka.model.Order;
import com.learning.kafka.model.Payment;
import com.learning.kafka.producer.PaymentProducer;
import jakarta.transaction.Transactional;
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

    private final PaymentProducer paymentProducer;
    private final Random random = new Random();

    @Transactional
    public Payment processPayment(Order order) {
        log.info("Processing payment for order: {}", order.getOrderId());

        Payment payment = Payment.create(
                order.getOrderId(),
                order.getCorrelationId(),
                order.getTotalAmount(),
                Payment.PaymentMethod.CREDIT_CARD
        );

        try {
            Payment result = processPayment(payment);

            if (COMPLETED.equals(result.getStatus())) {
                log.info("Payment successful: {}", result.getPaymentId());
                paymentProducer.publishPaymentProcessed(result);
            } else if (FAILED.equals(result.getStatus())) {
                log.error("Payment failed: {}", result.getPaymentId());
                paymentProducer.publishPaymentFailed(result);
            }
            return result;
        } catch (RetryableException e) {
            log.warn("Temporary payment failure - will retry: {}", order.getOrderId());
            throw e;
        } catch (NonRetryableException e) {
            log.error("Permanent payment failure: {}", order.getOrderId());
            Payment failed = payment.fail(e.getMessage());
            paymentProducer.publishPaymentFailed(failed);
            return failed;
        }
    }

    private Payment processPayment(Payment payment) {
        int outcome = random.nextInt(100);

        if (outcome < 70) {
            return payment.complete();
        } else if (outcome < 90) {
            throw new RetryableException("Payment gateway temporarily unavailable");
        } else {
            throw new NonRetryableException("Invalid payment method");
        }
    }
}
