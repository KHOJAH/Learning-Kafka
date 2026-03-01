package com.learning.kafka.repository;

import com.learning.kafka.saga.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, String> {

    Optional<SagaState> findByOrderId(String orderId);

    List<SagaState> findByStatus(SagaState.SagaStatus status);

    List<SagaState> findByCurrentStep(SagaState.SagaStep step);

    List<SagaState> findByStatusAndUpdatedAtBefore(SagaState.SagaStatus status, Instant cutoffDate);

    @Query("SELECT s FROM SagaState s WHERE s.status IN ('FAILED', 'TIMED_OUT') " +
            "AND s.currentStep = 'COMPENSATING'")
    List<SagaState> findSagasNeedCompensation();

    @Modifying
    @Query("DELETE FROM SagaState s WHERE s.status = :status " +
            "AND s.updatedAt < :cutoffDate")
    int deleteByStatusAndUpdatedBefore(@Param("status") SagaState.SagaStatus status,
                                       @Param("cutoffDate") Instant cutoffDate);
}
