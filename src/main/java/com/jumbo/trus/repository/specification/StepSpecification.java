package com.jumbo.trus.repository.specification;

import com.jumbo.trus.entity.StepUpdateEntity;
import com.jumbo.trus.entity.StepUpdateEntity_;
import com.jumbo.trus.entity.filter.StepFilter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
