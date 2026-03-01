package com.learning.kafka.producer;

import com.learning.kafka.model.Notification;
import com.learning.kafka.outbox.OutboxHelper;
import com.learning.kafka.service.NotificationEventPublisher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProducer implements NotificationEventPublisher {

    private final OutboxHelper outboxHelper;

    private static final String NOTIFICATION_EMAIL_TOPIC = "notification-email";
    private static final String NOTIFICATION_SMS_TOPIC = "notification-sms";

    @Override
    @Transactional
    public void publishEmailNotification(Notification notification) {
        log.info("Saving email notification event to outbox: {}", notification.getNotificationId());
        outboxHelper.saveOutboxEvent("NOTIFICATION_EMAIL", notification.getNotificationId(), notification, NOTIFICATION_EMAIL_TOPIC);
    }

    @Override
    @Transactional
    public void publishSmsNotification(Notification notification) {
        log.info("Saving SMS notification event to outbox: {}", notification.getNotificationId());
        outboxHelper.saveOutboxEvent("NOTIFICATION_SMS", notification.getNotificationId(), notification, NOTIFICATION_SMS_TOPIC);
    }
}
