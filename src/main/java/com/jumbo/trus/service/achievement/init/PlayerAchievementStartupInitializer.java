package com.jumbo.trus.service.achievement.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerAchievementStartupInitializer {

    private final PlayerAchievementInitializationService initializationService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeMissingAchievements() {
        int initialized =
                initializationService
                        .initializeAllMissingPlayerAchievements();

        log.info(
                "Player achievement initialization finished, created={}",
                initialized
        );
    }
}