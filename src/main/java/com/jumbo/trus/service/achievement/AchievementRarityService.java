package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.dto.achievement.AchievementRarity;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.achievement.helper.AchievementAccomplishedStats;
import com.jumbo.trus.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementRarityService {

    private final PlayerService playerService;
    private final PlayerAchievementRepository playerAchievementRepository;

    public void enrichWithRarity(List<AchievementDTO> achievements, Long appTeamId) {
        RarityContext context = createContext(appTeamId);

        for (AchievementDTO achievement : achievements) {
            enrichAchievement(achievement, context);
        }
    }

    private RarityContext createContext(Long appTeamId) {
        List<PlayerDTO> teamMembers = playerService.getAll(appTeamId);

        long totalPlayersOnly = teamMembers.stream()
                .filter(player -> !Boolean.TRUE.equals(player.isFan()))
                .count();

        long totalPlayersAndFans = teamMembers.size();

        Map<Long, AchievementAccomplishedStats> statsByAchievementId =
                playerAchievementRepository.countAccomplishedStatsByAchievementForTeam(appTeamId)
                        .stream()
                        .collect(Collectors.toMap(
                                row -> ((Number) row[0]).longValue(),
                                row -> new AchievementAccomplishedStats(
                                        ((Number) row[1]).longValue(),
                                        ((Number) row[2]).longValue()
                                )
                        ));

        return new RarityContext(
                totalPlayersOnly,
                totalPlayersAndFans,
                statsByAchievementId
        );
    }

    private void enrichAchievement(AchievementDTO achievement, RarityContext context) {
        if (achievement == null) {
            return;
        }

        boolean onlyForPlayers = Boolean.TRUE.equals(achievement.isOnlyForPlayers());

        AchievementAccomplishedStats stats = context.statsByAchievementId()
                .getOrDefault(
                        achievement.getId(),
                        new AchievementAccomplishedStats(0L, 0L)
                );

        long totalRelevantPeople = onlyForPlayers
                ? context.totalPlayersOnly()
                : context.totalPlayersAndFans();

        long accomplishedPeople = onlyForPlayers
                ? stats.playersOnly()
                : stats.playersAndFans();

        float successRate = totalRelevantPeople == 0
                ? 0F
                : (float) accomplishedPeople / totalRelevantPeople;

        achievement.setTeamSuccessRate(successRate);
        achievement.setRarity(resolveRarity(successRate));
    }

    private AchievementRarity resolveRarity(float successRate) {
        if (successRate <= 0.05F) {
            return AchievementRarity.LEGENDARY;
        }

        if (successRate <= 0.15F) {
            return AchievementRarity.EPIC;
        }

        if (successRate <= 0.45F) {
            return AchievementRarity.RARE;
        }

        return AchievementRarity.COMMON;
    }

    private record RarityContext(
            long totalPlayersOnly,
            long totalPlayersAndFans,
            Map<Long, AchievementAccomplishedStats> statsByAchievementId
    ) {
    }
}