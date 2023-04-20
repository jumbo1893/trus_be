package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.BeerEntity;
import com.jumbo.trus.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BeerRepository extends PagingAndSortingRepository<BeerEntity, Long>, JpaRepository<BeerEntity, Long>, JpaSpecificationExecutor<BeerEntity> {

    @Query(value = "SELECT * from beer LIMIT :limit", nativeQuery = true)
    List<BeerEntity> getAll(@Param("limit") int limit);

    /*@Query(value = "SELECT * from beer WHERE match_id = 'match' AND player_id  = 'player' LIMIT 1", nativeQuery = true)
    BeerEntity getBeerByMatchAndPlayer(@Param("match") Long matchId, @Param("player") Long playerId);*/

}

