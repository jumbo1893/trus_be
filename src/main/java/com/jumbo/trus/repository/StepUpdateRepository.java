package com.jumbo.trus.repository;

import com.jumbo.trus.entity.StepUpdateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface StepUpdateRepository extends PagingAndSortingRepository<StepUpdateEntity, Long>, JpaRepository<StepUpdateEntity, Long>, JpaSpecificationExecutor<StepUpdateEntity> {

    Optional<StepUpdateEntity> findByUserId(Long userId);
}

