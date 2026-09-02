package com.jumbo.trus.service.achievement;

public record AchievementProgressMessage(
        Long playerId,
        String achievementName,
        String missingText
) {
}
