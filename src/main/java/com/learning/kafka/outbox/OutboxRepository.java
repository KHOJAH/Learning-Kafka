package com.learning.kafka.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findByStatus(OutboxEvent.EventStatus status);

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'RETRYING' " +
            "AND e.retryCount < :maxRetries " +
            "AND e.createdAt < :dueDate")
    List<OutboxEvent> findEventsDueForRetry(@Param("maxRetries") int maxRetries,
                                            @Param("dueDate") Instant dueDate);
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = 'PUBLISHED' " +
            "AND e.publishedAt < :cutoffDate")
    int deleteByStatusAndPublishedBefore(@Param("cutoffDate") Instant cutoffDate);

}
