package com.jumbo.trus.repository;

import com.jumbo.trus.entity.MatchWeatherEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface MatchWeatherRepository extends PagingAndSortingRepository<MatchWeatherEntity, Long>, JpaRepository<MatchWeatherEntity, Long>, JpaSpecificationExecutor<MatchWeatherEntity> {

    Optional<MatchWeatherEntity> findByFootballMatchId(Long footballMatchId);


}

