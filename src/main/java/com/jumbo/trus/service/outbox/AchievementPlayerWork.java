package com.jumbo.trus.service.outbox;

import com.jumbo.trus.entity.outbox.OutboxAggregateType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public record AchievementPlayerWork(
        Map<Long, Set<OutboxAggregateType>> changesByMatch,
        Set<OutboxAggregateType> unscopedChanges
) {

    public AchievementPlayerWork {
        Map<Long, Set<OutboxAggregateType>> matches = new LinkedHashMap<>();
        changesByMatch.forEach((matchId, types) -> matches.put(matchId, Set.copyOf(types)));
        changesByMatch = Map.copyOf(matches);
        unscopedChanges = Set.copyOf(unscopedChanges);
    }
}
