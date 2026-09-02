package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.SeasonEntity;
import com.jumbo.trus.entity.achievement.AchievementEntity;
import com.jumbo.trus.entity.achievement.AchievementProgressNotificationEntity;
import com.jumbo.trus.entity.achievement.PlayerAchievementEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.achievement.AchievementProgressNotificationRepository;
import com.jumbo.trus.repository.achievement.AchievementProgressQueryRepository;
import com.jumbo.trus.repository.achievement.AchievementProgressQueryRepository.DrinkerProgressProjection;
import com.jumbo.trus.repository.achievement.AchievementProgressQueryRepository.PlayerDrinkTotalsProjection;
import com.jumbo.trus.repository.achievement.AchievementProgressQueryRepository.ScorerProgressProjection;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.notification.push.maker.AchievementProgressNotificationMaker;
import com.jumbo.trus.service.outbox.AchievementEventBatch;
import com.jumbo.trus.service.player.PlayerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.jumbo.trus.service.achievement.AchievementCodes.KDYZ_ONO_TO_CHUTNA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AchievementProgressServiceTest {

    @Test
    void drinkNotificationsUseFiveAndOneDrinkBoundaries() {
        assertThat(AchievementProgressService.drinkProximityLevel(6)).isNull();
        assertThat(AchievementProgressService.drinkProximityLevel(5)).isEqualTo(5L);
        assertThat(AchievementProgressService.drinkProximityLevel(2)).isEqualTo(5L);
        assertThat(AchievementProgressService.drinkProximityLevel(1)).isEqualTo(1L);
        assertThat(AchievementProgressService.drinkProximityLevel(0)).isNull();
    }

    @Test
    void stepNotificationsUseTenThousandAndOneThousandStepBoundaries() {
        assertThat(AchievementProgressService.stepProximityLevel(10_001)).isNull();
        assertThat(AchievementProgressService.stepProximityLevel(10_000)).isEqualTo(10_000L);
        assertThat(AchievementProgressService.stepProximityLevel(1_001)).isEqualTo(10_000L);
        assertThat(AchievementProgressService.stepProximityLevel(1_000)).isEqualTo(1_000L);
        assertThat(AchievementProgressService.stepProximityLevel(1)).isEqualTo(1_000L);
        assertThat(AchievementProgressService.stepProximityLevel(0)).isNull();
    }

    @Test
    void scorerNotificationRequiresOneGoalToReallyReachFirstPlace() {
        assertThat(AchievementProgressService.isOneGoalFromLead(
                scorer(4L, 1L, 5L, 2L))).isFalse();
        assertThat(AchievementProgressService.isOneGoalFromLead(
                scorer(4L, 2L, 5L, 2L))).isTrue();
        assertThat(AchievementProgressService.isOneGoalFromLead(
                scorer(5L, 2L, 5L, 2L))).isFalse();
    }

    @Test
    void drinkerNotificationRequiresOneDrinkToReallyReachBestAverage() {
        assertThat(AchievementProgressService.isOneDrinkFromLead(
                drinker(9L, 3L, 10L, 3L))).isTrue();
        assertThat(AchievementProgressService.isOneDrinkFromLead(
                drinker(5L, 3L, 7L, 3L))).isFalse();
        assertThat(AchievementProgressService.isOneDrinkFromLead(
                drinker(10L, 3L, 10L, 3L))).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void reservesEachDrinkBoundaryOnlyOnceAndAllowsCloserBoundaryLater() {
        ProgressFixture fixture = progressFixture(45L);

        fixture.service().evaluateAndNotify(fixture.batch());

        ArgumentCaptor<List<AchievementProgressNotificationEntity>> markerCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(fixture.notificationRepository()).saveAll(markerCaptor.capture());
        assertThat(markerCaptor.getValue())
                .singleElement()
                .satisfies(marker -> {
                    assertThat(marker.getContextKey()).isEqualTo("ALL");
                    assertThat(marker.getProximityThreshold()).isEqualTo(5L);
                });

        ArgumentCaptor<List<AchievementProgressMessage>> messageCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(fixture.notificationMaker()).sendNotifications(messageCaptor.capture());
        assertThat(messageCaptor.getValue())
                .singleElement()
                .satisfies(message -> assertThat(message.missingText()).isEqualTo("chybí 5 piv"));

        ProgressFixture closerFixture = progressFixture(49L);
        AchievementProgressNotificationEntity widerBoundary =
                new AchievementProgressNotificationEntity(
                        closerFixture.playerAchievement(), "ALL", 5L);
        when(closerFixture.notificationRepository()
                .findAllByPlayerAchievementIdIn(List.of(11L)))
                .thenReturn(List.of(widerBoundary));

        closerFixture.service().evaluateAndNotify(closerFixture.batch());

        ArgumentCaptor<List<AchievementProgressNotificationEntity>> closerMarkerCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(closerFixture.notificationRepository()).saveAll(closerMarkerCaptor.capture());
        assertThat(closerMarkerCaptor.getValue())
                .singleElement()
                .extracting(AchievementProgressNotificationEntity::getProximityThreshold)
                .isEqualTo(1L);
    }

    @Test
    void doesNotReserveTheSameDrinkBoundaryAgain() {
        ProgressFixture fixture = progressFixture(47L);
        AchievementProgressNotificationEntity existingBoundary =
                new AchievementProgressNotificationEntity(
                        fixture.playerAchievement(), "ALL", 5L);
        when(fixture.notificationRepository().findAllByPlayerAchievementIdIn(List.of(11L)))
                .thenReturn(List.of(existingBoundary));

        fixture.service().evaluateAndNotify(fixture.batch());

        verify(fixture.notificationRepository(), never()).saveAll(any());
        verify(fixture.notificationMaker()).sendNotifications(List.of());
    }

    private static ProgressFixture progressFixture(long beers) {
        AppTeamService appTeamService = mock(AppTeamService.class);
        PlayerService playerService = mock(PlayerService.class);
        MatchRepository matchRepository = mock(MatchRepository.class);
        PlayerAchievementRepository playerAchievementRepository =
                mock(PlayerAchievementRepository.class);
        AchievementProgressQueryRepository queryRepository =
                mock(AchievementProgressQueryRepository.class);
        AchievementProgressNotificationRepository notificationRepository =
                mock(AchievementProgressNotificationRepository.class);
        StepAchievementCalculator stepCalculator = mock(StepAchievementCalculator.class);
        AchievementProgressNotificationMaker notificationMaker =
                mock(AchievementProgressNotificationMaker.class);

        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setId(3L);
        PlayerDTO player = new PlayerDTO();
        player.setId(7L);

        AchievementEntity achievement = new AchievementEntity();
        achievement.setId(10L);
        achievement.setCode(KDYZ_ONO_TO_CHUTNA);
        achievement.setName("Když ono to chutná");
        achievement.setOnlyForPlayers(false);
        PlayerEntity playerEntity = new PlayerEntity();
        playerEntity.setId(7L);
        PlayerAchievementEntity playerAchievement = new PlayerAchievementEntity();
        playerAchievement.setId(11L);
        playerAchievement.setAchievement(achievement);
        playerAchievement.setPlayer(playerEntity);
        playerAchievement.setAccomplished(false);

        MatchEntity match = new MatchEntity();
        match.setId(100L);
        match.setAppTeam(appTeam);
        SeasonEntity season = new SeasonEntity();
        season.setId(20L);
        match.setSeason(season);

        PlayerDrinkTotalsProjection totals = mock(PlayerDrinkTotalsProjection.class);
        when(totals.getPlayerId()).thenReturn(7L);
        when(totals.getBeerCount()).thenReturn(beers);
        when(totals.getLiquorCount()).thenReturn(0L);

        when(appTeamService.findAppTeamByIdOrThrow(3L)).thenReturn(appTeam);
        when(playerService.getAllByIds(Set.of(7L), 3L)).thenReturn(List.of(player));
        when(playerAchievementRepository.findAllByPlayerIdIn(List.of(7L)))
                .thenReturn(List.of(playerAchievement));
        when(matchRepository.findAllById(Set.of(100L))).thenReturn(List.of(match));
        when(queryRepository.findDrinkTotals(Set.of(7L), 3L)).thenReturn(List.of(totals));
        when(notificationRepository.findAllByPlayerAchievementIdIn(List.of(11L)))
                .thenReturn(List.of());

        AchievementProgressService service = new AchievementProgressService(
                appTeamService,
                playerService,
                matchRepository,
                playerAchievementRepository,
                queryRepository,
                notificationRepository,
                stepCalculator,
                notificationMaker
        );
        AchievementEventBatch batch = new AchievementEventBatch(
                Map.of(3L, Map.of(
                        OutboxAggregateType.BEER,
                        Map.of(100L, Set.of(7L))
                )),
                Map.of()
        );
        return new ProgressFixture(
                service,
                batch,
                playerAchievement,
                notificationRepository,
                notificationMaker
        );
    }

    private static ScorerProgressProjection scorer(
            Long playerGoals,
            Long playerAssists,
            Long leaderGoals,
            Long leaderAssists
    ) {
        ScorerProgressProjection projection = mock(ScorerProgressProjection.class);
        when(projection.getPlayerGoals()).thenReturn(playerGoals);
        when(projection.getPlayerAssists()).thenReturn(playerAssists);
        when(projection.getLeaderGoals()).thenReturn(leaderGoals);
        when(projection.getLeaderAssists()).thenReturn(leaderAssists);
        return projection;
    }

    private static DrinkerProgressProjection drinker(
            Long playerDrinks,
            Long playerMatches,
            Long leaderDrinks,
            Long leaderMatches
    ) {
        DrinkerProgressProjection projection = mock(DrinkerProgressProjection.class);
        when(projection.getPlayerDrinks()).thenReturn(playerDrinks);
        when(projection.getPlayerMatches()).thenReturn(playerMatches);
        when(projection.getLeaderDrinks()).thenReturn(leaderDrinks);
        when(projection.getLeaderMatches()).thenReturn(leaderMatches);
        return projection;
    }

    private record ProgressFixture(
            AchievementProgressService service,
            AchievementEventBatch batch,
            PlayerAchievementEntity playerAchievement,
            AchievementProgressNotificationRepository notificationRepository,
            AchievementProgressNotificationMaker notificationMaker
    ) {
    }
}
