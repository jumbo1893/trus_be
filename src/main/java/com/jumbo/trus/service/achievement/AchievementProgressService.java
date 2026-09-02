package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.achievement.AchievementProgressNotificationEntity;
import com.jumbo.trus.entity.achievement.PlayerAchievementEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.achievement.AchievementProgressNotificationRepository;
import com.jumbo.trus.repository.achievement.AchievementProgressQueryRepository;
import com.jumbo.trus.repository.achievement.AchievementProgressQueryRepository.DrinkerProgressProjection;
import com.jumbo.trus.repository.achievement.AchievementProgressQueryRepository.PlayerDrinkTotalsProjection;
import com.jumbo.trus.repository.achievement.AchievementProgressQueryRepository.PlayerMetricProjection;
import com.jumbo.trus.repository.achievement.AchievementProgressQueryRepository.ScorerProgressProjection;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.notification.push.maker.AchievementProgressNotificationMaker;
import com.jumbo.trus.service.outbox.AchievementEventBatch;
import com.jumbo.trus.service.outbox.AchievementPlayerWork;
import com.jumbo.trus.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.jumbo.trus.service.achievement.AchievementCodes.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementProgressService {

    private static final String ALL_CONTEXT = "ALL";

    private static final List<CountMilestone> DRINK_MILESTONES = List.of(
            new CountMilestone(JEDNOU_SE_ZACIT_MUSI, 1, ProgressUnit.BEER),
            new CountMilestone(KDYZ_ONO_TO_CHUTNA, 50, ProgressUnit.BEER),
            new CountMilestone(SOUDEK, 100, ProgressUnit.BEER),
            new CountMilestone(CISTERNA, 500, ProgressUnit.BEER),
            new CountMilestone(PRITVRDIME, 1, ProgressUnit.LIQUOR),
            new CountMilestone(RUMOVY_NADENIK, 20, ProgressUnit.LIQUOR),
            new CountMilestone(ACHIEVEMENT_MILANA_CURDY, 50, ProgressUnit.LIQUOR)
    );

    private static final List<CountMilestone> STEP_MILESTONES = List.of(
            new CountMilestone(OKOLO_HRADCE, 65_000, ProgressUnit.STEP),
            new CountMilestone(PRAZAK, 160_000, ProgressUnit.STEP),
            new CountMilestone(OD_SEVERU_K_JIHU, 341_000, ProgressUnit.STEP),
            new CountMilestone(OD_VYCHODU_NA_ZAPAD, 612_000, ProgressUnit.STEP),
            new CountMilestone(VSECHNY_CESTY_VEDOU_DO_RIMA, 1_600_000, ProgressUnit.STEP),
            new CountMilestone(EVROPSKY_POCHUZKAR, 7_200_000, ProgressUnit.STEP),
            new CountMilestone(CESTA_KOLEM_SVETA, 51_380_000, ProgressUnit.STEP)
    );

    private static final List<CountMilestone> FAN_ATTENDANCE_MILESTONES = List.of(
            new CountMilestone(PERMICE_NA_TRUS, 10, ProgressUnit.ATTENDANCE),
            new CountMilestone(ULTRUS, 30, ProgressUnit.ATTENDANCE)
    );

    private static final List<String> LATE_ARRIVAL_FINE_NAMES = List.of(
            "Pozdní příchod do začátku",
            "Pozdní příchod po 10. minutě",
            "Pozdní příchod po začátku",
            "Nepříchod"
    );

    private final AppTeamService appTeamService;
    private final PlayerService playerService;
    private final MatchRepository matchRepository;
    private final PlayerAchievementRepository playerAchievementRepository;
    private final AchievementProgressQueryRepository progressQueryRepository;
    private final AchievementProgressNotificationRepository progressNotificationRepository;
    private final StepAchievementCalculator stepAchievementCalculator;
    private final AchievementProgressNotificationMaker progressNotificationMaker;

    @Transactional
    public void evaluateAndNotify(AchievementEventBatch batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }

        List<ProgressCandidate> candidates = new ArrayList<>();
        stepAchievementCalculator.beginCalculationBatch();
        try {
            batch.workByTeamAndPlayer().forEach((appTeamId, workByPlayer) ->
                    evaluateTeam(appTeamId, workByPlayer, candidates)
            );
        } finally {
            stepAchievementCalculator.endCalculationBatch();
        }

        List<AchievementProgressMessage> messages = reserveNewNotifications(candidates);
        progressNotificationMaker.sendNotifications(messages);
    }

    private void evaluateTeam(
            Long appTeamId,
            Map<Long, AchievementPlayerWork> workByPlayer,
            List<ProgressCandidate> candidates
    ) {
        if (workByPlayer.isEmpty()) {
            return;
        }

        AppTeamEntity appTeam = appTeamService.findAppTeamByIdOrThrow(appTeamId);
        List<PlayerDTO> players = playerService.getAllByIds(workByPlayer.keySet(), appTeamId);
        if (players.isEmpty()) {
            return;
        }

        List<Long> playerIds = players.stream().map(PlayerDTO::getId).toList();
        Map<Long, Map<String, PlayerAchievementEntity>> achievementsByPlayer =
                loadAchievementsByPlayer(playerIds);
        Map<Long, Long> seasonByMatch = loadSeasonByMatch(workByPlayer, appTeamId);

        Set<Long> beerPlayerIds = playersWithChange(players, workByPlayer, OutboxAggregateType.BEER);
        Map<Long, PlayerDrinkTotalsProjection> drinksByPlayer = beerPlayerIds.isEmpty()
                ? Map.of()
                : progressQueryRepository.findDrinkTotals(beerPlayerIds, appTeamId).stream()
                .collect(Collectors.toMap(PlayerDrinkTotalsProjection::getPlayerId, Function.identity()));

        Set<Long> attendancePlayerIds = players.stream()
                .filter(PlayerDTO::isFan)
                .map(PlayerDTO::getId)
                .filter(playerId -> hasAnyChange(
                        workByPlayer.get(playerId),
                        OutboxAggregateType.MATCH,
                        OutboxAggregateType.PLAYER
                ))
                .collect(Collectors.toSet());
        Map<Long, Long> attendanceByPlayer = attendancePlayerIds.isEmpty()
                ? Map.of()
                : progressQueryRepository.findFanAttendanceTotals(attendancePlayerIds, appTeamId).stream()
                .collect(Collectors.toMap(
                        PlayerMetricProjection::getPlayerId,
                        projection -> safeLong(projection.getMetricValue())
                ));

        for (PlayerDTO player : players) {
            AchievementPlayerWork work = workByPlayer.get(player.getId());
            Map<String, PlayerAchievementEntity> playerAchievements =
                    achievementsByPlayer.getOrDefault(player.getId(), Map.of());
            Set<Long> changedSeasonIds = changedSeasonIds(work, seasonByMatch);

            if (beerPlayerIds.contains(player.getId())) {
                PlayerDrinkTotalsProjection totals = drinksByPlayer.get(player.getId());
                addDrinkMilestones(
                        player,
                        playerAchievements,
                        totals == null ? 0 : safeLong(totals.getBeerCount()),
                        totals == null ? 0 : safeLong(totals.getLiquorCount()),
                        candidates
                );
                addSeasonBeerMilestone(
                        player,
                        appTeam,
                        playerAchievements,
                        changedSeasonIds,
                        candidates
                );
            }

            if (hasAnyChange(work, OutboxAggregateType.BEER, OutboxAggregateType.MATCH)) {
                addSeasonDrinkerProgress(
                        player,
                        appTeam,
                        playerAchievements,
                        changedSeasonIds,
                        candidates
                );
            }

            if (hasAnyChange(work, OutboxAggregateType.STEP)) {
                addStepMilestones(player, appTeam, playerAchievements, candidates);
            }

            if (attendancePlayerIds.contains(player.getId())) {
                addFanAttendanceMilestones(
                        player,
                        playerAchievements,
                        attendanceByPlayer.getOrDefault(player.getId(), 0L),
                        candidates
                );
            }

            if (hasAnyChange(work, OutboxAggregateType.FINE, OutboxAggregateType.RECEIVED_FINE)) {
                addFineMilestones(
                        player,
                        appTeam,
                        playerAchievements,
                        changedSeasonIds,
                        candidates
                );
            }

            if (hasAnyChange(work, OutboxAggregateType.GOAL)) {
                addSeasonScorerProgress(
                        player,
                        appTeam,
                        playerAchievements,
                        changedSeasonIds,
                        candidates
                );
            }
        }
    }

    private Map<Long, Map<String, PlayerAchievementEntity>> loadAchievementsByPlayer(
            List<Long> playerIds
    ) {
        return playerAchievementRepository.findAllByPlayerIdIn(playerIds).stream()
                .filter(playerAchievement -> playerAchievement.getAchievement() != null)
                .collect(Collectors.groupingBy(
                        playerAchievement -> playerAchievement.getPlayer().getId(),
                        Collectors.toMap(
                                playerAchievement -> playerAchievement.getAchievement().getCode(),
                                Function.identity(),
                                (first, ignored) -> first
                        )
                ));
    }

    private Map<Long, Long> loadSeasonByMatch(
            Map<Long, AchievementPlayerWork> workByPlayer,
            Long appTeamId
    ) {
        Set<Long> matchIds = workByPlayer.values().stream()
                .map(AchievementPlayerWork::changesByMatch)
                .map(Map::keySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        if (matchIds.isEmpty()) {
            return Map.of();
        }

        return matchRepository.findAllById(matchIds).stream()
                .filter(match -> match.getAppTeam() != null)
                .filter(match -> Objects.equals(match.getAppTeam().getId(), appTeamId))
                .filter(match -> match.getSeason() != null)
                .collect(Collectors.toMap(
                        MatchEntity::getId,
                        match -> match.getSeason().getId()
                ));
    }

    private Set<Long> playersWithChange(
            List<PlayerDTO> players,
            Map<Long, AchievementPlayerWork> workByPlayer,
            OutboxAggregateType type
    ) {
        return players.stream()
                .map(PlayerDTO::getId)
                .filter(playerId -> hasAnyChange(workByPlayer.get(playerId), type))
                .collect(Collectors.toSet());
    }

    private Set<Long> changedSeasonIds(
            AchievementPlayerWork work,
            Map<Long, Long> seasonByMatch
    ) {
        if (work == null) {
            return Set.of();
        }
        return work.changesByMatch().keySet().stream()
                .map(seasonByMatch::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean hasAnyChange(
            AchievementPlayerWork work,
            OutboxAggregateType... expectedTypes
    ) {
        if (work == null) {
            return false;
        }
        Set<OutboxAggregateType> expected = EnumSet.noneOf(OutboxAggregateType.class);
        expected.addAll(Arrays.asList(expectedTypes));
        if (work.unscopedChanges().stream().anyMatch(expected::contains)) {
            return true;
        }
        return work.changesByMatch().values().stream()
                .flatMap(Collection::stream)
                .anyMatch(expected::contains);
    }

    private void addDrinkMilestones(
            PlayerDTO player,
            Map<String, PlayerAchievementEntity> achievements,
            long beerCount,
            long liquorCount,
            List<ProgressCandidate> candidates
    ) {
        for (CountMilestone milestone : DRINK_MILESTONES) {
            long current = milestone.unit() == ProgressUnit.BEER ? beerCount : liquorCount;
            addCountCandidate(
                    eligibleAchievement(player, achievements, milestone.achievementCode()),
                    ALL_CONTEXT,
                    milestone.target() - current,
                    drinkProximityLevel(milestone.target() - current),
                    milestone.unit(),
                    candidates
            );
        }
    }

    private void addSeasonBeerMilestone(
            PlayerDTO player,
            AppTeamEntity appTeam,
            Map<String, PlayerAchievementEntity> achievements,
            Set<Long> seasonIds,
            List<ProgressCandidate> candidates
    ) {
        PlayerAchievementEntity achievement = eligibleAchievement(player, achievements, STENE);
        if (achievement == null) {
            return;
        }
        for (Long seasonId : seasonIds) {
            long beers = safeLong(progressQueryRepository.sumBeersInSeason(
                    player.getId(), appTeam.getId(), seasonId));
            long remaining = 60 - beers;
            addCountCandidate(
                    achievement,
                    seasonContext(seasonId),
                    remaining,
                    drinkProximityLevel(remaining),
                    ProgressUnit.BEER,
                    candidates
            );
        }
    }

    private void addStepMilestones(
            PlayerDTO player,
            AppTeamEntity appTeam,
            Map<String, PlayerAchievementEntity> achievements,
            List<ProgressCandidate> candidates
    ) {
        long steps = stepAchievementCalculator.totalSteps(player.getId(), appTeam.getId());
        for (CountMilestone milestone : STEP_MILESTONES) {
            long remaining = milestone.target() - steps;
            addCountCandidate(
                    eligibleAchievement(player, achievements, milestone.achievementCode()),
                    ALL_CONTEXT,
                    remaining,
                    stepProximityLevel(remaining),
                    ProgressUnit.STEP,
                    candidates
            );
        }
    }

    private void addFanAttendanceMilestones(
            PlayerDTO player,
            Map<String, PlayerAchievementEntity> achievements,
            long attendanceCount,
            List<ProgressCandidate> candidates
    ) {
        for (CountMilestone milestone : FAN_ATTENDANCE_MILESTONES) {
            long remaining = milestone.target() - attendanceCount;
            addCountCandidate(
                    eligibleAchievement(player, achievements, milestone.achievementCode()),
                    ALL_CONTEXT,
                    remaining,
                    remaining == 1 ? 1L : null,
                    ProgressUnit.ATTENDANCE,
                    candidates
            );
        }
    }

    private void addFineMilestones(
            PlayerDTO player,
            AppTeamEntity appTeam,
            Map<String, PlayerAchievementEntity> achievements,
            Set<Long> seasonIds,
            List<ProgressCandidate> candidates
    ) {
        addGlobalFineMilestone(
                player,
                appTeam,
                eligibleAchievement(player, achievements, ROSS_GELLER),
                List.of("Svatba"),
                3,
                "chybí 1 pokuta za svatbu",
                candidates
        );
        addGlobalFineMilestone(
                player,
                appTeam,
                eligibleAchievement(player, achievements, AMERICKY_FOTBALISTA),
                List.of("Překop"),
                10,
                "chybí 1 pokuta za překop",
                candidates
        );

        for (Long seasonId : seasonIds) {
            addSeasonFineMilestone(
                    player,
                    appTeam,
                    eligibleAchievement(player, achievements, POROUCHANY_BUDIK),
                    seasonId,
                    LATE_ARRIVAL_FINE_NAMES,
                    3,
                    "chybí 1 pozdní příchod",
                    candidates
            );
            addSeasonFineMilestone(
                    player,
                    appTeam,
                    eligibleAchievement(player, achievements, MEDMRDKA),
                    seasonId,
                    List.of("Zmínka v tisku"),
                    2,
                    "chybí 1 zmínka v tisku",
                    candidates
            );
        }
    }

    private void addGlobalFineMilestone(
            PlayerDTO player,
            AppTeamEntity appTeam,
            PlayerAchievementEntity achievement,
            List<String> fineNames,
            long target,
            String missingText,
            List<ProgressCandidate> candidates
    ) {
        if (achievement == null) {
            return;
        }
        long count = safeLong(progressQueryRepository.sumFineCount(
                player.getId(), appTeam.getId(), fineNames));
        addFineCandidate(achievement, ALL_CONTEXT, target - count, missingText, candidates);
    }

    private void addSeasonFineMilestone(
            PlayerDTO player,
            AppTeamEntity appTeam,
            PlayerAchievementEntity achievement,
            Long seasonId,
            List<String> fineNames,
            long target,
            String missingText,
            List<ProgressCandidate> candidates
    ) {
        if (achievement == null) {
            return;
        }
        long count = safeLong(progressQueryRepository.sumFineCountInSeason(
                player.getId(), appTeam.getId(), seasonId, fineNames));
        addFineCandidate(
                achievement,
                seasonContext(seasonId),
                target - count,
                missingText,
                candidates
        );
    }

    private void addFineCandidate(
            PlayerAchievementEntity achievement,
            String context,
            long remaining,
            String missingText,
            List<ProgressCandidate> candidates
    ) {
        if (achievement != null && remaining == 1) {
            candidates.add(new ProgressCandidate(achievement, context, 1, missingText));
        }
    }

    private void addSeasonScorerProgress(
            PlayerDTO player,
            AppTeamEntity appTeam,
            Map<String, PlayerAchievementEntity> achievements,
            Set<Long> seasonIds,
            List<ProgressCandidate> candidates
    ) {
        PlayerAchievementEntity achievement = eligibleAchievement(player, achievements, STRELEC);
        if (achievement == null) {
            return;
        }

        for (Long seasonId : seasonIds) {
            ScorerProgressProjection progress = progressQueryRepository.findScorerProgress(
                    player.getId(), appTeam.getId(), seasonId);
            if (!isOneGoalFromLead(progress)) {
                continue;
            }
            candidates.add(new ProgressCandidate(
                    achievement,
                    seasonContext(seasonId),
                    1,
                    "chybí 1 gól"
            ));
        }
    }

    private void addSeasonDrinkerProgress(
            PlayerDTO player,
            AppTeamEntity appTeam,
            Map<String, PlayerAchievementEntity> achievements,
            Set<Long> seasonIds,
            List<ProgressCandidate> candidates
    ) {
        PlayerAchievementEntity achievement = eligibleAchievement(
                player, achievements, KDYZ_LEJU_TAK_PORADNE);
        if (achievement == null) {
            return;
        }

        for (Long seasonId : seasonIds) {
            DrinkerProgressProjection progress = progressQueryRepository.findDrinkerProgress(
                    player.getId(), appTeam.getId(), seasonId);
            if (!isOneDrinkFromLead(progress)) {
                continue;
            }
            candidates.add(new ProgressCandidate(
                    achievement,
                    seasonContext(seasonId),
                    1,
                    "chybí 1 pivo nebo panák"
            ));
        }
    }

    static boolean isOneGoalFromLead(ScorerProgressProjection progress) {
        if (progress == null) {
            return false;
        }
        long playerGoals = safeLong(progress.getPlayerGoals());
        long playerAssists = safeLong(progress.getPlayerAssists());
        long leaderGoals = safeLong(progress.getLeaderGoals());
        long leaderAssists = safeLong(progress.getLeaderAssists());

        boolean alreadyLeading = playerGoals > leaderGoals
                || (playerGoals == leaderGoals && playerAssists >= leaderAssists);
        long goalsAfterOne = playerGoals + 1;
        boolean leadingAfterOne = goalsAfterOne > leaderGoals
                || (goalsAfterOne == leaderGoals && playerAssists >= leaderAssists);
        return leaderGoals > 0 && !alreadyLeading && leadingAfterOne;
    }

    static boolean isOneDrinkFromLead(DrinkerProgressProjection progress) {
        if (progress == null) {
            return false;
        }
        long playerDrinks = safeLong(progress.getPlayerDrinks());
        long playerMatches = safeLong(progress.getPlayerMatches());
        long leaderDrinks = safeLong(progress.getLeaderDrinks());
        long leaderMatches = safeLong(progress.getLeaderMatches());
        if (playerMatches <= 0 || leaderMatches <= 0 || leaderDrinks <= 0) {
            return false;
        }

        long currentLeft = playerDrinks * leaderMatches;
        long afterOneLeft = (playerDrinks + 1) * leaderMatches;
        long leaderRight = leaderDrinks * playerMatches;
        return currentLeft < leaderRight && afterOneLeft >= leaderRight;
    }

    private PlayerAchievementEntity eligibleAchievement(
            PlayerDTO player,
            Map<String, PlayerAchievementEntity> achievements,
            String achievementCode
    ) {
        PlayerAchievementEntity achievement = achievements.get(achievementCode);
        if (achievement == null || Boolean.TRUE.equals(achievement.getAccomplished())) {
            return null;
        }
        if (Boolean.TRUE.equals(achievement.getAchievement().getOnlyForPlayers()) && player.isFan()) {
            return null;
        }
        return achievement;
    }

    private void addCountCandidate(
            PlayerAchievementEntity achievement,
            String context,
            long remaining,
            Long proximityLevel,
            ProgressUnit unit,
            List<ProgressCandidate> candidates
    ) {
        if (achievement == null || remaining <= 0 || proximityLevel == null) {
            return;
        }
        candidates.add(new ProgressCandidate(
                achievement,
                context,
                proximityLevel,
                "chybí " + formatCount(remaining, unit)
        ));
    }

    private List<AchievementProgressMessage> reserveNewNotifications(
            List<ProgressCandidate> candidates
    ) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<Long> playerAchievementIds = candidates.stream()
                .map(candidate -> candidate.playerAchievement().getId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (playerAchievementIds.isEmpty()) {
            return List.of();
        }

        Map<NotificationScope, Long> closestSentThreshold = new HashMap<>();
        for (AchievementProgressNotificationEntity notification
                : progressNotificationRepository.findAllByPlayerAchievementIdIn(playerAchievementIds)) {
            NotificationScope scope = new NotificationScope(
                    notification.getPlayerAchievement().getId(),
                    notification.getContextKey()
            );
            closestSentThreshold.merge(
                    scope,
                    notification.getProximityThreshold(),
                    Math::min
            );
        }

        List<AchievementProgressNotificationEntity> notificationsToSave = new ArrayList<>();
        List<AchievementProgressMessage> messages = new ArrayList<>();
        for (ProgressCandidate candidate : candidates) {
            PlayerAchievementEntity playerAchievement = candidate.playerAchievement();
            NotificationScope scope = new NotificationScope(
                    playerAchievement.getId(), candidate.contextKey());
            Long closestThreshold = closestSentThreshold.get(scope);
            if (closestThreshold != null && closestThreshold <= candidate.proximityThreshold()) {
                continue;
            }

            notificationsToSave.add(new AchievementProgressNotificationEntity(
                    playerAchievement,
                    candidate.contextKey(),
                    candidate.proximityThreshold()
            ));
            messages.add(new AchievementProgressMessage(
                    playerAchievement.getPlayer().getId(),
                    playerAchievement.getAchievement().getName(),
                    candidate.missingText()
            ));
            closestSentThreshold.put(scope, candidate.proximityThreshold());
        }

        if (!notificationsToSave.isEmpty()) {
            progressNotificationRepository.saveAll(notificationsToSave);
            log.info("Reserved {} achievement progress notifications", notificationsToSave.size());
        }
        return messages;
    }

    static Long drinkProximityLevel(long remaining) {
        if (remaining <= 0) {
            return null;
        }
        if (remaining <= 1) {
            return 1L;
        }
        return remaining <= 5 ? 5L : null;
    }

    static Long stepProximityLevel(long remaining) {
        if (remaining <= 0) {
            return null;
        }
        if (remaining <= 1_000) {
            return 1_000L;
        }
        return remaining <= 10_000 ? 10_000L : null;
    }

    private String formatCount(long count, ProgressUnit unit) {
        return count + " " + switch (unit) {
            case BEER -> czechPlural(count, "pivo", "piva", "piv");
            case LIQUOR -> czechPlural(count, "panák", "panáky", "panáků");
            case STEP -> czechPlural(count, "krok", "kroky", "kroků");
            case ATTENDANCE -> czechPlural(count, "účast", "účasti", "účastí");
        };
    }

    private String czechPlural(long count, String one, String few, String many) {
        if (count == 1) {
            return one;
        }
        if (count >= 2 && count <= 4) {
            return few;
        }
        return many;
    }

    private static long safeLong(Long value) {
        return value == null ? 0 : value;
    }

    private String seasonContext(Long seasonId) {
        return "SEASON:" + seasonId;
    }

    private enum ProgressUnit {
        BEER,
        LIQUOR,
        STEP,
        ATTENDANCE
    }

    private record CountMilestone(
            String achievementCode,
            long target,
            ProgressUnit unit
    ) {
    }

    private record ProgressCandidate(
            PlayerAchievementEntity playerAchievement,
            String contextKey,
            long proximityThreshold,
            String missingText
    ) {
    }

    private record NotificationScope(
            Long playerAchievementId,
            String contextKey
    ) {
    }
}
