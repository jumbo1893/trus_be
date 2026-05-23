package com.jumbo.trus.repository;

import com.jumbo.trus.dto.goal.IPlayerGoalStats;
import com.jumbo.trus.entity.GoalEntity;
import org.springframework.data.domain.Pageable;
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
            JOIN player p 
              ON p.football_player_id = fmp.player_id
            JOIN football_match fm 
              ON fm.id = fmp.match_id
            JOIN match m 
              ON m.football_match_id = fm.id
            JOIN goal g 
              ON g.match_id = m.id 
             AND g.player_id = p.id
            WHERE p.id = :playerId
              AND m.app_team_id = :appTeamId
              AND fmp.goalkeeping_minutes > 59
              AND COALESCE(g.goal_number, 0) + COALESCE(g.assist_number, 0) > 0
              AND COALESCE(g.goal_number, 0) + COALESCE(g.assist_number, 0) = (
                  SELECT MAX(
                      COALESCE(g2.goal_number, 0) + COALESCE(g2.assist_number, 0)
                  )
                  FROM goal g2
                  WHERE g2.match_id = g.match_id
              )
            ORDER BY m.date ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<GoalEntity> findGoalkeeperWithMostPointsInMatch(
            @Param("playerId") Long playerId,
            @Param("appTeamId") Long appTeamId
    );

    @Query("""
        SELECT
            g.player.id AS playerId,
            SUM(g.goalNumber) AS goalNumber,
            SUM(g.assistNumber) AS assistNumber
        FROM goal g
        WHERE g.appTeam.id = :appTeamId
        GROUP BY g.player.id
        ORDER BY
            SUM(g.goalNumber) + SUM(g.assistNumber) DESC,
            SUM(g.goalNumber) DESC,
            g.player.id ASC
        """)
    List<IPlayerGoalStats> findTopGoalStatsByAppTeam(
            @Param("appTeamId") Long appTeamId,
            Pageable pageable
    );

    @Query("""
        SELECT
            g.player.id AS playerId,
            SUM(g.goalNumber) AS goalNumber,
            SUM(g.assistNumber) AS assistNumber
        FROM goal g
        WHERE g.appTeam.id = :appTeamId
          AND g.match.season.id = :seasonId
        GROUP BY g.player.id
        ORDER BY
            SUM(g.goalNumber) + SUM(g.assistNumber) DESC,
            SUM(g.goalNumber) DESC,
            g.player.id ASC
        """)
    List<IPlayerGoalStats> findTopGoalStatsByAppTeamAndSeason(
            @Param("appTeamId") Long appTeamId,
            @Param("seasonId") Long seasonId,
            Pageable pageable
    );

}

