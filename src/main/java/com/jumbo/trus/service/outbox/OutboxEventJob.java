package com.jumbo.trus.service.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventJob {

    private final OutboxEventProcessor outboxEventProcessor;

    @Scheduled(
            fixedDelayString = "${outbox.processing.fixed-delay-ms:60000}",
            initialDelayString = "${outbox.processing.initial-delay-ms:60000}"
    )
    public void processAchievementEvents() {
        outboxEventProcessor.processEvents();
    }
}
