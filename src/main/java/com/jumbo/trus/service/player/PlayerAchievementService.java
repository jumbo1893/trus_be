package com.jumbo.trus.service.player;

import com.jumbo.trus.dto.player.PlayerSetup;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.service.achievement.AchievementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerAchievementService {

    private final PlayerService playerService;
    private final AchievementService achievementService;

    public PlayerSetup setupPlayerWithAchievements(Long playerId, AppTeamEntity appTeamEntity) {
        PlayerSetup playerSetup = playerService.setupPlayer(playerId, appTeamEntity);
        if (playerId != null) {
            playerSetup.setAchievementPlayerDetail(achievementService.getAchievementsForPlayer(playerId));
        }
        log.debug("vracím player setup");
        return playerSetup;
    }
}
