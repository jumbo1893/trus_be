package com.jumbo.trus.repository.specification;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.Locale;

/** Shared restrictions applied before statistics are aggregated. */
public final class StatisticsPredicates {
    private StatisticsPredicates() {}

    public static void addMatch(List<Predicate> predicates, Path<?> match,
                                CriteriaBuilder cb, StatisticsFilter filter) {
        if (filter.getSeasonIds() != null && !filter.getSeasonIds().isEmpty()
                && !filter.getSeasonIds().contains(Config.ALL_SEASON_ID)) {
            predicates.add(match.get("season").get("id").in(filter.getSeasonIds()));
        }
        if (filter.getOpponentNames() != null && !filter.getOpponentNames().isEmpty()) {
            predicates.add(cb.lower(cb.trim(match.get("name"))).in(
                    filter.getOpponentNames().stream()
                            .map(name -> name.trim().toLowerCase(Locale.ROOT)).toList()));
        }
    }

    public static void addPlayer(List<Predicate> predicates, Path<?> player,
                                 StatisticsFilter filter) {
        if (filter.getPlayerIds() != null && !filter.getPlayerIds().isEmpty()) {
            predicates.add(player.get("id").in(filter.getPlayerIds()));
        }
    }
}
