package com.jumbo.trus.service.outbox;

import com.jumbo.trus.entity.outbox.OutboxEventPayload;
import com.jumbo.trus.entity.outbox.OutboxRelatedEntityType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class OutboxEventPayloadFactory {

    private OutboxEventPayloadFactory() {
    }

    public static OutboxEventPayload matchAdded(
            Long matchId,
            Long seasonId,
            Set<Long> addedPlayerIds
    ) {
        Set<Long> affectedPlayerIds = new HashSet<>(addedPlayerIds);

        return new OutboxEventPayload(
                matchId,
                seasonId,
                affectedPlayerIds,
                addedPlayerIds,
                null,
                Map.of()
        );
    }

    public static OutboxEventPayload matchDeleted(
            Long matchId,
            Long seasonId,
            Set<Long> removedPlayerIds
    ) {
        return new OutboxEventPayload(
                matchId,
                seasonId,
                removedPlayerIds,
                Set.of(),
                removedPlayerIds,
                Map.of()
        );
    }

    public static OutboxEventPayload matchChanged(
            Long matchId,
            Long seasonId,
            Set<Long> addedPlayerIds,
            Set<Long> removedPlayerIds
    ) {
        Set<Long> affectedPlayerIds = new HashSet<>(addedPlayerIds);
        affectedPlayerIds.addAll(removedPlayerIds);

        return new OutboxEventPayload(
                matchId,
                seasonId,
                affectedPlayerIds,
                addedPlayerIds,
                removedPlayerIds,
                Map.of()
        );
    }

    public static OutboxEventPayload playerDeleted(Set<Long> affectedMatches) {
        return affectedMatches(affectedMatches);
    }

    public static OutboxEventPayload playerCreated(Set<Long> affectedMatches) {
        return affectedMatches(affectedMatches);
    }

    public static OutboxEventPayload playerUpdated(Set<Long> affectedMatches) {
        return affectedMatches(affectedMatches);
    }

    public static OutboxEventPayload seasonCreated(Set<Long> affectedMatches) {
        return affectedMatches(affectedMatches);
    }

    public static OutboxEventPayload seasonUpdated(Set<Long> affectedMatches) {
        return affectedMatches(affectedMatches);
    }

    public static OutboxEventPayload seasonDeleted(Set<Long> affectedMatches) {
        return affectedMatches(affectedMatches);
    }

    public static OutboxEventPayload fineCreated(Set<Long> affectedMatches) {
        return affectedMatches(affectedMatches);
    }

    public static OutboxEventPayload fineUpdated(Set<Long> affectedMatches, Set<Long> affectedPlayerIds) {
        return affectedMatchesAndPlayers(affectedMatches, affectedPlayerIds);
    }

    public static OutboxEventPayload fineDeleted(Set<Long> affectedMatches, Set<Long> affectedPlayerIds) {
        return affectedMatchesAndPlayers(affectedMatches, affectedPlayerIds);
    }

    public static OutboxEventPayload footballMatchUpdated(
            Set<Long> affectedMatches,
            Set<Long> affectedFootballPlayerIds
    ) {
        Map<OutboxRelatedEntityType, Set<Long>> relatedEntityIds = new HashMap<>();
        relatedEntityIds.put(OutboxRelatedEntityType.MATCH, affectedMatches);
        relatedEntityIds.put(OutboxRelatedEntityType.FOOTBALL_PLAYER, affectedFootballPlayerIds);

        return new OutboxEventPayload(
                null,
                null,
                null,
                null,
                null,
                relatedEntityIds
        );
    }

    private static OutboxEventPayload affectedMatches(Set<Long> affectedMatches) {
        return new OutboxEventPayload(
                null,
                null,
                null,
                null,
                null,
                Map.of(
                        OutboxRelatedEntityType.MATCH,
                        affectedMatches
                )
        );
    }

    private static OutboxEventPayload affectedMatchesAndPlayers(
            Set<Long> affectedMatches,
            Set<Long> affectedPlayerIds
    ) {
        return new OutboxEventPayload(
                null,
                null,
                affectedPlayerIds,
                null,
                null,
                Map.of(OutboxRelatedEntityType.MATCH, affectedMatches)
        );
    }

    public static OutboxEventPayload receivedFinesChanged(
            Long matchId,
            Long seasonId,
            Set<Long> affectedPlayerIds,
            Set<Long> receivedFineIds,
            Set<Long> fineIds
    ) {
        Map<OutboxRelatedEntityType, Set<Long>> relatedEntityIds = new HashMap<>();
        relatedEntityIds.put(OutboxRelatedEntityType.RECEIVED_FINE, receivedFineIds);
        relatedEntityIds.put(OutboxRelatedEntityType.FINE, fineIds);
        return new OutboxEventPayload(
                matchId,
                seasonId,
                affectedPlayerIds,
                Set.of(),
                Set.of(),
               relatedEntityIds
        );
    }

    public static OutboxEventPayload beersChanged(
            Long matchId,
            Long seasonId,
            Set<Long> affectedPlayerIds,
            Set<Long> beerIds
    ) {
        return new OutboxEventPayload(
                matchId,
                seasonId,
                affectedPlayerIds,
                Set.of(),
                Set.of(),
                Map.of(
                        OutboxRelatedEntityType.BEER,
                        beerIds
                )
        );
    }

    public static OutboxEventPayload goalChanged(
            Long matchId,
            Long seasonId,
            Set<Long> affectedPlayerIds,
            Set<Long> goalIds
    ) {
        return new OutboxEventPayload(
                matchId,
                seasonId,
                affectedPlayerIds,
                Set.of(),
                Set.of(),
                Map.of(
                        OutboxRelatedEntityType.GOAL,
                        goalIds
                )
        );
    }

    public static OutboxEventPayload footbarUpdated(
            Long matchId,
            Long seasonId,
            Set<Long> affectedPlayerIds,
            Set<Long> footbarSessionIds
    ) {
        return new OutboxEventPayload(
                matchId,
                seasonId,
                affectedPlayerIds,
                Set.of(),
                Set.of(),
                Map.of(
                        OutboxRelatedEntityType.FOOTBAR,
                        footbarSessionIds
                )
        );
    }
}
