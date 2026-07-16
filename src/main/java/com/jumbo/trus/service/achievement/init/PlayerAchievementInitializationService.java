package com.jumbo.trus.service.achievement.init;

import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.achievement.AchievementEntity;
import com.jumbo.trus.entity.achievement.PlayerAchievementEntity;
import com.jumbo.trus.repository.PlayerRepository;
import com.jumbo.trus.repository.achievement.AchievementRepository;
import com.jumbo.trus.repository.achievement.PlayerAchievementKeyProjection;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.achievement.helper.AchievementEligibilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerAchievementInitializationService {

    private final PlayerRepository playerRepository;
    private final AchievementRepository achievementRepository;
    private final PlayerAchievementRepository playerAchievementRepository;
    private final AchievementEligibilityService achievementEligibilityService;

    /**
     * Doplní všechny chybějící povolené kombinace hráč × achievement.
     */
    @Transactional
    public int initializeAllMissingPlayerAchievements() {
        List<PlayerEntity> players = playerRepository.findAll();
        List<AchievementEntity> achievements =
                achievementRepository.findAll();

        if (players.isEmpty() || achievements.isEmpty()) {
            return 0;
        }

        Set<PlayerAchievementKey> existingKeys = loadExistingKeys();

        Date createdAt = new Date();

        List<PlayerAchievementEntity> missingAchievements =
                new ArrayList<>();

        for (PlayerEntity player : players) {
            for (AchievementEntity achievement : achievements) {

                if (!achievementEligibilityService.canHaveAchievement(
                        player,
                        achievement
                )) {
                    continue;
                }

                PlayerAchievementKey key = new PlayerAchievementKey(
                        player.getId(),
                        achievement.getId()
                );

                if (existingKeys.contains(key)) {
                    continue;
                }

                missingAchievements.add(
                        new PlayerAchievementEntity(
                                achievement,
                                player,
                                false,
                                createdAt
                        )
                );

                // Chrání i před duplicitou uvnitř stejného běhu.
                existingKeys.add(key);
            }
        }

        if (missingAchievements.isEmpty()) {
            log.info("No missing player achievements found");
            return 0;
        }

        playerAchievementRepository.saveAll(missingAchievements);

        log.info(
                "Initialized {} missing player achievements",
                missingAchievements.size()
        );

        return missingAchievements.size();
    }

    /**
     * Doplní nový achievement všem hráčům/fanouškům,
     * kteří jej podle pravidel mohou mít.
     */
    @Transactional
    public int initializeAchievementForAllPlayers(
            Long achievementId
    ) {
        AchievementEntity achievement =
                achievementRepository.findById(achievementId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Achievement s ID "
                                        + achievementId
                                        + " neexistuje"
                        ));

        Set<Long> initializedPlayerIds =
                new HashSet<>(
                        playerAchievementRepository
                                .findPlayerIdsByAchievementId(
                                        achievementId
                                )
                );

        Date createdAt = new Date();

        List<PlayerAchievementEntity> missingAchievements =
                playerRepository.findAll()
                        .stream()
                        .filter(player ->
                                achievementEligibilityService
                                        .canHaveAchievement(
                                                player,
                                                achievement
                                        )
                        )
                        .filter(player ->
                                !initializedPlayerIds.contains(
                                        player.getId()
                                )
                        )
                        .map(player ->
                                new PlayerAchievementEntity(
                                        achievement,
                                        player,
                                        false,
                                        createdAt
                                )
                        )
                        .toList();

        if (missingAchievements.isEmpty()) {
            return 0;
        }

        playerAchievementRepository.saveAll(missingAchievements);

        log.info(
                "Initialized achievement {} for {} players",
                achievement.getCode(),
                missingAchievements.size()
        );

        return missingAchievements.size();
    }

    /**
     * Doplní konkrétnímu hráči/fanouškovi všechny achievementy,
     * které podle pravidel může mít.
     */
    @Transactional
    public int initializeAchievementsForPlayer(Long playerId) {
        PlayerEntity player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Hráč s ID " + playerId + " neexistuje"
                ));

        Set<Long> initializedAchievementIds =
                new HashSet<>(
                        playerAchievementRepository
                                .findAchievementIdsByPlayerId(playerId)
                );

        Date createdAt = new Date();

        List<PlayerAchievementEntity> missingAchievements =
                achievementRepository.findAll()
                        .stream()
                        .filter(achievement ->
                                achievementEligibilityService
                                        .canHaveAchievement(
                                                player,
                                                achievement
                                        )
                        )
                        .filter(achievement ->
                                !initializedAchievementIds.contains(
                                        achievement.getId()
                                )
                        )
                        .map(achievement ->
                                new PlayerAchievementEntity(
                                        achievement,
                                        player,
                                        false,
                                        createdAt
                                )
                        )
                        .toList();

        if (missingAchievements.isEmpty()) {
            return 0;
        }

        playerAchievementRepository.saveAll(missingAchievements);

        log.info(
                "Initialized {} achievements for playerId={}",
                missingAchievements.size(),
                playerId
        );

        return missingAchievements.size();
    }

    private Set<PlayerAchievementKey> loadExistingKeys() {
        Set<PlayerAchievementKey> keys = new HashSet<>();

        for (PlayerAchievementKeyProjection projection :
                playerAchievementRepository
                        .findAllPlayerAchievementKeys()) {

            keys.add(
                    new PlayerAchievementKey(
                            projection.getPlayerId(),
                            projection.getAchievementId()
                    )
            );
        }

        return keys;
    }

    private record PlayerAchievementKey(
            Long playerId,
            Long achievementId
    ) {
    }
}