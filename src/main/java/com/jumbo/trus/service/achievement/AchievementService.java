package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.dto.achievement.AchievementDetail;
import com.jumbo.trus.dto.achievement.AchievementPlayerDetail;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.lock.LockManager;
import com.jumbo.trus.mapper.achievement.AchievementMapper;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.achievement.AchievementRepository;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.achievement.helper.AchievementRecalculationContext;
import com.jumbo.trus.service.achievement.helper.AchievementType;
import com.jumbo.trus.service.order.OrderAchievementBySuccessRate;
import com.jumbo.trus.service.player.PlayerService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;

import java.text.Collator;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

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
    private final AchievementRarityService achievementRarityService;
    private final MatchRepository matchRepository;

    @Transactional
    public void updateAllPlayerAchievements(AppTeamEntity appTeam, AchievementType achievementType) {
        updatePlayerAchievements(appTeam, achievementType, AchievementRecalculationContext.full(null));
    }

    @Transactional
    public void updatePlayerAchievements(
            AppTeamEntity appTeam,
            AchievementType achievementType,
            AchievementRecalculationContext context
    ) {
        if (appTeam == null) {
            throw new IllegalStateException("AppTeamId nebyl nastaven!");
        }

        AchievementRecalculationContext safeContext = context == null
                ? AchievementRecalculationContext.full(null)
                : context;

        executeWithAchievementLock(appTeam, () -> {
            if (safeContext.shouldUseFullRecalculation()) {
                log.info("Starting full achievement calculation. appTeamId={}, type={}, context={}",
                        appTeam.getId(), achievementType, safeContext);
                List<PlayerDTO> players = loadAllPlayers(appTeam);
                achievementCalculator.calculateAllAchievements(players, appTeam, achievementType);
                return;
            }

            AchievementRecalculationContext enrichedContext = enrichContext(appTeam, safeContext);
            List<PlayerDTO> players = loadPlayersForContext(appTeam, enrichedContext);

            if (players.isEmpty()) {
                log.info("Skipping achievement calculation because no affected players were found. appTeamId={}, type={}, context={}",
                        appTeam.getId(), achievementType, enrichedContext);
                return;
            }

            log.info("Starting scoped achievement calculation. appTeamId={}, type={}, players={}, context={}",
                    appTeam.getId(), achievementType, players.size(), enrichedContext);
            achievementCalculator.calculateAchievementsByContext(players, appTeam, achievementType, enrichedContext);
        });
    }

    private void executeWithAchievementLock(AppTeamEntity appTeam, Runnable runnable) {
        ReentrantLock lock = lockManager.getLock(appTeam.getId());
        if (lock.tryLock()) {
            try {
                runnable.run();
            } finally {
                lock.unlock();
            }
            return;
        }

        lock.lock();
        try {
            log.info("Starting queued achievement calculation. appTeamId={}", appTeam.getId());
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    private List<PlayerDTO> loadAllPlayers(AppTeamEntity appTeam) {
        long playersLoadStart = System.nanoTime();
        List<PlayerDTO> players = playerService.getAll(appTeam.getId());
        log.info("Loaded all players for achievement calculation in {} ms. appTeamId={}, players={}",
                (System.nanoTime() - playersLoadStart) / 1_000_000, appTeam.getId(), players.size());
        return players;
    }

    private AchievementRecalculationContext enrichContext(AppTeamEntity appTeam, AchievementRecalculationContext context) {
        Set<Long> affectedPlayerIds = new HashSet<>(context.affectedPlayerIds());
        Set<Long> changedSeasonIds = new HashSet<>(context.changedSeasonIds());

        if (context.hasChangedMatches()) {
            affectedPlayerIds.addAll(matchRepository.findAffectedPlayerIdsByMatchIds(context.changedMatchIds()));
            affectedPlayerIds.addAll(playerAchievementRepository.findPlayerIdsWithAccomplishedAchievementsOnMatchIds(
                    appTeam.getId(),
                    context.changedMatchIds()
            ));
            changedSeasonIds.addAll(matchRepository.findSeasonIdsByMatchIds(context.changedMatchIds()));
        }

        return AchievementRecalculationContext.scoped(
                context.changedMatchIds(),
                affectedPlayerIds,
                changedSeasonIds,
                context.changedDependencies()
        );
    }

    private List<PlayerDTO> loadPlayersForContext(AppTeamEntity appTeam, AchievementRecalculationContext context) {
        if (!context.hasAffectedPlayers()) {
            return loadAllPlayers(appTeam);
        }

        long playersLoadStart = System.nanoTime();
        List<PlayerDTO> players = context.affectedPlayerIds().stream()
                .map(this::safeGetPlayer)
                .filter(player -> player != null && player.getId() != 0)
                .toList();
        log.info("Loaded scoped players for achievement calculation in {} ms. appTeamId={}, players={}, requestedPlayerIds={}",
                (System.nanoTime() - playersLoadStart) / 1_000_000, appTeam.getId(), players.size(), context.affectedPlayerIds().size());
        return players;
    }

    private PlayerDTO safeGetPlayer(Long playerId) {
        try {
            return playerService.getPlayer(playerId);
        } catch (RuntimeException exception) {
            log.warn("Skipping player during scoped achievement calculation because player could not be loaded. playerId={}",
                    playerId,
                    exception);
            return null;
        }
    }


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
        List<Long> playerIdList = achievementDetailService.getPlayerIdList(appTeamId);

        List<AchievementDTO> achievements = achievementRepository.findAll()
                .stream()
                .map(achievementMapper::toDTO)
                .toList();

        achievementRarityService.enrichWithRarity(achievements, appTeamId);

        return achievements.stream()
                .map(achievement -> achievementDetailService.returnAchievementDetail(
                        achievement,
                        playerIdList,
                        true
                ))
                .sorted(new OrderAchievementBySuccessRate())
                .toList();
    }

    public AchievementPlayerDetail getAchievementsForPlayer(Long playerId, Long appTeamId) {
        Collator czechCollator = Collator.getInstance(Locale.forLanguageTag("cs-CZ"));
        czechCollator.setStrength(Collator.PRIMARY);

        List<PlayerAchievementDTO> playerAchievementList = playerAchievementRepository.findAllByPlayerId(playerId)
                .stream()
                .map(playerAchievementMapper::toDTO)
                .sorted((a, b) -> {
                    String nameA = a.getAchievement() == null || a.getAchievement().getName() == null
                            ? ""
                            : a.getAchievement().getName();

                    String nameB = b.getAchievement() == null || b.getAchievement().getName() == null
                            ? ""
                            : b.getAchievement().getName();

                    return czechCollator.compare(nameA, nameB);
                })
                .toList();

        List<AchievementDTO> achievements = playerAchievementList.stream()
                .map(PlayerAchievementDTO::getAchievement)
                .filter(Objects::nonNull)
                .toList();

        achievementRarityService.enrichWithRarity(achievements, appTeamId);

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
}