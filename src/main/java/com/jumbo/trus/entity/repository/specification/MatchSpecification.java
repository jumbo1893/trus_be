package com.jumbo.trus.entity.repository.specification;

import com.jumbo.trus.entity.*;
import com.jumbo.trus.entity.filter.MatchFilter;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class MatchSpecification implements Specification<MatchEntity> {

    private final MatchFilter filter;

    @Override
    public Predicate toPredicate(Root<MatchEntity> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();

        if (filter.getName() != null) {
            predicates.add(criteriaBuilder.equal(root.get(MatchEntity_.NAME), filter.getName()));
        }

        if (filter.getDate() != null) {
            predicates.add(criteriaBuilder.equal(root.get(MatchEntity_.DATE), filter.getDate()));
        }

        if (filter.getSeasonId() != null) {
            Join<SeasonEntity, MatchEntity> seasonJoin = root.join(MatchEntity_.SEASON);
            predicates.add(criteriaBuilder.equal(seasonJoin.get(SeasonEntity_.ID), filter.getSeasonId()));
        }

        if (filter.getPlayerList() != null) {
            for (Long playerId : filter.getPlayerList()) {
                Join<PlayerEntity, MatchEntity> playerJoin = root.join(MatchEntity_.PLAYER_LIST);
                predicates.add(playerJoin.get(PlayerEntity_.ID).in(playerId));
            }
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }
}
