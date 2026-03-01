package com.learning.kafka.outbox;

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
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false)
    private String eventType;
    @Column(nullable = false)
    private String aggregateId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    @Column(nullable = false)
    private String topic;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;
    @Column(nullable = false)
    private Integer retryCount;
    private String errorMessage;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant publishedAt;

    public enum EventStatus {
        PENDING,
        PUBLISHED,
        FAILED,
        RETRYING
    }

    public static OutboxEvent create(String eventType, String aggregateId,
                                     String payload, String topic) {
        return OutboxEvent.builder()
                .eventType(eventType)
                .aggregateId(aggregateId)
                .payload(payload)
                .topic(topic)
                .status(EventStatus.PENDING)
                .retryCount(0)
                .createdAt(Instant.now())
                .build();
    }

    public void markAsPublished() {
        this.status = EventStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    public void markAsFailed(String error) {
        this.status = EventStatus.FAILED;
        this.errorMessage = error;
        this.retryCount = this.retryCount + 1;
    }

    public void markForRetry() {
        this.status = EventStatus.RETRYING;
        this.retryCount = this.retryCount + 1;
    }
}
