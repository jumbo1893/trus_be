package com.jumbo.trus.service.achievement;

import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.PlayerStepTotalProjection;
import com.jumbo.trus.repository.StepPeriodStatsProjection;
import com.jumbo.trus.repository.StepUpdateRepository;
import com.jumbo.trus.repository.footbar.FootbarSessionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StepAchievementCalculatorTest {

    private static final ZoneId PRAGUE = ZoneId.of("Europe/Prague");

    private final StepUpdateRepository stepRepository = mock(StepUpdateRepository.class);
    private final FootbarSessionRepository footbarRepository = mock(FootbarSessionRepository.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final StepAchievementCalculator calculator = new StepAchievementCalculator(
            stepRepository, footbarRepository, matchRepository);

    @Test
    void totalStepsAreLoadedOnlyOncePerAchievementCalculationBatch() {
        LocalDate today = LocalDate.now(PRAGUE);
        StepPeriodStatsProjection totalStats = stats(200_000L, 30L);
        when(stepRepository.playerStats(7L, 3L, LocalDate.of(1970, 1, 1), today))
                .thenReturn(totalStats);

        calculator.beginCalculationBatch();
        try {
            assertThat(calculator.totalSteps(7L, 3L)).isEqualTo(200_000L);
            assertThat(calculator.totalSteps(7L, 3L)).isEqualTo(200_000L);
        } finally {
            calculator.endCalculationBatch();
        }

        verify(stepRepository, times(1))
                .playerStats(7L, 3L, LocalDate.of(1970, 1, 1), today);
    }

    @Test
    void milestoneUsesCumulativeValuesFromTheFirstDayThresholdWasReached() {
        LocalDate today = LocalDate.now(PRAGUE);
        StepPeriodStatsProjection currentStats = stats(2_100_000L, 300L);
        StepPeriodStatsProjection attainmentStats = stats(1_605_000L, 201L);
        when(stepRepository.playerStats(7L, 3L, LocalDate.of(1970, 1, 1), today))
                .thenReturn(currentStats);
        when(stepRepository.milestoneStats(7L, 3L, 1_600_000L))
                .thenReturn(Optional.of(attainmentStats));

        calculator.beginCalculationBatch();
        Optional<StepAchievementCalculator.MilestoneResult> result;
        try {
            result = calculator.milestoneResult(7L, 3L, 1_600_000L);
        } finally {
            calculator.endCalculationBatch();
        }

        StepAchievementCalculator.MilestoneResult milestone = result.orElseThrow();
        assertThat(milestone).isEqualTo(new StepAchievementCalculator.MilestoneResult(
                1_605_000L, 201L, 1_252L));
        assertThat(milestone.detail()).isEqualTo(
                "Uchozená vzdálenost 1605000 kroků, respektive 1252 kilometrů. "
                        + "Vše zvládnuto pouze za 201 dní.");
    }

    @Test
    void strengthSavingUsesExactlyTwoCalendarDaysBeforeMatchAndRequiresTwoKilometers() {
        LocalDate matchDate = LocalDate.of(2026, 1, 20);
        MatchEntity match = match(10L, matchDate);
        StepPeriodStatsProjection walkingStats = stats(2_000L, 2L);
        when(matchRepository.findByIdAndAppTeamId(10L, 3L)).thenReturn(Optional.of(match));
        when(footbarRepository.findDistanceByPlayerAndMatch(7L, 10L, 3L)).thenReturn(2_000.0);
        when(stepRepository.playerStats(
                7L, 3L, LocalDate.of(2026, 1, 18), LocalDate.of(2026, 1, 19)))
                .thenReturn(walkingStats);

        Optional<StepAchievementCalculator.MatchResult> result =
                calculator.calculateStrengthSaving(7L, 3L, 10L);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().matchId()).isEqualTo(10L);
        assertThat(result.orElseThrow().detail()).contains("2000 kroků");
    }

    @Test
    void strengthSavingFailsWhenMatchDistanceIsBelowTwoKilometers() {
        MatchEntity match = match(10L, LocalDate.of(2026, 1, 20));
        when(matchRepository.findByIdAndAppTeamId(10L, 3L)).thenReturn(Optional.of(match));
        when(footbarRepository.findDistanceByPlayerAndMatch(7L, 10L, 3L)).thenReturn(1_999.9);

        assertThat(calculator.calculateStrengthSaving(7L, 3L, 10L)).isEmpty();
    }

    @Test
    void walkerAcceptsSharedFirstPlaceWhenAtLeastThreePeopleWalked() {
        MatchEntity current = match(20L, LocalDate.now(PRAGUE).minusDays(1));
        MatchEntity previous = match(10L, LocalDate.now(PRAGUE).minusDays(8));
        List<PlayerStepTotalProjection> totals = List.of(
                total(7L, 60_000L),
                total(8L, 60_000L),
                total(9L, 42_000L));
        when(matchRepository.findByIdAndAppTeamId(20L, 3L)).thenReturn(Optional.of(current));
        when(matchRepository.findFirstByAppTeamIdAndDateLessThanOrderByDateDesc(3L, current.getDate()))
                .thenReturn(Optional.of(previous));
        when(stepRepository.playerTotals(
                3L, LocalDate.now(PRAGUE).minusDays(8), LocalDate.now(PRAGUE).minusDays(1)))
                .thenReturn(totals);

        Optional<StepAchievementCalculator.MatchResult> result =
                calculator.calculateWalker(7L, 3L, 20L);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().detail()).contains("60000 kroků", "3 chodců");
    }

    @Test
    void walkerFailsWhenOnlyTwoPeopleReportedSteps() {
        MatchEntity current = match(20L, LocalDate.now(PRAGUE).minusDays(1));
        MatchEntity previous = match(10L, LocalDate.now(PRAGUE).minusDays(8));
        List<PlayerStepTotalProjection> totals = List.of(
                total(7L, 60_000L), total(8L, 42_000L));
        when(matchRepository.findByIdAndAppTeamId(20L, 3L)).thenReturn(Optional.of(current));
        when(matchRepository.findFirstByAppTeamIdAndDateLessThanOrderByDateDesc(3L, current.getDate()))
                .thenReturn(Optional.of(previous));
        when(stepRepository.playerTotals(
                3L, LocalDate.now(PRAGUE).minusDays(8), LocalDate.now(PRAGUE).minusDays(1)))
                .thenReturn(totals);

        assertThat(calculator.calculateWalker(7L, 3L, 20L)).isEmpty();
    }

    private static MatchEntity match(Long id, LocalDate date) {
        MatchEntity match = new MatchEntity();
        match.setId(id);
        match.setDate(Date.from(date.atTime(18, 30).atZone(PRAGUE).toInstant()));
        return match;
    }

    private static StepPeriodStatsProjection stats(Long steps, Long days) {
        StepPeriodStatsProjection projection = mock(StepPeriodStatsProjection.class);
        when(projection.getStepCount()).thenReturn(steps);
        when(projection.getDayCount()).thenReturn(days);
        return projection;
    }

    private static PlayerStepTotalProjection total(Long playerId, Long steps) {
        PlayerStepTotalProjection projection = mock(PlayerStepTotalProjection.class);
        when(projection.getPlayerId()).thenReturn(playerId);
        when(projection.getStepCount()).thenReturn(steps);
        return projection;
    }
}
