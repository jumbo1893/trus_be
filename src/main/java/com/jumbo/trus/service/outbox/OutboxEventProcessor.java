package com.jumbo.trus.service.outbox;

import com.jumbo.trus.entity.outbox.OutboxEventEntity;
import com.jumbo.trus.entity.outbox.OutboxEventStatus;
import com.jumbo.trus.repository.OutboxEventRepository;
import com.jumbo.trus.service.achievement.AchievementService;
import com.jumbo.trus.service.achievement.AchievementProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final AchievementEventProcessor achievementEventProcessor;
    private final AchievementService achievementService;
    private final AchievementProgressService achievementProgressService;
    private final OutboxProcessingProperties processingProperties;


    public void processEvents() {
        Instant now = Instant.now();
        recoverStaleProcessingEvents(now);

        List<OutboxEventEntity> newEvents = outboxEventRepository.findReadyForProcessing(
                OutboxEventStatus.NEW,
                OutboxEventStatus.RETRY,
                now,
                PageRequest.of(0, 100)
        );
        if (newEvents.isEmpty()) {
            return;
        }
        newEvents.forEach(OutboxEventEntity::markAsProcessing);
        List<OutboxEventEntity> claimedEvents = outboxEventRepository.saveAllAndFlush(newEvents);
        processEventsForAchievements(claimedEvents);
        outboxEventRepository.saveAllAndFlush(claimedEvents);
    }

    private void processEventsForAchievements(List<OutboxEventEntity> events) {
        try {
            AchievementEventBatch calculationBatch = achievementEventProcessor.createCalculationBatch(events);
            achievementService.calculateEventBatch(calculationBatch);
            try {
                achievementProgressService.evaluateAndNotify(calculationBatch);
            } catch (Exception progressException) {
                log.error(
                        "Achievement progress notifications failed for {} events; core achievement processing remains successful",
                        events.size(),
                        progressException
                );
            }
            events.forEach(OutboxEventEntity::markAsDone);
        } catch (Exception e) {
            log.error("Failed to prepare achievement calculation batch for {} events", events.size(), e);
            for (OutboxEventEntity event : events) {
                retryOrFail(event, e.getMessage(), Instant.now());
            }
        }
    }

    private void recoverStaleProcessingEvents(Instant now) {
        Instant processingStartedBefore = now.minus(processingProperties.getStaleTimeout());
        List<OutboxEventEntity> staleEvents = outboxEventRepository
                .findTop100ByStatusAndProcessingStartedAtBeforeOrderByProcessingStartedAtAsc(
                        OutboxEventStatus.PROCESSING,
                        processingStartedBefore
                );
        if (staleEvents.isEmpty()) {
            return;
        }

        log.warn("Recovering {} stale outbox events that started processing before {}",
                staleEvents.size(), processingStartedBefore);
        staleEvents.forEach(event -> retryOrFail(
                event,
                "Processing timed out before completion",
                now
        ));
        outboxEventRepository.saveAllAndFlush(staleEvents);
    }

    private void retryOrFail(OutboxEventEntity event, String error, Instant now) {
        if (event.getAttemptCount() >= processingProperties.getMaxAttempts()) {
            event.markAsFailed(error);
            return;
        }
        event.markForRetry(error, now.plus(processingProperties.getRetryDelay()));
    }
}
