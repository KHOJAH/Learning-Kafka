package com.learning.kafka.consumer;

import com.learning.kafka.model.Notification;
import com.learning.kafka.model.Order;
import com.learning.kafka.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = {"notification-email"},
            groupId = "notification-email-group",
            errorHandler = "notificationErrorHandler")
    public void listenEmailNotification(Notification notification, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Received email notification request: {}", notification.getNotificationId());
        log.info("Topic: {}", topic);
        log.info("Recipient: {}", notification.getRecipient());

        try {
            Order order = Order.builder()
                    .orderId(notification.getOrderId())
                    .customerEmail(notification.getRecipient())
                    .build();

            Notification result = notificationService.sendOrderConfirmation(order);

            log.info("Email notification sent: {}", result.getNotificationId());
            
        } catch (Exception e) {
            log.error("Failed to send email notification: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = {"notification-sms"},
            groupId = "notification-sms-group",
            errorHandler = "notificationErrorHandler")
    public void listenSMSNotification(Notification notification, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Received SMS notification request: {}", notification.getNotificationId());
        log.info("Topic: {}", topic);
        log.info("Recipient: {}", notification.getRecipient());

        try {
            Order order = Order.builder()
                    .orderId(notification.getOrderId())
                    .build();

            Notification result = notificationService.sendOrderConfirmationSms(order, notification.getRecipient());

            log.info("SMS notification sent: {}", result.getNotificationId());
            
        } catch (Exception e) {
            log.error("Failed to send SMS notification: {}", e.getMessage(), e);
            throw e;
        }
    }

}
