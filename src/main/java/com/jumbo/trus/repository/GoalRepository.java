package com.jumbo.trus.repository;

import com.jumbo.trus.entity.GoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GoalRepository extends PagingAndSortingRepository<GoalEntity, Long>, JpaRepository<GoalEntity, Long>, JpaSpecificationExecutor<GoalEntity> {

    @Query(value = "SELECT * from goal LIMIT :limit", nativeQuery = true)
    List<GoalEntity> getAll(@Param("limit") int limit);

    @Modifying
    @Query(value = "DELETE from goal WHERE player_id=:#{#playerId}", nativeQuery = true)
    void deleteByPlayerId(@Param("playerId") long playerId);

    @Modifying
    @Query(value = "DELETE from goal WHERE match_id=:#{#matchId}", nativeQuery = true)
    void deleteByMatchId(@Param("matchId") long matchId);

    @Query(value = """
                SELECT g.*
                FROM football_match_player fmp
                JOIN player p ON fmp.player_id = p.football_player_id
                JOIN football_match fm ON fmp.match_id = fm.id
                JOIN match m ON m.football_match_id = fm.id
                JOIN goal g ON g.match_id = m.id AND g.player_id = p.id
                WHERE p.id = :playerId
                AND fmp.goalkeeping_minutes > 59
                AND (g.goal_number + g.assist_number) = (
                    SELECT MAX(g2.goal_number + g2.assist_number)
                    FROM goal g2
                    WHERE g2.match_id = g.match_id
                )
                ORDER BY m.date ASC
                LIMIT 1;
            
            """, nativeQuery = true)
    Optional<GoalEntity> findGoalkeeperWithMostPointsInMatch(@Param("playerId") Long playerId);

}

