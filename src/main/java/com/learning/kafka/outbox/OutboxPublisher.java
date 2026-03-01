package com.learning.kafka.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRY_ATTEMPTS = 3;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        log.info("=== Polling outbox for pending events... ===");

        List<OutboxEvent> pendingEvents = outboxRepository.findByStatus(OutboxEvent.EventStatus.PENDING);

        log.info("Found {} pending event(s) in database", pendingEvents.size());

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Publishing {} pending event(s) to Kafka", pendingEvents.size());

        pendingEvents.forEach(event ->
                log.info("  - Event: {} | Type: {} | Topic: {} | AggregateId: {}",
                        event.getId(), event.getEventType(), event.getTopic(), event.getAggregateId())
        );

        for (OutboxEvent event : pendingEvents) {
            try {
                log.info("Publishing event: {} - {}", event.getEventType(), event.getAggregateId());

                Object payload = objectMapper.readValue(event.getPayload(), Object.class);

                Headers headers = new RecordHeaders()
                        .add("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8))
                        .add("aggregateId", event.getAggregateId().getBytes(StandardCharsets.UTF_8));

                ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(
                        event.getTopic(),
                        null,
                        event.getAggregateId(),
                        payload,
                        headers
                );

                kafkaTemplate.send(producerRecord).get();

                event.markAsPublished();
                outboxRepository.save(event);

                log.info("Event published successfully: {} - Topic: {}", event.getId(), event.getTopic());
            } catch (Exception e) {
                log.error("Failed to publish event: {} - {}", event.getId(), e.getMessage(), e);

                if (event.getRetryCount() < MAX_RETRY_ATTEMPTS) {
                    event.markForRetry();
                    outboxRepository.save(event);
                    log.info("Event marked for retry (attempt {}/{}): {}",
                            event.getRetryCount(), MAX_RETRY_ATTEMPTS, event.getId());
                } else {
                    event.markAsFailed(e.getMessage());
                    outboxRepository.save(event);
                    log.error("Event failed after {} retries: {}", MAX_RETRY_ATTEMPTS, event.getId());
                }
            }
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldEvents() {
        Instant cutoff = Instant.now().minusSeconds(86400);
        int deleted = outboxRepository.deleteByStatusAndPublishedBefore(cutoff);
        log.info("Cleaned up {} old published events", deleted);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processRetryingEvents() {
        log.debug("Checking for events due for retry...");

        Instant dueDate = Instant.now().minusSeconds(5);
        List<OutboxEvent> retryingEvents = outboxRepository.findEventsDueForRetry(MAX_RETRY_ATTEMPTS, dueDate);

        if (retryingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} event(s) due for retry", retryingEvents.size());

        for (OutboxEvent event : retryingEvents) {
            try {
                log.info("Retrying event: {} - {}", event.getEventType(), event.getAggregateId());

                Object payload = objectMapper.readValue(event.getPayload(), Object.class);

                Headers headers = new RecordHeaders()
                        .add("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8))
                        .add("aggregateId", event.getAggregateId().getBytes(StandardCharsets.UTF_8));

                ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(
                        event.getTopic(),
                        null,
                        event.getAggregateId(),
                        payload,
                        headers
                );

                kafkaTemplate.send(producerRecord).get();

                event.markAsPublished();
                outboxRepository.save(event);

                log.info("Event retry successful: {}", event.getId());
            } catch (Exception e) {
                log.error("Retry failed for event: {} - {}", event.getId(), e.getMessage(), e);

                if (event.getRetryCount() >= MAX_RETRY_ATTEMPTS) {
                    event.markAsFailed(e.getMessage());
                    outboxRepository.save(event);
                    log.error("Event failed after max retries: {}", event.getId());
                }
            }
        }
    }
}
