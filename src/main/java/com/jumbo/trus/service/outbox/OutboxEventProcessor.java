package com.jumbo.trus.service.outbox;

import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.outbox.*;
import com.jumbo.trus.repository.OutboxEventRepository;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.header.OperationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final OperationContext operationContext;
    private final AppTeamService appTeamService;

    public void createEvent(
            OutboxEventType eventType,
            OutboxAggregateType aggregateType,
            Long aggregateId,
            OutboxEventPayload payload
    ) {
        OutboxEventEntity event = new OutboxEventEntity();

        event.setOperationId(operationContext.getOperationId());
        event.setEventType(eventType);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setAppTeamId(appTeamService.getCurrentAppTeamOrThrow().getId());
        event.setActorUserId(getCurrentUserId());
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