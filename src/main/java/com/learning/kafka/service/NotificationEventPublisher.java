package com.learning.kafka.service;

import com.learning.kafka.model.Notification;

public interface NotificationEventPublisher {

    void publishEmailNotification(Notification notification);

    void publishSmsNotification(Notification notification);
}
