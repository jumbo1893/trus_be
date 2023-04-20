package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.SeasonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchRepository extends PagingAndSortingRepository<MatchEntity, Long>, JpaRepository<MatchEntity, Long>, JpaSpecificationExecutor<MatchEntity> {

    @Query(value = "SELECT * from match LIMIT :limit", nativeQuery = true)
    List<MatchEntity> getAll(@Param("limit") int limit);
}
