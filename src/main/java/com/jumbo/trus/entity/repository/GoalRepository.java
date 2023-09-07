package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.GoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GoalRepository extends PagingAndSortingRepository<GoalEntity, Long>, JpaRepository<GoalEntity, Long>, JpaSpecificationExecutor<GoalEntity> {

    @Query(value = "SELECT * from goal LIMIT :limit", nativeQuery = true)
    List<GoalEntity> getAll(@Param("limit") int limit);

    @Modifying
    @Query(value = "DELETE from goal WHERE player_id=:#{#playerId}", nativeQuery = true)
    void deleteByPlayerId(@Param("playerId") long playerId);

    @Modifying
    @Query(value = "DELETE from goal WHERE match_id=:#{#matchId}", nativeQuery = true)
    void deleteByMatchId(@Param("matchId") long matchId);
}

