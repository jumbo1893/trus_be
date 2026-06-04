package com.jumbo.trus.repository;

import com.jumbo.trus.dto.goal.IPlayerGoalStats;
import com.jumbo.trus.dto.player.stats.projection.IPlayerGoalCountProjection;
import com.jumbo.trus.dto.goal.projection.IGoalAttendanceDetail;
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

    @Query(value = """
        SELECT
            p.id AS playerId,
            p.name AS playerName,
            p.birthday AS playerBirthday,
            p.fan AS fan,
            p.active AS active,
            m.id AS matchId,
            m.name AS matchName,
            m.date AS matchDate,
            m.season_id AS seasonId,
            m.home AS home,
            m.home_goal_number AS homeGoalNumber,
            m.away_goal_number AS awayGoalNumber,
            CAST(COALESCE(SUM(g.goal_number), 0) AS int) AS goalNumber,
            CAST(COALESCE(SUM(g.assist_number), 0) AS int) AS assistNumber
        FROM match_players mp
        JOIN player p
          ON p.id = mp.player_id
        JOIN match m
          ON m.id = mp.match_id
        LEFT JOIN goal g
          ON g.player_id = p.id
         AND g.match_id = m.id
         AND g.app_team_id = :appTeamId
        WHERE m.app_team_id = :appTeamId
          AND (:seasonId = -3 OR m.season_id = :seasonId)
          AND (:playerId IS NULL OR p.id = :playerId)
          AND (:matchId IS NULL OR m.id = :matchId)
          AND (
              :stringFilter IS NULL
              OR LOWER(p.name) LIKE LOWER(CONCAT('%', :stringFilter, '%'))
              OR LOWER(m.name) LIKE LOWER(CONCAT('%', :stringFilter, '%'))
          )
        GROUP BY
            p.id,
            p.name,
            p.birthday,
            p.fan,
            p.active,
            m.id,
            m.name,
            m.date,
            m.season_id,
            m.home,
            m.home_goal_number,
            m.away_goal_number
        ORDER BY
            m.date DESC,
            p.name ASC
        """, nativeQuery = true)
    List<IGoalAttendanceDetail> findGoalAttendanceDetails(
            @Param("appTeamId") Long appTeamId,
            @Param("seasonId") Long seasonId,
            @Param("playerId") Long playerId,
            @Param("matchId") Long matchId,
            @Param("stringFilter") String stringFilter
    );


    @Query("""
        SELECT
            COALESCE(SUM(g.goalNumber), 0) AS totalGoals,
            COALESCE(SUM(g.assistNumber), 0) AS totalAssists
        FROM goal g
        WHERE g.appTeam.id = :appTeamId
          AND g.player.id = :playerId
    """)
    IPlayerGoalCountProjection sumForPlayerAndAppTeam(
            @Param("playerId") Long playerId,
            @Param("appTeamId") Long appTeamId
    );

    @Query("""
        SELECT
            COALESCE(SUM(g.goalNumber), 0) AS totalGoals,
            COALESCE(SUM(g.assistNumber), 0) AS totalAssists
        FROM goal g
        WHERE g.appTeam.id = :appTeamId
          AND g.player.id = :playerId
          AND g.match.season.id = :seasonId
    """)
    IPlayerGoalCountProjection sumForPlayerAndAppTeamAndSeason(
            @Param("playerId") Long playerId,
            @Param("appTeamId") Long appTeamId,
            @Param("seasonId") Long seasonId
    );

}

