package com.learning.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topics.order.created:order-created}")
    private String orderCreatedTopic;

    @Value("${kafka.topics.order.confirmed:order-confirmed}")
    private String orderConfirmedTopic;

    @Value("${kafka.topics.order.cancelled:order-cancelled}")
    private String orderCancelledTopic;

    @Value("${kafka.topics.payment.processed:payment-processed}")
    private String paymentProcessedTopic;

    @Value("${kafka.topics.payment.failed:payment-failed}")
    private String paymentFailedTopic;

    @Value("${kafka.topics.inventory.reservation:inventory-reservation}")
    private String inventoryReservationTopic;

    @Value("${kafka.topics.inventory.reserved:inventory-reserved}")
    private String inventoryReservedTopic;

    @Value("${kafka.topics.inventory.released:inventory-released}")
    private String inventoryReleasedTopic;

    @Value("${kafka.topics.notification.email:notification-email}")
    private String notificationEmailTopic;

    @Value("${kafka.topics.notification.sms:notification-sms}")
    private String notificationSmsTopic;

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(orderCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderConfirmedTopic() {
        return TopicBuilder.name(orderConfirmedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name(orderCancelledTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentProcessedTopic() {
        return TopicBuilder.name(paymentProcessedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(paymentFailedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryReservationTopic() {
        return TopicBuilder.name(inventoryReservationTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryReservedTopic() {
        return TopicBuilder.name(inventoryReservedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryReleasedTopic() {
        return TopicBuilder.name(inventoryReleasedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationEmailTopic() {
        return TopicBuilder.name(notificationEmailTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationSmsTopic() {
        return TopicBuilder.name(notificationSmsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCreatedDlt() {
        return TopicBuilder.name(orderCreatedTopic + "-dlt")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentProcessedDlt() {
        return TopicBuilder.name(paymentProcessedTopic + "-dlt")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
