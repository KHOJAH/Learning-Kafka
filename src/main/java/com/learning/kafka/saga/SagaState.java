package com.learning.kafka.saga;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "saga_state")
public class SagaState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String sagaId;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStep currentStep;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private String errorMessage;

    public enum SagaStep {
        ORDER_CREATED,

        PAYMENT_PROCESSED,

        INVENTORY_RESERVED,

        ORDER_CONFIRMED,

        COMPENSATING,

        COMPLETED,

        FAILED
    }

    public enum SagaStatus {
        IN_PROGRESS,

        COMPLETED,

        FAILED,

        TIMED_OUT
    }

    public static SagaState create(String orderId, String correlationId) {
        Instant now = Instant.now();
        return SagaState.builder()
                .sagaId(correlationId)
                .orderId(orderId)
                .correlationId(correlationId)
                .currentStep(SagaStep.ORDER_CREATED)
                .status(SagaStatus.IN_PROGRESS)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void updateStep(SagaStep step) {
        this.currentStep = step;
        this.updatedAt = Instant.now();
    }

    public void markAsCompleted() {
        this.status = SagaStatus.COMPLETED;
        this.currentStep = SagaStep.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void markAsFailed(String error) {
        this.status = SagaStatus.FAILED;
        this.currentStep = SagaStep.FAILED;
        this.errorMessage = error;
        this.updatedAt = Instant.now();
    }

    public void markForCompensation() {
        this.currentStep = SagaStep.COMPENSATING;
        this.updatedAt = Instant.now();
    }
}
