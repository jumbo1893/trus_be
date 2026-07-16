package com.jumbo.trus.service.achievement.helper;

import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.achievement.AchievementEntity;
import org.springframework.stereotype.Component;

@Component
public class AchievementEligibilityService {

    public boolean canHaveAchievement(
            PlayerEntity player,
            AchievementEntity achievement
    ) {
        if (player == null || achievement == null) {
            return false;
        }

        boolean onlyForPlayers =
                Boolean.TRUE.equals(achievement.getOnlyForPlayers());

        boolean fan =
                Boolean.TRUE.equals(player.isFan());

        return !onlyForPlayers || !fan;
    }
}