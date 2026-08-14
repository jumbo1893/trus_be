package com.jumbo.trus.service.outbox;

import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import com.jumbo.trus.entity.outbox.OutboxEventEntity;
import com.jumbo.trus.entity.outbox.OutboxEventPayload;
import com.jumbo.trus.entity.outbox.OutboxEventType;
import com.jumbo.trus.entity.outbox.OutboxRelatedEntityType;
import com.jumbo.trus.repository.MatchPlayerIdProjection;
import com.jumbo.trus.repository.MatchRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AchievementEventProcessorTest {

    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final AchievementEventProcessor processor = new AchievementEventProcessor(matchRepository);

    @Test
    void mergesPlayersForSameTeamTypeAndMatch() {
        OutboxEventEntity first = event(
                1L,
                OutboxAggregateType.BEER,
                10L,
                payload(100L, Set.of(1L, 2L), Map.of())
        );
        OutboxEventEntity second = event(
                1L,
                OutboxAggregateType.BEER,
                11L,
                payload(100L, Set.of(2L, 3L), Map.of())
        );

        AchievementEventBatch batch = processor.createCalculationBatch(List.of(first, second));

        assertThat(batch.matchWorkByTeam().get(1L).get(OutboxAggregateType.BEER).get(100L))
                .containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(batch.unscopedWorkByTeam()).isEmpty();
    }

    @Test
    void keepsAggregateTypesAndTeamsSeparated() {
        OutboxEventEntity beer = event(
                1L,
                OutboxAggregateType.BEER,
                10L,
                payload(100L, Set.of(1L), Map.of())
        );
        OutboxEventEntity goalForOtherTeam = event(
                2L,
                OutboxAggregateType.GOAL,
                20L,
                payload(100L, Set.of(2L), Map.of())
        );

        AchievementEventBatch batch = processor.createCalculationBatch(List.of(beer, goalForOtherTeam));

        assertThat(batch.matchWorkByTeam()).containsOnlyKeys(1L, 2L);
        assertThat(batch.matchWorkByTeam().get(1L)).containsOnlyKeys(OutboxAggregateType.BEER);
        assertThat(batch.matchWorkByTeam().get(2L)).containsOnlyKeys(OutboxAggregateType.GOAL);
    }

    @Test
    void loadsMissingPlayersForAllRelatedMatchesInOneQuery() {
        Set<Long> matchIds = Set.of(100L, 101L);
        List<MatchPlayerIdProjection> rows = List.of(row(100L, 1L), row(100L, 2L), row(101L, 3L));
        when(matchRepository.findAffectedPlayersByMatchIds(eq(matchIds)))
                .thenReturn(rows);
        OutboxEventEntity season = event(
                1L,
                OutboxAggregateType.SEASON,
                50L,
                payload(null, Set.of(), Map.of(OutboxRelatedEntityType.MATCH, matchIds))
        );

        AchievementEventBatch batch = processor.createCalculationBatch(List.of(season));

        Map<Long, Set<Long>> seasonWork = batch.matchWorkByTeam().get(1L).get(OutboxAggregateType.SEASON);
        assertThat(seasonWork.get(100L)).containsExactlyInAnyOrder(1L, 2L);
        assertThat(seasonWork.get(101L)).containsExactly(3L);
        verify(matchRepository).findAffectedPlayersByMatchIds(eq(matchIds));
    }

    @Test
    void doesNotAddLookedUpPlayersToExplicitWorkForSameMatch() {
        Set<Long> matchIds = Set.of(100L);
        List<MatchPlayerIdProjection> rows = List.of(row(100L, 1L), row(100L, 2L));
        when(matchRepository.findAffectedPlayersByMatchIds(eq(matchIds)))
                .thenReturn(rows);
        OutboxEventEntity goal = event(
                1L,
                OutboxAggregateType.GOAL,
                10L,
                payload(100L, Set.of(7L), Map.of())
        );
        OutboxEventEntity footballMatch = event(
                1L,
                OutboxAggregateType.FOOTBALL_MATCH,
                20L,
                payload(null, Set.of(), Map.of(OutboxRelatedEntityType.MATCH, matchIds))
        );

        AchievementEventBatch batch = processor.createCalculationBatch(List.of(goal, footballMatch));

        assertThat(batch.matchWorkByTeam().get(1L).get(OutboxAggregateType.GOAL).get(100L))
                .containsExactly(7L);
        assertThat(batch.matchWorkByTeam().get(1L).get(OutboxAggregateType.FOOTBALL_MATCH).get(100L))
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void recordsEventsWithoutMatchesAsUnscopedWork() {
        OutboxEventEntity playerCreated = event(
                1L,
                OutboxAggregateType.PLAYER,
                7L,
                payload(null, Set.of(), Map.of())
        );

        AchievementEventBatch batch = processor.createCalculationBatch(List.of(playerCreated));

        assertThat(batch.matchWorkByTeam()).isEmpty();
        assertThat(batch.unscopedWorkByTeam().get(1L).get(OutboxAggregateType.PLAYER)).containsExactly(7L);
        assertThat(batch.workByTeamAndPlayer().get(1L).get(7L).unscopedChanges())
                .containsExactlyInAnyOrder(OutboxAggregateType.PLAYER, OutboxAggregateType.MATCH);
    }

    @Test
    void transposesMatchWorkToPlayerMatchAndChangedTypes() {
        OutboxEventEntity beer = event(
                1L,
                OutboxAggregateType.BEER,
                10L,
                payload(100L, Set.of(7L), Map.of())
        );
        OutboxEventEntity goal = event(
                1L,
                OutboxAggregateType.GOAL,
                11L,
                payload(100L, Set.of(7L), Map.of())
        );

        AchievementPlayerWork playerWork = processor.createCalculationBatch(List.of(beer, goal))
                .workByTeamAndPlayer().get(1L).get(7L);

        assertThat(playerWork.changesByMatch().get(100L))
                .containsExactlyInAnyOrder(OutboxAggregateType.BEER, OutboxAggregateType.GOAL);
    }

    @Test
    void fineDefinitionChangeAlsoTriggersReceivedFineAchievements() {
        OutboxEventEntity fine = event(
                1L,
                OutboxAggregateType.FINE,
                10L,
                payload(100L, Set.of(7L), Map.of())
        );

        AchievementPlayerWork playerWork = processor.createCalculationBatch(List.of(fine))
                .workByTeamAndPlayer().get(1L).get(7L);

        assertThat(playerWork.changesByMatch().get(100L))
                .containsExactlyInAnyOrder(OutboxAggregateType.FINE, OutboxAggregateType.RECEIVED_FINE);
    }

    private OutboxEventEntity event(
            Long appTeamId,
            OutboxAggregateType aggregateType,
            Long aggregateId,
            OutboxEventPayload payload
    ) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setAppTeamId(appTeamId);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(OutboxEventType.MATCH_UPDATED);
        event.setPayload(payload);
        return event;
    }

    private OutboxEventPayload payload(
            Long matchId,
            Set<Long> playerIds,
            Map<OutboxRelatedEntityType, Set<Long>> relatedIds
    ) {
        return new OutboxEventPayload(matchId, null, playerIds, Set.of(), Set.of(), relatedIds);
    }

    private MatchPlayerIdProjection row(Long matchId, Long playerId) {
        MatchPlayerIdProjection row = mock(MatchPlayerIdProjection.class);
        when(row.getMatchId()).thenReturn(matchId);
        when(row.getPlayerId()).thenReturn(playerId);
        return row;
    }
}
