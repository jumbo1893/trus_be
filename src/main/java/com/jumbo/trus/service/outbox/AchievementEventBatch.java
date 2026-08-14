package com.jumbo.trus.service.outbox;

import com.jumbo.trus.entity.outbox.OutboxAggregateType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Immutable plan of achievement recalculations created from one outbox polling batch.
 * Match-scoped work is grouped by team, changed aggregate type and match.
 */
public record AchievementEventBatch(
        Map<Long, Map<OutboxAggregateType, Map<Long, Set<Long>>>> matchWorkByTeam,
        Map<Long, Map<OutboxAggregateType, Set<Long>>> unscopedWorkByTeam
) {

    public AchievementEventBatch {
        matchWorkByTeam = immutableMatchWork(matchWorkByTeam);
        unscopedWorkByTeam = immutableUnscopedWork(unscopedWorkByTeam);
    }

    public boolean isEmpty() {
        return matchWorkByTeam.isEmpty() && unscopedWorkByTeam.isEmpty();
    }

    public Map<Long, Map<Long, AchievementPlayerWork>> workByTeamAndPlayer() {
        Map<Long, Map<Long, MutablePlayerWork>> mutable = new LinkedHashMap<>();

        matchWorkByTeam.forEach((teamId, workByType) ->
                workByType.forEach((type, workByMatch) ->
                        workByMatch.forEach((matchId, playerIds) ->
                                playerIds.forEach(playerId -> mutable
                                        .computeIfAbsent(teamId, ignored -> new LinkedHashMap<>())
                                        .computeIfAbsent(playerId, ignored -> new MutablePlayerWork())
                                        .addMatchChange(matchId, type))
                        )
                )
        );

        unscopedWorkByTeam.forEach((teamId, workByType) ->
                workByType.forEach((type, playerIds) ->
                        playerIds.forEach(playerId -> mutable
                                .computeIfAbsent(teamId, ignored -> new LinkedHashMap<>())
                                .computeIfAbsent(playerId, ignored -> new MutablePlayerWork())
                                .unscopedChanges.add(type))
                )
        );

        Map<Long, Map<Long, AchievementPlayerWork>> result = new LinkedHashMap<>();
        mutable.forEach((teamId, workByPlayer) -> {
            Map<Long, AchievementPlayerWork> players = new LinkedHashMap<>();
            workByPlayer.forEach((playerId, work) -> players.put(playerId, work.toImmutable()));
            result.put(teamId, Map.copyOf(players));
        });
        return Map.copyOf(result);
    }

    private static Map<Long, Map<OutboxAggregateType, Map<Long, Set<Long>>>> immutableMatchWork(
            Map<Long, Map<OutboxAggregateType, Map<Long, Set<Long>>>> source
    ) {
        Map<Long, Map<OutboxAggregateType, Map<Long, Set<Long>>>> teams = new LinkedHashMap<>();
        source.forEach((teamId, workByType) -> {
            Map<OutboxAggregateType, Map<Long, Set<Long>>> types = new LinkedHashMap<>();
            workByType.forEach((type, workByMatch) -> {
                Map<Long, Set<Long>> matches = new LinkedHashMap<>();
                workByMatch.forEach((matchId, playerIds) ->
                        matches.put(matchId, Set.copyOf(playerIds))
                );
                types.put(type, Map.copyOf(matches));
            });
            teams.put(teamId, Map.copyOf(types));
        });
        return Map.copyOf(teams);
    }

    private static Map<Long, Map<OutboxAggregateType, Set<Long>>> immutableUnscopedWork(
            Map<Long, Map<OutboxAggregateType, Set<Long>>> source
    ) {
        Map<Long, Map<OutboxAggregateType, Set<Long>>> result = new LinkedHashMap<>();
        source.forEach((teamId, workByType) -> {
            Map<OutboxAggregateType, Set<Long>> types = new LinkedHashMap<>();
            workByType.forEach((type, playerIds) -> types.put(type, Set.copyOf(playerIds)));
            result.put(teamId, Map.copyOf(types));
        });
        return Map.copyOf(result);
    }

    private static final class MutablePlayerWork {
        private final Map<Long, Set<OutboxAggregateType>> changesByMatch = new LinkedHashMap<>();
        private final Set<OutboxAggregateType> unscopedChanges = new java.util.LinkedHashSet<>();

        private void addMatchChange(Long matchId, OutboxAggregateType type) {
            changesByMatch.computeIfAbsent(matchId, ignored -> new java.util.LinkedHashSet<>()).add(type);
        }

        private AchievementPlayerWork toImmutable() {
            return new AchievementPlayerWork(changesByMatch, unscopedChanges);
        }
    }
}
