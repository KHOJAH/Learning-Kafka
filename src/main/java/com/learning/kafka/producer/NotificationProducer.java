package com.learning.kafka.producer;

import com.learning.kafka.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String NOTIFICATION_EMAIL_TOPIC = "notification-email";
    private static final String NOTIFICATION_SMS_TOPIC = "notification-sms";

    public void sendEmailNotification(Notification notification) {
        log.info("Sending email notification: {}", notification.getNotificationId());
        kafkaTemplate.send(NOTIFICATION_EMAIL_TOPIC, notification.getOrderId(), notification)
                .whenComplete(handleSendResult(notification.getNotificationId(), notification.getType().name()));
    }

    public void sendSmsNotification(Notification notification) {
        log.info("Sending SMS notification: {}", notification.getNotificationId());
        kafkaTemplate.send(NOTIFICATION_SMS_TOPIC, notification.getOrderId(), notification)
                .whenComplete(handleSendResult(notification.getNotificationId(), notification.getType().name()));
    }

    private BiConsumer<SendResult<String, Object>, Throwable> handleSendResult(String notificationId, String type) {
        return (result, ex) -> {
            if (ex == null) {
                log.info("Event not ification sent - Partition: {}, Offset: {},notificationId: {},type: {}",
                        notificationId,
                        type,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send event notification: {}", ex.getMessage(), ex);
            }
        };
    }
}
