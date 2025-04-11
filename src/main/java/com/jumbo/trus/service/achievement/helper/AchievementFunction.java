package com.jumbo.trus.service.achievement.helper;

import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;

@FunctionalInterface
public interface AchievementFunction {
    PlayerAchievementDTO apply(PlayerDTO player, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType);
}