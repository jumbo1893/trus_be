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
public class BeerStatsSpecification implements Specification<BeerEntity> {

    private final StatisticsFilter filter;

    @Override
    public Predicate toPredicate(@NotNull Root<BeerEntity> root, @NotNull CriteriaQuery<?> query, @NotNull CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();


        if (filter.getMatchId() != null) {
            Join<MatchEntity, BeerEntity> matchJoin = root.join(BeerEntity_.MATCH);
            predicates.add(criteriaBuilder.equal(matchJoin.get(MatchEntity_.ID), filter.getMatchId()));
        }

        if (filter.getPlayerId() != null) {

            Join<PlayerEntity, BeerEntity> matchJoin = root.join(BeerEntity_.PLAYER);
            predicates.add(criteriaBuilder.equal(matchJoin.get(PlayerEntity_.ID), filter.getPlayerId()));
        }

        if (filter.getSeasonId() != null) {
            if (filter.getSeasonId() != Config.ALL_SEASON_ID) {
                Join<MatchEntity, BeerEntity> matchJoin = root.join(BeerEntity_.MATCH);
                Join<SeasonEntity, MatchEntity> seasonJoin = matchJoin.join(MatchEntity_.SEASON);
                predicates.add(criteriaBuilder.equal(seasonJoin.get(SeasonEntity_.ID), filter.getSeasonId()));
            }
        }

        if (filter.getStringFilter() != null && filter.getMatchStatsOrPlayerStats() != null && !filter.getStringFilter().isEmpty()) {
            String searchString = "%" + filter.getStringFilter().trim().toLowerCase() + "%";
            if (filter.getMatchStatsOrPlayerStats()) {
                Join<MatchEntity, BeerEntity> matchJoin = root.join(BeerEntity_.MATCH);
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(matchJoin.get(MatchEntity_.NAME)), searchString));
            }
            else {
                Join<PlayerEntity, BeerEntity> playerJoin = root.join(BeerEntity_.PLAYER);
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(playerJoin.get(PlayerEntity_.NAME)), searchString));
            }

        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }
}
