package com.learning.kafka.consumer;

import com.learning.kafka.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final Set<String> processedNotifications = ConcurrentHashMap.newKeySet();

    @KafkaListener(
            topics = "notification-email",
            groupId = "notification-email-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenEmailNotification(Notification notification, Acknowledgment ack) {
        log.info("Received email notification request: {}", notification.getNotificationId());
        log.info("Recipient: {}", notification.getRecipient());
        log.info("Type: {}", notification.getType());

        if (isDuplicate(notification.getNotificationId())) {
            log.warn("Duplicate notification detected - skipping: {}", notification.getNotificationId());
            ack.acknowledge();
            return;
        }

        try {
            Thread.sleep(100);
            
            log.info("Email notification sent successfully: {}", notification.getNotificationId());
            
            processedNotifications.add(notification.getNotificationId());
            ack.acknowledge();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Email notification sending interrupted: {}", e.getMessage(), e);
            throw new RuntimeException("Email notification interrupted", e);
        } catch (Exception e) {
            log.error("Failed to send email notification: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "notification-sms",
            groupId = "notification-sms-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenSMSNotification(Notification notification, Acknowledgment ack) {
        log.info("Received SMS notification request: {}", notification.getNotificationId());
        log.info("Recipient: {}", notification.getRecipient());
        log.info("Type: {}", notification.getType());

        if (isDuplicate(notification.getNotificationId())) {
            log.warn("Duplicate notification detected - skipping: {}", notification.getNotificationId());
            ack.acknowledge();
            return;
        }

        try {
            Thread.sleep(50);
            
            log.info("SMS notification sent successfully: {}", notification.getNotificationId());
            
            processedNotifications.add(notification.getNotificationId());
            ack.acknowledge();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("SMS notification sending interrupted: {}", e.getMessage(), e);
            throw new RuntimeException("SMS notification interrupted", e);
        } catch (Exception e) {
            log.error("Failed to send SMS notification: {}", e.getMessage(), e);
            throw e;
        }
    }

    private boolean isDuplicate(String notificationId) {
        return processedNotifications.contains(notificationId);
    }
}
