package com.jumbo.trus.service.player;

import com.jumbo.trus.dto.achievement.IPlayerAchievementStats;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.player.PlayerSetup;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.achievement.AchievementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerAchievementService {

    private final PlayerStatsFacade playerStatsFacade;
    private final AchievementService achievementService;
    private final PlayerAchievementRepository playerAchievementRepository;
    private final PlayerAchievementMapper playerAchievementMapper;
    private final PlayerAchievementMapper mapper;

    public PlayerSetup setupPlayerWithAchievements(Long playerId, AppTeamEntity appTeamEntity) {
        PlayerSetup playerSetup = playerStatsFacade.setupPlayer(playerId, appTeamEntity);
        if (playerId != null) {
            playerSetup.setAchievementPlayerDetail(achievementService.getAchievementsForPlayer(playerId));
        }
        return playerSetup;
    }

    public List<PlayerAchievementDTO> getLastPlayerAchievements(int count, List<Long> playerIds) {
        return playerAchievementRepository.findLastAccomplishedByPlayers(
                playerIds,
                PageRequest.of(0, count)
        ).stream().map(playerAchievementMapper::toDTO).toList();
    }

    public List<IPlayerAchievementStats> getListOfPlayersOrderAccomplishedAchievements(long appTeamId, int count) {
        return
                playerAchievementRepository.findTopAchievementStatsByAppTeam(
                        appTeamId,
                        PageRequest.of(0, count)
                );
    }

    public List<PlayerAchievementDTO> getAllAccomplishedAchievementsByMatch(long appTeamId, Long  matchId, Long  footballMatchId) {
        return playerAchievementRepository.findAllAccomplishedByMatchOrFootballMatch(
                appTeamId,
                matchId,
                footballMatchId
        ).stream().map(mapper::toDTO).toList();
    }
}
