package com.learning.kafka.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
        log.info("Polling outbox for pending events...");

        List<OutboxEvent> pendingEvents = outboxRepository.findByStatus(OutboxEvent.EventStatus.PENDING);

        for (OutboxEvent event : pendingEvents) {
            try {
                log.info("Publishing event: {} - {}", event.getEventType(), event.getAggregateId());

                Object payload = objectMapper.readValue(event.getPayload(), Object.class);

                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), payload).get();

                event.markAsPublished();
                outboxRepository.save(event);

                log.info("Event published successfully: {}", event.getId());
            } catch (Exception e) {
                log.error("Failed to publish event: {} - {}", event.getId(), e.getMessage());

                if (event.getRetryCount() < MAX_RETRY_ATTEMPTS) {
                    event.markForRetry();
                } else {
                    event.markAsFailed(e.getMessage());
                }
                outboxRepository.save(event);
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
}
