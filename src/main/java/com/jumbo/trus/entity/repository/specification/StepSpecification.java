package com.jumbo.trus.entity.repository.specification;

import com.jumbo.trus.entity.*;
import com.jumbo.trus.entity.filter.GoalFilter;
import com.jumbo.trus.entity.filter.StepFilter;
import jakarta.persistence.criteria.*;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class StepSpecification implements Specification<StepUpdateEntity> {

    private final StepFilter filter;

    @Override
    public Predicate toPredicate(@NotNull Root<StepUpdateEntity> root, @NotNull CriteriaQuery<?> query, @NotNull CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();


        if (filter.getUserId() != null) {
            predicates.add(criteriaBuilder.equal(root.get(StepUpdateEntity_.USER_ID), filter.getUserId()));

        }
        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }
}
