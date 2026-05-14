package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.achievement.*;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.lock.LockManager;
import com.jumbo.trus.mapper.achievement.AchievementMapper;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.repository.achievement.AchievementRepository;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.achievement.helper.AchievementAccomplishedStats;
import com.jumbo.trus.service.achievement.helper.AchievementType;
import com.jumbo.trus.service.order.OrderAchievementBySuccessRate;
import com.jumbo.trus.service.player.PlayerService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementService {

    private final PlayerService playerService;
    private final AchievementMapper achievementMapper;
    private final AchievementRepository achievementRepository;
    private final PlayerAchievementRepository playerAchievementRepository;
    private final PlayerAchievementMapper playerAchievementMapper;
    private final AchievementCalculator achievementCalculator;
    private final LockManager lockManager;
    private final AchievementDetailService achievementDetailService;

    @Async
    @Transactional
    public void updateAllPlayerAchievements(AppTeamEntity appTeam, AchievementType achievementType) {
        if (appTeam == null) {
            throw new IllegalStateException("AppTeamId nebyl nastaven!");
        }
        ReentrantLock lock = lockManager.getLock(appTeam.getId());
        if (lock.tryLock()) { // Non-blocking pokus – pokud zamčené, počkáme (fair lock)
            try {
                log.debug("Začínám počítat pro appTeam {}!", appTeam.getId());
                achievementCalculator.calculateAllAchievements(playerService.getAll(appTeam.getId()), appTeam, achievementType);
            } finally {
                lock.unlock();
            }
        } else {
            // Pokud nelze získat lock ihned, zařaď do fronty nebo retry – ale pro jednoduchost: čekej blokujícím způsobem
            lock.lock(); // Blokující čekání
            try {
                achievementCalculator.calculateAllAchievements(playerService.getAll(appTeam.getId()), appTeam, achievementType);
            } finally {
                lock.unlock();
            }
        }
    }
        /*achievementCalculator.calculateAllAchievements(playerService.getAll(appTeam.getId()), appTeam, achievementType);
        log.debug("Vypočteno!");*/


    public void updatePlayerAchievements(Long playerId, AppTeamEntity appTeam) {
        List<PlayerDTO> playerDTOList = new ArrayList<>();
        playerDTOList.add(playerService.getPlayer(playerId));
        achievementCalculator.calculateAllAchievements(playerDTOList, appTeam, AchievementType.ALL);
    }

    public AchievementDetail getAchievementDetail(long playerAchievementId, AppTeamEntity appTeam) {
        return achievementDetailService.getAchievementDetail(playerAchievementId, appTeam);
    }

    public PlayerAchievementDTO editPlayerAchievement(Long oldAchievementId, PlayerAchievementDTO playerAchievementDTO, AppTeamEntity appTeam) throws NotFoundException {
        PlayerAchievementDTO oldAchievement = playerAchievementRepository.findById(oldAchievementId).map(playerAchievementMapper::toDTO).orElseThrow(() -> new EntityNotFoundException(String.valueOf(oldAchievementId)));
        achievementCalculator.updateExistingAchievement(oldAchievement, playerAchievementDTO, appTeam);
        return playerAchievementDTO;
    }


    public List<AchievementDetail> getAllDetailedAchievements(long appTeamId) {
        List<AchievementDetail> achievementDetailList = new ArrayList<>();

        List<Long> playerIdList = achievementDetailService.getPlayerIdList(appTeamId);

        List<AchievementDTO> achievements = achievementRepository.findAll()
                .stream()
                .map(achievementMapper::toDTO)
                .toList();

        for (AchievementDTO achievement : achievements) {
            achievementDetailList.add(
                    achievementDetailService.returnAchievementDetail(
                            achievement,
                            playerIdList,
                            true
                    )
            );
        }

        achievementDetailList.sort(new OrderAchievementBySuccessRate());

        return achievementDetailList;
    }

    public AchievementPlayerDetail getAchievementsForPlayer(Long playerId, Long appTeamId) {

        List<PlayerAchievementDTO> playerAchievementList = playerAchievementRepository.findAllByPlayerId(playerId)
                .stream()
                .map(playerAchievementMapper::toDTO)
                .toList();

        enrichAchievementsWithRarity(playerAchievementList, appTeamId);

        AchievementPlayerDetail achievementPlayerDetail = new AchievementPlayerDetail();

        for (PlayerAchievementDTO playerAchievementDTO : playerAchievementList) {
            if (Boolean.TRUE.equals(playerAchievementDTO.getAccomplished())) {
                achievementPlayerDetail.getAccomplishedPlayerAchievements().add(playerAchievementDTO);
            } else {
                achievementPlayerDetail.getNotAccomplishedPlayerAchievements().add(playerAchievementDTO);
            }
        }

        achievementPlayerDetail.setTotalCount(playerAchievementList.size());
        achievementPlayerDetail.setSuccessRate(
                calculateSuccessRate(
                        playerAchievementList.size(),
                        achievementPlayerDetail.getAccomplishedPlayerAchievements().size()
                )
        );

        return achievementPlayerDetail;
    }

    private float calculateSuccessRate(int totalNumber, int accomplishedNumber) {
        if (totalNumber == 0) {
            return 0F;
        }
        return (float) accomplishedNumber/totalNumber;
    }

    private AchievementRarity resolveRarity(float successRate) {

        if (successRate <= 0.10F) {
            return AchievementRarity.LEGENDARY;
        }

        if (successRate <= 0.30F) {
            return AchievementRarity.EPIC;
        }

        if (successRate <= 0.60F) {
            return AchievementRarity.RARE;
        }

        return AchievementRarity.COMMON;
    }

    private void enrichAchievementsWithRarity(
            List<PlayerAchievementDTO> playerAchievementList,
            Long appTeamId
    ) {
        List<PlayerDTO> teamMembers = playerService.getAll(appTeamId);

        long totalPlayersOnly = teamMembers.stream()
                .filter(player -> !Boolean.TRUE.equals(player.isFan()))
                .count();

        long totalPlayersAndFans = teamMembers.size();

        List<Object[]> accomplishedCounts =
                playerAchievementRepository.countAccomplishedStatsByAchievementForTeam(appTeamId);

        Map<Long, AchievementAccomplishedStats> accomplishedStatsByAchievementId =
                accomplishedCounts.stream()
                        .collect(Collectors.toMap(
                                row -> (Long) row[0],
                                row -> new AchievementAccomplishedStats(
                                        ((Number) row[1]).longValue(),
                                        ((Number) row[2]).longValue()
                                )
                        ));

        for (PlayerAchievementDTO playerAchievementDTO : playerAchievementList) {
            AchievementDTO achievement = playerAchievementDTO.getAchievement();

            if (achievement == null) {
                continue;
            }

            Long achievementId = achievement.getId();
            boolean onlyForPlayers = Boolean.TRUE.equals(achievement.isOnlyForPlayers());

            AchievementAccomplishedStats stats = accomplishedStatsByAchievementId.getOrDefault(
                    achievementId,
                    new AchievementAccomplishedStats(0L, 0L)
            );

            long totalRelevantPeople = onlyForPlayers
                    ? totalPlayersOnly
                    : totalPlayersAndFans;

            long accomplishedPeople = onlyForPlayers
                    ? stats.playersOnly()
                    : stats.playersAndFans();

            float achievementSuccessRate = totalRelevantPeople == 0
                    ? 0F
                    : (float) accomplishedPeople / totalRelevantPeople;

            AchievementRarity rarity = resolveRarity(
                    achievementSuccessRate
            );

            achievement.setTeamSuccessRate(achievementSuccessRate);
            achievement.setRarity(rarity);
        }
    }
}