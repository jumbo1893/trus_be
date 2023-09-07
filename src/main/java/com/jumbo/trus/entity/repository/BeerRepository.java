package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.BeerEntity;
import com.jumbo.trus.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BeerRepository extends PagingAndSortingRepository<BeerEntity, Long>, JpaRepository<BeerEntity, Long>, JpaSpecificationExecutor<BeerEntity> {

    @Query(value = "SELECT * from beer LIMIT :limit", nativeQuery = true)
    List<BeerEntity> getAll(@Param("limit") int limit);

    @Modifying
    @Query(value = "DELETE from beer WHERE player_id=:#{#playerId}", nativeQuery = true)
    void deleteByPlayerId(@Param("playerId") long playerId);

    @Modifying
    @Query(value = "DELETE from beer WHERE match_id=:#{#matchId}", nativeQuery = true)
    void deleteByMatchId(@Param("matchId") long matchId);

}

