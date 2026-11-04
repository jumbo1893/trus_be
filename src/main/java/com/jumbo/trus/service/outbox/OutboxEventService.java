package com.jumbo.trus.service.outbox;

import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.outbox.*;
import com.jumbo.trus.repository.OutboxEventRepository;
import com.jumbo.trus.service.auth.AppTeamService;
import org.springframework.beans.factory.ObjectProvider;
import com.jumbo.trus.service.header.OperationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final OperationContext operationContext;
    private final ObjectProvider<AppTeamService> appTeamService;

    public void createEvent(
            OutboxEventType eventType,
            OutboxAggregateType aggregateType,
            Long aggregateId,
            OutboxEventPayload payload
    ) {
        createEventForTeam(
                eventType,
                aggregateType,
                aggregateId,
                payload,
                appTeamService.getObject().getCurrentAppTeamOrThrow().getId(),
                getCurrentUserId()
        );
    }

    public void createAuditEvent(OutboxEventType type, Long aggregateId, Long appTeamId, Long actorUserId) {
        createAuditEvent(type, aggregateId, appTeamId, actorUserId, Map.of());
    }

    public void createAuditEvent(OutboxEventType type, Long aggregateId, Long appTeamId,
                                 Long actorUserId, Map<String, String> details) {
        createEventForTeam(type, OutboxAggregateType.OTHER, aggregateId,
                new OutboxEventPayload(null, null, null, null, null, null, details),
                appTeamId == null ? -1L : appTeamId,
                actorUserId == null ? getCurrentUserId() : actorUserId);
    }

    public void createEventForTeam(
            OutboxEventType eventType,
            OutboxAggregateType aggregateType,
            Long aggregateId,
            OutboxEventPayload payload,
            Long appTeamId,
            Long actorUserId
    ) {
        OutboxEventEntity event = new OutboxEventEntity();

        event.setOperationId(operationContext.getOperationId());
        event.setEventType(eventType);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setAppTeamId(appTeamId);
        event.setActorUserId(actorUserId);
        event.setPayload(payload);
        event.setSchemaVersion(1);
        event.setStatus(OutboxEventStatus.NEW);
        event.setAttemptCount(0);
        event.setCreatedAt(Instant.now());
        outboxEventRepository.save(event);
    }

    private Long getCurrentUserId() {
        try {
            UserEntity user = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return user.getId();
        } catch (Exception e) {
            return -1L;
        }
    }
}
