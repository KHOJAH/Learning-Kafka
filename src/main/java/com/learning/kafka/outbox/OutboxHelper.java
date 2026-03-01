package com.learning.kafka.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxHelper {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void saveOutboxEvent(String eventType, String aggregateId, Object payload, String topic) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.create(eventType, aggregateId, payloadJson, topic);
            outboxRepository.save(event);
            log.debug("Outbox event saved: {} - {}", eventType, aggregateId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload for event: {}", eventType, e);
            throw new RuntimeException("Failed to serialize event payload", e);
        }
    }
}
