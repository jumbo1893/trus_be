package com.jumbo.trus.service.outbox;

import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import com.jumbo.trus.entity.outbox.OutboxEventEntity;
import com.jumbo.trus.entity.outbox.OutboxEventPayload;
import com.jumbo.trus.entity.outbox.OutboxRelatedEntityType;
import com.jumbo.trus.repository.MatchPlayerIdProjection;
import com.jumbo.trus.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AchievementEventProcessor {

    private final MatchRepository matchRepository;

    /**
     * Normalizes and merges all events from one poll before any achievement is calculated.
     */
    public AchievementEventBatch createCalculationBatch(List<OutboxEventEntity> events) {
        if (events == null || events.isEmpty()) {
            return new AchievementEventBatch(Map.of(), Map.of());
        }

        List<EventWork> eventWork = events.stream()
                .map(this::toEventWork)
                .toList();

        Set<Long> matchesRequiringPlayerLookup = eventWork.stream()
                .filter(EventWork::requiresPlayerLookup)
                .map(EventWork::matchIds)
                .flatMap(Collection::stream)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        Map<Long, Set<Long>> lookedUpPlayersByMatch = loadPlayersByMatch(matchesRequiringPlayerLookup);
        Map<Long, Map<OutboxAggregateType, Map<Long, Set<Long>>>> matchWorkByTeam = new LinkedHashMap<>();
        Map<Long, Map<OutboxAggregateType, Set<Long>>> unscopedWorkByTeam = new LinkedHashMap<>();

        for (EventWork work : eventWork) {
            if (work.matchIds().isEmpty()) {
                for (OutboxAggregateType changedType : work.changedTypes()) {
                    unscopedWorkByTeam
                            .computeIfAbsent(work.appTeamId(), ignored -> new LinkedHashMap<>())
                            .computeIfAbsent(changedType, ignored -> new LinkedHashSet<>())
                            .addAll(work.explicitPlayerIds());
                }
                continue;
            }

            for (Long matchId : work.matchIds()) {
                Set<Long> affectedPlayerIds = new LinkedHashSet<>(work.explicitPlayerIds());
                if (work.requiresPlayerLookup()) {
                    affectedPlayerIds.addAll(lookedUpPlayersByMatch.getOrDefault(matchId, Set.of()));
                }
                for (OutboxAggregateType changedType : work.changedTypes()) {
                    merge(matchWorkByTeam, work.appTeamId(), changedType, matchId, affectedPlayerIds);
                }
            }
        }

        AchievementEventBatch batch = new AchievementEventBatch(matchWorkByTeam, unscopedWorkByTeam);
        log.debug("Created achievement calculation batch from {} events: {}", events.size(), batch);
        return batch;
    }

    private EventWork toEventWork(OutboxEventEntity event) {
        OutboxEventPayload payload = event.getPayload();
        Set<Long> matchIds = new LinkedHashSet<>();
        if (payload.matchId() != null) {
            matchIds.add(payload.matchId());
        }
        matchIds.addAll(relatedIds(payload, OutboxRelatedEntityType.MATCH));

        Set<Long> explicitPlayerIds = new LinkedHashSet<>(payload.affectedPlayerIds());
        if (event.getAggregateType() == OutboxAggregateType.PLAYER && event.getAggregateId() != null) {
            explicitPlayerIds.add(event.getAggregateId());
        }

        return new EventWork(
                event.getAppTeamId(),
                changedTypes(event.getAggregateType()),
                matchIds,
                explicitPlayerIds,
                explicitPlayerIds.isEmpty() && !matchIds.isEmpty()
        );
    }

    private Set<OutboxAggregateType> changedTypes(OutboxAggregateType aggregateType) {
        return switch (aggregateType) {
            case FINE -> Set.of(OutboxAggregateType.FINE, OutboxAggregateType.RECEIVED_FINE);
            case SEASON -> Set.of(OutboxAggregateType.SEASON, OutboxAggregateType.MATCH);
            case PLAYER -> Set.of(OutboxAggregateType.PLAYER, OutboxAggregateType.MATCH);
            default -> Set.of(aggregateType);
        };
    }

    private Set<Long> relatedIds(OutboxEventPayload payload, OutboxRelatedEntityType type) {
        return payload.relatedEntityIds().getOrDefault(type, Set.of());
    }

    private Map<Long, Set<Long>> loadPlayersByMatch(Set<Long> matchIds) {
        if (matchIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Set<Long>> result = new LinkedHashMap<>();
        for (MatchPlayerIdProjection row : matchRepository.findAffectedPlayersByMatchIds(matchIds)) {
            result.computeIfAbsent(row.getMatchId(), ignored -> new LinkedHashSet<>())
                    .add(row.getPlayerId());
        }
        return result;
    }

    private void merge(
            Map<Long, Map<OutboxAggregateType, Map<Long, Set<Long>>>> target,
            Long appTeamId,
            OutboxAggregateType aggregateType,
            Long matchId,
            Set<Long> playerIds
    ) {
        target.computeIfAbsent(appTeamId, ignored -> new LinkedHashMap<>())
                .computeIfAbsent(aggregateType, ignored -> new LinkedHashMap<>())
                .computeIfAbsent(matchId, ignored -> new LinkedHashSet<>())
                .addAll(playerIds);
    }

    private record EventWork(
            Long appTeamId,
            Set<OutboxAggregateType> changedTypes,
            Set<Long> matchIds,
            Set<Long> explicitPlayerIds,
            boolean requiresPlayerLookup
    ) {
        private EventWork {
            matchIds = Set.copyOf(matchIds);
            changedTypes = Set.copyOf(changedTypes);
            explicitPlayerIds = Set.copyOf(explicitPlayerIds);
        }
    }
}
