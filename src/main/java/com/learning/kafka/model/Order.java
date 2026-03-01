package com.learning.kafka.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private String orderId;
    private String customerId;
    private String customerEmail;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String items;
    private String shippingAddress;
    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;
    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;
    private String correlationId;
    private String idempotencyKey;

    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        PROCESSING,
        SHIPPED,
        DELIVERED,
        CANCELLED,
        FAILED
    }

    public static Order createNew(String customerId, String customerEmail,
                                  BigDecimal totalAmount, String items,
                                  String shippingAddress) {
        String orderId = UUID.randomUUID().toString();

        return Order.builder()
                .orderId(orderId)
                .customerId(customerId)
                .customerEmail(customerEmail)
                .totalAmount(totalAmount)
                .items(items)
                .shippingAddress(shippingAddress)
                .status(OrderStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .correlationId(UUID.randomUUID().toString())
                .idempotencyKey("ORDER_" + orderId)
                .build();
    }

    /**
     * Transitions order to PROCESSING state after validation.
     */
    public Order transitionToProcessing() {
        return Order.builder()
                .orderId(this.orderId)
                .customerId(this.customerId)
                .customerEmail(this.customerEmail)
                .totalAmount(this.totalAmount)
                .items(this.items)
                .shippingAddress(this.shippingAddress)
                .status(OrderStatus.PROCESSING)
                .createdAt(this.createdAt)
                .updatedAt(Instant.now())
                .correlationId(this.correlationId)
                .idempotencyKey(this.idempotencyKey)
                .build();
    }

    /**
     * Confirms order after successful payment and inventory reservation.
     */
    public Order confirm() {
        return Order.builder()
                .orderId(this.orderId)
                .customerId(this.customerId)
                .customerEmail(this.customerEmail)
                .totalAmount(this.totalAmount)
                .items(this.items)
                .shippingAddress(this.shippingAddress)
                .status(OrderStatus.CONFIRMED)
                .createdAt(this.createdAt)
                .updatedAt(Instant.now())
                .correlationId(this.correlationId)
                .idempotencyKey(this.idempotencyKey)
                .build();
    }

    /**
     * Cancels the order.
     */
    public Order cancel() {
        return Order.builder()
                .orderId(this.orderId)
                .customerId(this.customerId)
                .customerEmail(this.customerEmail)
                .totalAmount(this.totalAmount)
                .items(this.items)
                .shippingAddress(this.shippingAddress)
                .status(OrderStatus.CANCELLED)
                .createdAt(this.createdAt)
                .updatedAt(Instant.now())
                .correlationId(this.correlationId)
                .idempotencyKey(this.idempotencyKey)
                .build();
    }

    /**
     * Fails the order due to an error (e.g., payment failure, insufficient stock).
     */
    public Order fail(String reason) {
        return Order.builder()
                .orderId(this.orderId)
                .customerId(this.customerId)
                .customerEmail(this.customerEmail)
                .totalAmount(this.totalAmount)
                .items(this.items)
                .shippingAddress(this.shippingAddress)
                .status(OrderStatus.FAILED)
                .createdAt(this.createdAt)
                .updatedAt(Instant.now())
                .correlationId(this.correlationId)
                .idempotencyKey(this.idempotencyKey)
                .build();
    }
}
