package com.jumbo.trus.repository;

import com.jumbo.trus.entity.outbox.OutboxEventEntity;
import com.jumbo.trus.entity.outbox.OutboxEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEventEntity, Long> {

    @Query("""
            SELECT event
            FROM OutboxEventEntity event
            WHERE event.status = :newStatus
               OR (event.status = :retryStatus
                   AND (event.nextAttemptAt IS NULL OR event.nextAttemptAt <= :now))
            ORDER BY event.createdAt ASC
            """)
    List<OutboxEventEntity> findReadyForProcessing(
            @Param("newStatus") OutboxEventStatus newStatus,
            @Param("retryStatus") OutboxEventStatus retryStatus,
            @Param("now") Instant now,
            Pageable pageable
    );

    List<OutboxEventEntity> findTop100ByStatusAndProcessingStartedAtBeforeOrderByProcessingStartedAtAsc(
            OutboxEventStatus status,
            Instant processingStartedBefore
    );
}
