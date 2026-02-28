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
public class Payment {
    private String paymentId;
    private String orderId;
    private String correlationId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String failureReason;
    private String transactionId;
    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant processedAt;

    public enum PaymentMethod {
        CREDIT_CARD,
        DEBIT_CARD,
        PAYPAL,
        BANK_TRANSFER,
        CRYPTO
    }

    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        REFUNDED
    }

    public static Payment create(String orderId, String correlationId,
                                 BigDecimal amount, PaymentMethod method) {

        return Payment.builder()
                .paymentId(UUID.randomUUID().toString())
                .orderId(orderId)
                .correlationId(correlationId)
                .amount(amount)
                .paymentMethod(method)
                .status(PaymentStatus.PENDING)
                .transactionId("TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .processedAt(Instant.now())
                .build();
    }

    public Payment complete() {
        return Payment.builder()
                .paymentId(this.paymentId)
                .orderId(this.orderId)
                .correlationId(this.correlationId)
                .amount(this.amount)
                .paymentMethod(this.paymentMethod)
                .status(PaymentStatus.COMPLETED)
                .transactionId(this.transactionId)
                .processedAt(Instant.now())
                .build();
    }

    public Payment fail(String reason) {
        return Payment.builder()
                .paymentId(this.paymentId)
                .orderId(this.orderId)
                .correlationId(this.correlationId)
                .amount(this.amount)
                .paymentMethod(this.paymentMethod)
                .status(PaymentStatus.FAILED)
                .transactionId(this.transactionId)
                .processedAt(Instant.now())
                .failureReason(reason)
                .build();
    }
}
