package com.jumbo.trus.service.achievement;

import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.PlayerStepTotalProjection;
import com.jumbo.trus.repository.StepPeriodStatsProjection;
import com.jumbo.trus.repository.StepUpdateRepository;
import com.jumbo.trus.repository.footbar.FootbarSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Component
@RequiredArgsConstructor
public class StepAchievementCalculator {

    // Daily step reports currently contain no measured distance or stride length.
    // Keep the estimate in one place so it can later be replaced with a user-specific value.
    static final double METERS_PER_STEP = 0.78;
    static final double MINIMUM_MATCH_DISTANCE_METERS = 2_000.0;
    static final int MINIMUM_WALKERS = 3;

    private static final ZoneId PRAGUE_ZONE = ZoneId.of("Europe/Prague");
    private static final LocalDate STEP_HISTORY_START = LocalDate.of(1970, 1, 1);

    private final StepUpdateRepository stepUpdateRepository;
    private final FootbarSessionRepository footbarSessionRepository;
    private final MatchRepository matchRepository;
    private final ThreadLocal<Map<PlayerTeamKey, Long>> totalStepCache = new ThreadLocal<>();

    void beginCalculationBatch() {
        totalStepCache.set(new HashMap<>());
    }

    void endCalculationBatch() {
        totalStepCache.remove();
    }

    public long totalSteps(Long playerId, Long appTeamId) {
        Map<PlayerTeamKey, Long> cache = totalStepCache.get();
        if (cache == null) {
            return loadTotalSteps(playerId, appTeamId);
        }
        PlayerTeamKey key = new PlayerTeamKey(playerId, appTeamId);
        return cache.computeIfAbsent(key, ignored -> loadTotalSteps(playerId, appTeamId));
    }

    private long loadTotalSteps(Long playerId, Long appTeamId) {
        return stats(
                playerId,
                appTeamId,
                STEP_HISTORY_START,
                LocalDate.now(PRAGUE_ZONE)).stepCount();
    }

    public Optional<MilestoneResult> milestoneResult(
            Long playerId,
            Long appTeamId,
            long threshold
    ) {
        if (totalSteps(playerId, appTeamId) < threshold) {
            return Optional.empty();
        }
        return stepUpdateRepository.milestoneStats(playerId, appTeamId, threshold)
                .map(result -> {
                    long stepCount = safeLong(result.getStepCount());
                    long dayCount = safeLong(result.getDayCount());
                    long kilometers = Math.round(stepCount * METERS_PER_STEP / 1_000.0);
                    return new MilestoneResult(stepCount, dayCount, kilometers);
                });
    }

    public Optional<MatchResult> findStrengthSaving(Long playerId, Long appTeamId) {
        for (Long matchId : matchRepository.findCompletedMatchIds(appTeamId, new Date())) {
            Optional<MatchResult> result = calculateStrengthSaving(playerId, appTeamId, matchId);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    public Optional<MatchResult> calculateStrengthSaving(
            Long playerId,
            Long appTeamId,
            Long matchId) {
        MatchEntity match = completedMatch(matchId, appTeamId, false).orElse(null);
        if (match == null) {
            return Optional.empty();
        }

        double runningDistance = Optional.ofNullable(
                        footbarSessionRepository.findDistanceByPlayerAndMatch(
                                playerId, matchId, appTeamId))
                .orElse(0.0);
        if (runningDistance < MINIMUM_MATCH_DISTANCE_METERS) {
            return Optional.empty();
        }

        LocalDate matchDate = toPragueDate(match.getDate());
        PeriodStats walking = stats(
                playerId,
                appTeamId,
                matchDate.minusDays(2),
                matchDate.minusDays(1));
        if (walking.dayCount() < 2) {
            return Optional.empty();
        }

        double walkingDistance = walking.stepCount() * METERS_PER_STEP;
        if (runningDistance <= walkingDistance) {
            return Optional.empty();
        }

        return Optional.of(new MatchResult(
                matchId,
                "V zápase uběhl " + formatKilometers(runningDistance)
                        + " km. Za předchozí dva dny ušel " + walking.stepCount()
                        + " kroků, tedy přibližně " + formatKilometers(walkingDistance) + " km."));
    }

    public Optional<MatchResult> findWalker(Long playerId, Long appTeamId) {
        // The leaderboard uses whole calendar days. Waiting until the next day prevents
        // awarding a winner from an incomplete match-day total.
        Date startOfToday = Date.from(LocalDate.now(PRAGUE_ZONE)
                .atStartOfDay(PRAGUE_ZONE)
                .toInstant());
        for (Long matchId : matchRepository.findCompletedMatchIds(appTeamId, startOfToday)) {
            Optional<MatchResult> result = calculateWalker(playerId, appTeamId, matchId);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    public Optional<MatchResult> calculateWalker(
            Long playerId,
            Long appTeamId,
            Long matchId) {
        MatchEntity match = completedMatch(matchId, appTeamId, true).orElse(null);
        if (match == null) {
            return Optional.empty();
        }

        MatchEntity previousMatch = matchRepository
                .findFirstByAppTeamIdAndDateLessThanOrderByDateDesc(appTeamId, match.getDate())
                .orElse(null);
        if (previousMatch == null) {
            return Optional.empty();
        }

        LocalDate from = toPragueDate(previousMatch.getDate());
        LocalDate to = toPragueDate(match.getDate());
        List<PlayerStepTotalProjection> totals = stepUpdateRepository
                .playerTotals(appTeamId, from, to);
        if (totals.size() < MINIMUM_WALKERS) {
            return Optional.empty();
        }

        long winningSteps = safeLong(totals.get(0).getStepCount());
        long playerSteps = totals.stream()
                .filter(total -> playerId.equals(total.getPlayerId()))
                .map(PlayerStepTotalProjection::getStepCount)
                .mapToLong(StepAchievementCalculator::safeLong)
                .findFirst()
                .orElse(0);
        if (playerSteps == 0 || playerSteps != winningSteps) {
            return Optional.empty();
        }

        return Optional.of(new MatchResult(
                matchId,
                "Mezi zápasy od " + formatDate(from) + " do " + formatDate(to)
                        + " ušel " + playerSteps + " kroků a byl nejlepší z "
                        + totals.size() + " chodců."));
    }

    private Optional<MatchEntity> completedMatch(
            Long matchId,
            Long appTeamId,
            boolean requireCompletedDay) {
        return matchRepository.findByIdAndAppTeamId(matchId, appTeamId)
                .filter(match -> {
                    if (requireCompletedDay) {
                        return toPragueDate(match.getDate()).isBefore(LocalDate.now(PRAGUE_ZONE));
                    }
                    return !match.getDate().after(new Date());
                });
    }

    private PeriodStats stats(Long playerId, Long appTeamId, LocalDate from, LocalDate to) {
        StepPeriodStatsProjection stats = stepUpdateRepository
                .playerStats(playerId, appTeamId, from, to);
        if (stats == null) {
            return new PeriodStats(0, 0);
        }
        return new PeriodStats(safeLong(stats.getStepCount()), safeLong(stats.getDayCount()));
    }

    private LocalDate toPragueDate(Date date) {
        return date.toInstant().atZone(PRAGUE_ZONE).toLocalDate();
    }

    private static long safeLong(Long value) {
        return value == null ? 0 : value;
    }

    private String formatKilometers(double meters) {
        return String.format(Locale.forLanguageTag("cs-CZ"), "%.2f", meters / 1_000.0);
    }

    private String formatDate(LocalDate date) {
        return date.getDayOfMonth() + ". " + date.getMonthValue() + ". " + date.getYear();
    }

    public record MatchResult(Long matchId, String detail) {
    }

    public record MilestoneResult(long stepCount, long dayCount, long kilometers) {
        public String detail() {
            return "Uchozená vzdálenost " + stepCount + " kroků, respektive "
                    + kilometers + " kilometrů. Vše zvládnuto pouze za " + dayCount + " dní.";
        }
    }

    private record PeriodStats(long stepCount, long dayCount) {
    }

    private record PlayerTeamKey(Long playerId, Long appTeamId) {
    }
}
