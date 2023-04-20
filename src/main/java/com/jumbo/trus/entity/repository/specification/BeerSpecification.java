package com.jumbo.trus.entity.repository.specification;

import com.jumbo.trus.entity.*;
import com.jumbo.trus.entity.filter.BeerFilter;
import com.jumbo.trus.entity.filter.MatchFilter;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class BeerSpecification implements Specification<BeerEntity> {

    private final BeerFilter filter;

    @Override
    public Predicate toPredicate(Root<BeerEntity> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
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
            Join<MatchEntity, BeerEntity> matchJoin = root.join(BeerEntity_.MATCH);
            Join<SeasonEntity, MatchEntity> seasonJoin = matchJoin.join(MatchEntity_.SEASON);
            predicates.add(criteriaBuilder.equal(seasonJoin.get(SeasonEntity_.ID), filter.getSeasonId()));
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }
}
