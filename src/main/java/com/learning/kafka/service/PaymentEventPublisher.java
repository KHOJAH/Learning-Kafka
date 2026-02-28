package com.learning.kafka.service;

import com.learning.kafka.model.Payment;

public interface PaymentEventPublisher {

    void publishPaymentProcessed(Payment payment);

    void publishPaymentFailed(Payment payment);
}
