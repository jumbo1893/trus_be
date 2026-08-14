package com.jumbo.trus.service.outbox;

import com.jumbo.trus.entity.outbox.OutboxEventEntity;
import com.jumbo.trus.entity.outbox.OutboxEventStatus;
import com.jumbo.trus.repository.OutboxEventRepository;
import com.jumbo.trus.service.achievement.AchievementService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxEventProcessorTest {

    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    private final AchievementEventProcessor eventProcessor = mock(AchievementEventProcessor.class);
    private final AchievementService achievementService = mock(AchievementService.class);
    private final OutboxProcessingProperties processingProperties = processingProperties();
    private final OutboxEventProcessor processor = new OutboxEventProcessor(
            repository,
            eventProcessor,
            achievementService,
            processingProperties
    );

    OutboxEventProcessorTest() {
        when(repository.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void marksEventsDoneOnlyAfterAchievementCalculationSucceeds() {
        OutboxEventEntity event = new OutboxEventEntity();
        AchievementEventBatch batch = new AchievementEventBatch(Map.of(), Map.of());
        whenReadyEvents(List.of(event));
        when(eventProcessor.createCalculationBatch(List.of(event))).thenReturn(batch);

        processor.processEvents();

        verify(achievementService).calculateEventBatch(batch);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.DONE);
    }

    @Test
    void continuesWithManagedEntitiesReturnedByClaimSave() {
        OutboxEventEntity selectedEvent = new OutboxEventEntity();
        OutboxEventEntity managedEvent = new OutboxEventEntity();
        managedEvent.setStatus(OutboxEventStatus.PROCESSING);
        AchievementEventBatch batch = new AchievementEventBatch(Map.of(), Map.of());
        whenReadyEvents(List.of(selectedEvent));
        when(repository.saveAllAndFlush(List.of(selectedEvent))).thenReturn(List.of(managedEvent));
        when(repository.saveAllAndFlush(List.of(managedEvent))).thenReturn(List.of(managedEvent));
        when(eventProcessor.createCalculationBatch(List.of(managedEvent))).thenReturn(batch);

        processor.processEvents();

        verify(achievementService).calculateEventBatch(batch);
        assertThat(managedEvent.getStatus()).isEqualTo(OutboxEventStatus.DONE);
    }

    @Test
    void schedulesWholeBatchForRetryWhenAchievementCalculationFails() {
        OutboxEventEntity event = new OutboxEventEntity();
        AchievementEventBatch batch = new AchievementEventBatch(Map.of(), Map.of());
        whenReadyEvents(List.of(event));
        when(eventProcessor.createCalculationBatch(List.of(event))).thenReturn(batch);
        doThrow(new IllegalStateException("calculation failed"))
                .when(achievementService).calculateEventBatch(any());

        processor.processEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.RETRY);
        assertThat(event.getLastError()).isEqualTo("calculation failed");
        assertThat(event.getNextAttemptAt()).isAfter(Instant.now());
    }

    @Test
    void marksEventFailedAfterMaximumNumberOfAttempts() {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setAttemptCount(processingProperties.getMaxAttempts() - 1);
        AchievementEventBatch batch = new AchievementEventBatch(Map.of(), Map.of());
        whenReadyEvents(List.of(event));
        when(eventProcessor.createCalculationBatch(List.of(event))).thenReturn(batch);
        doThrow(new IllegalStateException("calculation failed"))
                .when(achievementService).calculateEventBatch(any());

        processor.processEvents();

        assertThat(event.getAttemptCount()).isEqualTo(processingProperties.getMaxAttempts());
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getNextAttemptAt()).isNull();
    }

    @Test
    void recoversStaleProcessingEventForRetry() {
        OutboxEventEntity staleEvent = new OutboxEventEntity();
        staleEvent.setStatus(OutboxEventStatus.PROCESSING);
        staleEvent.setAttemptCount(1);
        staleEvent.setProcessingStartedAt(Instant.now().minus(Duration.ofHours(1)));
        when(repository.findTop100ByStatusAndProcessingStartedAtBeforeOrderByProcessingStartedAtAsc(
                eq(OutboxEventStatus.PROCESSING),
                any(Instant.class)
        )).thenReturn(List.of(staleEvent));

        processor.processEvents();

        assertThat(staleEvent.getStatus()).isEqualTo(OutboxEventStatus.RETRY);
        assertThat(staleEvent.getProcessingStartedAt()).isNull();
        assertThat(staleEvent.getLastError()).isEqualTo("Processing timed out before completion");
    }

    private void whenReadyEvents(List<OutboxEventEntity> events) {
        when(repository.findReadyForProcessing(
                eq(OutboxEventStatus.NEW),
                eq(OutboxEventStatus.RETRY),
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(events);
    }

    private OutboxProcessingProperties processingProperties() {
        OutboxProcessingProperties properties = new OutboxProcessingProperties();
        properties.setMaxAttempts(5);
        properties.setRetryDelay(Duration.ofMinutes(1));
        properties.setStaleTimeout(Duration.ofMinutes(10));
        return properties;
    }
}
