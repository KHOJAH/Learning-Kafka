package com.learning.kafka.producer;

import com.learning.kafka.model.Notification;
import com.learning.kafka.service.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProducer implements NotificationEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String NOTIFICATION_EMAIL_TOPIC = "notification-email";
    private static final String NOTIFICATION_SMS_TOPIC = "notification-sms";

    public void sendEmailNotification(Notification notification) {
        log.info("Sending email notification: {}", notification.getNotificationId());
        sendMessage(NOTIFICATION_EMAIL_TOPIC, notification.getOrderId(), notification,
            notification.getNotificationId(), notification.getType().name());
    }

    @Override
    public void publishEmailNotification(Notification notification) {
        sendEmailNotification(notification);
    }

    public void sendSmsNotification(Notification notification) {
        log.info("Sending SMS notification: {}", notification.getNotificationId());
        sendMessage(NOTIFICATION_SMS_TOPIC, notification.getOrderId(), notification,
            notification.getNotificationId(), notification.getType().name());
    }

    @Override
    public void publishSmsNotification(Notification notification) {
        sendSmsNotification(notification);
    }

    private void sendMessage(String topic, String key, Object payload, String notificationId, String type) {
        kafkaTemplate.send(topic, key, payload)
                .whenComplete(handleSendCompletion(notificationId, type, topic));
    }

    private BiConsumer<SendResult<String, Object>, Throwable> handleSendCompletion(String notificationId, String type, String topic) {
        return (result, ex) -> {
            if (ex == null) {
                log.info("Notification sent successfully - Topic: {}, Partition: {}, Offset: {}, NotificationId: {}, Type: {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        notificationId,
                        type);
            } else {
                log.error("Failed to send notification: {}", ex.getMessage(), ex);
            }
        };
    }
}
