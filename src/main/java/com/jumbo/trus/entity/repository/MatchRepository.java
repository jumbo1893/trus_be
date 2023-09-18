package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.SeasonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchRepository extends PagingAndSortingRepository<MatchEntity, Long>, JpaRepository<MatchEntity, Long>, JpaSpecificationExecutor<MatchEntity> {

    @Query(value = "SELECT * from match LIMIT :limit", nativeQuery = true)
    List<MatchEntity> getAll(@Param("limit") int limit);

    @Query(value = "SELECT * from match ORDER BY DATE DESC LIMIT :limit", nativeQuery = true)
    List<MatchEntity> getMatchesOrderByDateDesc(@Param("limit") int limit);

    @Query(value = "SELECT * from match ORDER BY DATE ASC LIMIT :limit", nativeQuery = true)
    List<MatchEntity> getMatchesOrderByDateAsc(@Param("limit") int limit);

    @Query(value = "SELECT * from match WHERE season_id=:#{#season} ORDER BY DATE DESC LIMIT 1", nativeQuery = true)
    MatchEntity getLastMatchBySeasonId(@Param("season") long seasonId);


    @Modifying
    @Query(value = "DELETE from match_players WHERE match_id=:#{#matchId}", nativeQuery = true)
    void deleteByPlayersInMatchByMatchId(@Param("matchId") long matchId);

    @Modifying
    @Query(value = "Update match SET season_id=-2 WHERE season_id=:#{#seasonId}", nativeQuery = true)
    void updateSeasonId(@Param("seasonId") long seasonId);
}
