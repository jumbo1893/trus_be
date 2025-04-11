package com.jumbo.trus.entity.repository.specification;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.entity.*;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import jakarta.persistence.criteria.*;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class GoalStatsSpecification implements Specification<GoalEntity> {

    private final StatisticsFilter filter;

    @Override
    public Predicate toPredicate(@NotNull Root<GoalEntity> root, @NotNull CriteriaQuery<?> query, @NotNull CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();

        if (filter.getAppTeam() != null) {
            predicates.add(criteriaBuilder.equal(root.get(GoalEntity_.APP_TEAM), filter.getAppTeam()));
        }

        if (filter.getMatchId() != null) {
            Join<MatchEntity, GoalEntity> join = root.join(GoalEntity_.MATCH);
            predicates.add(criteriaBuilder.equal(join.get(MatchEntity_.ID), filter.getMatchId()));
        }

        if (filter.getPlayerId() != null) {
            Join<PlayerEntity, GoalEntity> join = root.join(GoalEntity_.PLAYER);
            predicates.add(criteriaBuilder.equal(join.get(PlayerEntity_.ID), filter.getPlayerId()));
        }

        if (filter.getSeasonId() != null) {
            if (filter.getSeasonId() != Config.ALL_SEASON_ID) {
                Join<MatchEntity, GoalEntity> matchJoin = root.join(GoalEntity_.MATCH);
                Join<SeasonEntity, MatchEntity> seasonJoin = matchJoin.join(MatchEntity_.SEASON);
                predicates.add(criteriaBuilder.equal(seasonJoin.get(SeasonEntity_.ID), filter.getSeasonId()));
            }
        }

        if (filter.getStringFilter() != null && filter.getMatchStatsOrPlayerStats() != null && !filter.getStringFilter().isEmpty()) {
            String searchString = "%" + filter.getStringFilter().trim().toLowerCase() + "%";
            if (filter.getMatchStatsOrPlayerStats()) {
                Join<MatchEntity, GoalEntity> matchJoin = root.join(GoalEntity_.MATCH);
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(matchJoin.get(MatchEntity_.NAME)), searchString));
            }
            else {
                Join<PlayerEntity, GoalEntity> playerJoin = root.join(GoalEntity_.PLAYER);
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(playerJoin.get(PlayerEntity_.NAME)), searchString));
            }

        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }
}
