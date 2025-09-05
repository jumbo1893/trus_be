package com.jumbo.trus.repository.specification;

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
public class ReceivedFineStatsSpecification implements Specification<ReceivedFineEntity> {

    private final StatisticsFilter filter;

    @Override
    public Predicate toPredicate(@NotNull Root<ReceivedFineEntity> root, @NotNull CriteriaQuery<?> query, @NotNull CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();


        if (filter.getAppTeam() != null) {
            predicates.add(criteriaBuilder.equal(root.get(ReceivedFineEntity_.APP_TEAM), filter.getAppTeam()));
        }

        if (filter.getMatchId() != null) {
            Join<MatchEntity, ReceivedFineEntity> join = root.join(ReceivedFineEntity_.MATCH);
            predicates.add(criteriaBuilder.equal(join.get(MatchEntity_.ID), filter.getMatchId()));
        }

        if (filter.getPlayerId() != null) {
            Join<PlayerEntity, ReceivedFineEntity> join = root.join(ReceivedFineEntity_.PLAYER);
            predicates.add(criteriaBuilder.equal(join.get(PlayerEntity_.ID), filter.getPlayerId()));
        }

        if (filter.getSeasonId() != null) {
            if (filter.getSeasonId() != Config.ALL_SEASON_ID) {
                Join<MatchEntity, ReceivedFineEntity> join = root.join(ReceivedFineEntity_.MATCH);
                Join<SeasonEntity, MatchEntity> seasonJoin = join.join(MatchEntity_.SEASON);
                predicates.add(criteriaBuilder.equal(seasonJoin.get(SeasonEntity_.ID), filter.getSeasonId()));
            }
        }

        if (filter.getStringFilter() != null && filter.getMatchStatsOrPlayerStats() != null && !filter.getStringFilter().isEmpty()) {
            String searchString = "%" + filter.getStringFilter().trim().toLowerCase() + "%";
            if (filter.getMatchStatsOrPlayerStats()) {
                Join<MatchEntity, ReceivedFineEntity> join = root.join(ReceivedFineEntity_.MATCH);
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(join.get(MatchEntity_.NAME)), searchString));
            }
            else {
                Join<PlayerEntity, ReceivedFineEntity> join = root.join(ReceivedFineEntity_.PLAYER);
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(join.get(PlayerEntity_.NAME)), searchString));
            }

        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }
}
