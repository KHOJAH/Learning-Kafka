package com.learning.kafka.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private String notificationId;
    private String orderId;
    private String correlationId;
    private NotificationType type;
    private String recipient;
    private String subject;
    private String message;
    private NotificationStatus status;
    private String errorMessage;
    private Integer retryCount;
    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;
    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant sentAt;

    public enum NotificationType {
        EMAIL,
        SMS,
        WHATSAPP
    }

    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED,
        RETRYING
    }

    public static Notification createOrderConfirmation(Order order) {
        String subject = "Order Confirmation - " + order.getOrderId();
        String message = String.format("""
                                Dear Valued Customer,
                        
                        Your order has been confirmed!
                        
                        Order ID: %s
                        Total Amount: $%.2f
                        Items: %s
                        Shipping Address: %s
                        
                        Thank you for your purchase!
                        
                        Best regards,
                        E-Commerce Team
                        """,
                order.getOrderId(),
                order.getTotalAmount(),
                order.getItems(),
                order.getShippingAddress());

        return Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .orderId(order.getOrderId())
                .correlationId(order.getCorrelationId())
                .type(NotificationType.EMAIL)
                .recipient(order.getCustomerEmail())
                .subject(subject)
                .message(message)
                .status(NotificationStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    public static Notification createPaymentFailure(Order order, String failureReason) {
        String subject = "Payment Failed - Order : " + order.getOrderId();
        String message = String.format("""
                                Dear Valued Customer,
                        
                        Your order has been confirmed!
                        
                        Order ID: %s
                        Total Amount: $%.2f
                        Items: %s
                        Shipping Address: %s
                        Payment Failure Reason: %s
                        
                        Thank you for your purchase!
                        
                        Best regards,
                        E-Commerce Team
                        """,
                order.getOrderId(),
                order.getTotalAmount(),
                order.getItems(),
                order.getShippingAddress(),
                failureReason);

        return Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .orderId(order.getOrderId())
                .correlationId(order.getCorrelationId())
                .type(NotificationType.EMAIL)
                .recipient(order.getCustomerEmail())
                .subject(subject)
                .message(message)
                .status(NotificationStatus.FAILED)
                .build();
    }

    public Notification markAsSent() {
        Instant now = Instant.now();
        return Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .orderId(this.getOrderId())
                .correlationId(this.getCorrelationId())
                .type(NotificationType.EMAIL)
                .subject(subject)
                .message(message)
                .status(NotificationStatus.SENT)
                .sentAt(now)
                .build();
    }

    public Notification markAsFailed(String error) {
        Instant now = Instant.now();
        return Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .orderId(this.getOrderId())
                .correlationId(this.getCorrelationId())
                .type(NotificationType.EMAIL)
                .subject(subject)
                .message(message)
                .status(NotificationStatus.FAILED)
                .createdAt(now)
                .errorMessage(error)
                .retryCount(this.getRetryCount() + 1)
                .build();
    }
}
