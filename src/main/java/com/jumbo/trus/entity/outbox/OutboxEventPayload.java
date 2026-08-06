package com.jumbo.trus.entity.outbox;

import java.util.Map;
import java.util.Set;

public record OutboxEventPayload(
        Long matchId,
        Long seasonId,

        Set<Long> affectedPlayerIds,
        Set<Long> addedPlayerIds,
        Set<Long> removedPlayerIds,

        Map<OutboxRelatedEntityType, Set<Long>> relatedEntityIds


) {

    public OutboxEventPayload {
        affectedPlayerIds = emptyIfNull(affectedPlayerIds);
        addedPlayerIds = emptyIfNull(addedPlayerIds);
        removedPlayerIds = emptyIfNull(removedPlayerIds);
        relatedEntityIds = emptyMapIfNull(relatedEntityIds);
    }

    private static <T> Set<T> emptyIfNull(Set<T> values) {
        return values == null
                ? Set.of()
                : Set.copyOf(values);
    }

    private static <K, V> Map<K, V> emptyMapIfNull(Map<K, V> values) {
        return values == null
                ? Map.of()
                : Map.copyOf(values);
    }
}