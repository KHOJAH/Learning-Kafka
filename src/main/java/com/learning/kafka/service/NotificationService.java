package com.learning.kafka.service;

import com.learning.kafka.model.Notification;
import com.learning.kafka.model.Order;
import com.learning.kafka.producer.NotificationProducer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationProducer notificationProducer;

    @Transactional
    public Notification sendOrderConfirmation(Order order) {
        log.info("Sending order confirmation email to: {}", order.getCustomerEmail());

        Notification notification = Notification.createOrderConfirmation(order);

        try {
            Thread.sleep(100);
            log.info("Order confirmation email sent: {}", notification.getNotificationId());
            Notification sent = notification.markAsSent();
            notificationProducer.publishEmailNotification(sent);
            return sent;
        } catch (InterruptedException e) {
            log.error("Failed to send email: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return notification.markAsFailed("Email service unavailable");
        }
    }

    @Transactional
    public Notification sendPaymentFailure(Order order, String failureReason) {
        log.info("Sending payment failure email to: {}", order.getCustomerEmail());
        Notification notification = Notification.createPaymentFailure(order, failureReason);
        notificationProducer.publishEmailNotification(notification);
        return notification;
    }

    @Transactional
    public Notification sendOrderConfirmationSms(Order order, String phoneNumber) {
        String message = "Your order " + order.getOrderId() + " has been confirmed!";

        Notification notification = Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .orderId(order.getOrderId())
                .correlationId(order.getCorrelationId())
                .type(Notification.NotificationType.SMS)
                .recipient(phoneNumber)
                .message(message)
                .status(Notification.NotificationStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        log.info("Sending SMS to: {}", phoneNumber);
        Notification sent = notification.markAsSent();
        notificationProducer.publishSmsNotification(sent);
        return sent;
    }
}
