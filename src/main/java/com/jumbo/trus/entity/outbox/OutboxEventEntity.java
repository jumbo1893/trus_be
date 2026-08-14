package com.jumbo.trus.entity.outbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "outbox_event",
        indexes = {
                @Index(
                        name = "idx_outbox_event_processing",
                        columnList = "status, next_attempt_at, created_at"
                ),
                @Index(
                        name = "idx_outbox_event_operation",
                        columnList = "operation_id"
                ),
                @Index(
                        name = "idx_outbox_event_aggregate",
                        columnList = "aggregate_type, aggregate_id"
                ),
                @Index(
                        name = "idx_outbox_event_processing_started",
                        columnList = "status, processing_started_at"
                )
        }
)
@Getter
@Setter
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "operation_id",
            nullable = false,
            updatable = false
    )
    private UUID operationId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "event_type",
            nullable = false,
            length = 100,
            updatable = false
    )
    private OutboxEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "aggregate_type",
            nullable = false,
            length = 50,
            updatable = false
    )
    private OutboxAggregateType aggregateType;

    @Column(
            name = "aggregate_id",
            updatable = false
    )
    private Long aggregateId;

    @Column(
            name = "app_team_id",
            nullable = false,
            updatable = false
    )
    private Long appTeamId;

    @Column(
            name = "actor_user_id",
            updatable = false
    )
    private Long actorUserId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "payload",
            nullable = false,
            columnDefinition = "jsonb",
            updatable = false
    )
    private OutboxEventPayload payload;

    @Column(
            name = "schema_version",
            nullable = false,
            updatable = false
    )
    private int schemaVersion = 1;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private OutboxEventStatus status = OutboxEventStatus.NEW;

    @Column(
            name = "attempt_count",
            nullable = false
    )
    private int attemptCount = 0;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(
            name = "last_error",
            columnDefinition = "text"
    )
    private String lastError;

    /**
     * Ochrana proti souběžnému přepsání stejného eventu.
     */
    @Version
    @Column(
            name = "entity_version",
            nullable = false
    )
    private long entityVersion;

    @PrePersist
    protected void prePersist() {
        if (operationId == null) {
            operationId = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = Instant.now();
        }

        if (status == null) {
            status = OutboxEventStatus.NEW;
        }

        if (schemaVersion <= 0) {
            schemaVersion = 1;
        }
    }

    public void markAsProcessing() {
        this.status = OutboxEventStatus.PROCESSING;
        this.processingStartedAt = Instant.now();
        this.processedAt = null;
        this.nextAttemptAt = null;
        this.attemptCount++;
    }

    public void markAsDone() {
        this.status = OutboxEventStatus.DONE;
        this.processingStartedAt = null;
        this.processedAt = Instant.now();
        this.nextAttemptAt = null;
        this.lastError = null;
    }

    public void markForRetry(
            String error,
            Instant nextAttemptAt
    ) {
        this.status = OutboxEventStatus.RETRY;
        this.processingStartedAt = null;
        this.processedAt = null;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = shortenError(error);
    }

    public void markAsFailed(String error) {
        this.status = OutboxEventStatus.FAILED;
        this.processingStartedAt = null;
        this.nextAttemptAt = null;
        this.lastError = shortenError(error);
    }

    private String shortenError(String error) {
        if (error == null) {
            return null;
        }

        int maximumLength = 5_000;

        return error.length() <= maximumLength
                ? error
                : error.substring(0, maximumLength);
    }
}
