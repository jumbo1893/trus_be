package com.jumbo.trus.repository;

import com.jumbo.trus.entity.StepConsentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StepConsentRepository extends JpaRepository<StepConsentEntity, Long> {
    Optional<StepConsentEntity> findByUserIdAndAppTeamId(Long userId, Long appTeamId);
}
