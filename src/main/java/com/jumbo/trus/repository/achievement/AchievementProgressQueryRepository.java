package com.jumbo.trus.repository.achievement;

import com.jumbo.trus.entity.PlayerEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AchievementProgressQueryRepository extends Repository<PlayerEntity, Long> {

    @Query(value = """
            SELECT p.id AS playerId,
                   CAST(COALESCE(SUM(CASE WHEN m.app_team_id = :appTeamId
                                          THEN COALESCE(b.beer_number, 0) ELSE 0 END), 0) AS bigint) AS beerCount,
                   CAST(COALESCE(SUM(CASE WHEN m.app_team_id = :appTeamId
                                          THEN COALESCE(b.liquor_number, 0) ELSE 0 END), 0) AS bigint) AS liquorCount
            FROM player p
            LEFT JOIN beer b ON b.player_id = p.id
            LEFT JOIN match m ON m.id = b.match_id
            WHERE p.id IN (:playerIds)
              AND p.app_team_id = :appTeamId
            GROUP BY p.id
            """, nativeQuery = true)
    List<PlayerDrinkTotalsProjection> findDrinkTotals(
            @Param("playerIds") Collection<Long> playerIds,
            @Param("appTeamId") Long appTeamId
    );

    @Query(value = """
            SELECT p.id AS playerId,
                   CAST(COUNT(DISTINCT mp.match_id) AS bigint) AS metricValue
            FROM player p
            JOIN match_players mp ON mp.player_id = p.id
            JOIN match m ON m.id = mp.match_id
            WHERE p.id IN (:playerIds)
              AND p.app_team_id = :appTeamId
              AND p.fan = true
              AND m.app_team_id = :appTeamId
            GROUP BY p.id
            """, nativeQuery = true)
    List<PlayerMetricProjection> findFanAttendanceTotals(
            @Param("playerIds") Collection<Long> playerIds,
            @Param("appTeamId") Long appTeamId
    );

    @Query(value = """
            SELECT CAST(COALESCE(SUM(rf.fine_number), 0) AS bigint)
            FROM received_fine rf
            JOIN fine f ON f.id = rf.fine_id
            JOIN match m ON m.id = rf.match_id
            WHERE rf.player_id = :playerId
              AND m.app_team_id = :appTeamId
              AND f.code IN (:fineCodes)
              AND rf.fine_number > 0
            """, nativeQuery = true)
    Long sumFineCount(
            @Param("playerId") Long playerId,
            @Param("appTeamId") Long appTeamId,
            @Param("fineCodes") Collection<String> fineCodes
    );

    @Query(value = """
            SELECT CAST(COALESCE(SUM(rf.fine_number), 0) AS bigint)
            FROM received_fine rf
            JOIN fine f ON f.id = rf.fine_id
            JOIN match m ON m.id = rf.match_id
            WHERE rf.player_id = :playerId
              AND m.app_team_id = :appTeamId
              AND m.season_id = :seasonId
              AND f.code IN (:fineCodes)
              AND rf.fine_number > 0
            """, nativeQuery = true)
    Long sumFineCountInSeason(
            @Param("playerId") Long playerId,
            @Param("appTeamId") Long appTeamId,
            @Param("seasonId") Long seasonId,
            @Param("fineCodes") Collection<String> fineCodes
    );

    @Query(value = """
            SELECT CAST(COALESCE(SUM(b.beer_number), 0) AS bigint)
            FROM beer b
            JOIN match m ON m.id = b.match_id
            WHERE b.player_id = :playerId
              AND m.app_team_id = :appTeamId
              AND m.season_id = :seasonId
            """, nativeQuery = true)
    Long sumBeersInSeason(
            @Param("playerId") Long playerId,
            @Param("appTeamId") Long appTeamId,
            @Param("seasonId") Long seasonId
    );

    @Query(value = """
            WITH goal_stats AS (
                SELECT p.id AS player_id,
                       COALESCE(SUM(CASE WHEN m.season_id = :seasonId
                                         AND m.app_team_id = :appTeamId
                                         THEN COALESCE(g.goal_number, 0) ELSE 0 END), 0) AS goals,
                       COALESCE(SUM(CASE WHEN m.season_id = :seasonId
                                         AND m.app_team_id = :appTeamId
                                         THEN COALESCE(g.assist_number, 0) ELSE 0 END), 0) AS assists
                FROM player p
                LEFT JOIN goal g ON g.player_id = p.id
                LEFT JOIN match m ON m.id = g.match_id
                WHERE p.app_team_id = :appTeamId
                  AND p.fan = false
                GROUP BY p.id
            ), leader AS (
                SELECT goals, assists
                FROM goal_stats
                WHERE goals > 0
                ORDER BY goals DESC, assists DESC
                LIMIT 1
            )
            SELECT CAST(player.goals AS bigint) AS playerGoals,
                   CAST(player.assists AS bigint) AS playerAssists,
                   CAST(leader.goals AS bigint) AS leaderGoals,
                   CAST(leader.assists AS bigint) AS leaderAssists
            FROM goal_stats player
            CROSS JOIN leader
            WHERE player.player_id = :playerId
            """, nativeQuery = true)
    ScorerProgressProjection findScorerProgress(
            @Param("playerId") Long playerId,
            @Param("appTeamId") Long appTeamId,
            @Param("seasonId") Long seasonId
    );

    @Query(value = """
            WITH attendance AS (
                SELECT mp.player_id,
                       COUNT(DISTINCT mp.match_id) AS matches_count
                FROM match_players mp
                JOIN match m ON m.id = mp.match_id
                WHERE m.season_id = :seasonId
                  AND m.app_team_id = :appTeamId
                GROUP BY mp.player_id
            ), drinks AS (
                SELECT b.player_id,
                       SUM(COALESCE(b.beer_number, 0) + COALESCE(b.liquor_number, 0)) AS drink_count
                FROM beer b
                JOIN match m ON m.id = b.match_id
                WHERE m.season_id = :seasonId
                  AND m.app_team_id = :appTeamId
                GROUP BY b.player_id
            ), drink_stats AS (
                SELECT attendance.player_id,
                       attendance.matches_count,
                       COALESCE(drinks.drink_count, 0) AS drink_count
                FROM attendance
                LEFT JOIN drinks ON drinks.player_id = attendance.player_id
                WHERE attendance.matches_count > 0
            ), leader AS (
                SELECT drink_count, matches_count
                FROM drink_stats
                ORDER BY (CAST(drink_count AS numeric) / NULLIF(matches_count, 0)) DESC
                LIMIT 1
            )
            SELECT CAST(player.drink_count AS bigint) AS playerDrinks,
                   CAST(player.matches_count AS bigint) AS playerMatches,
                   CAST(leader.drink_count AS bigint) AS leaderDrinks,
                   CAST(leader.matches_count AS bigint) AS leaderMatches
            FROM drink_stats player
            CROSS JOIN leader
            WHERE player.player_id = :playerId
            """, nativeQuery = true)
    DrinkerProgressProjection findDrinkerProgress(
            @Param("playerId") Long playerId,
            @Param("appTeamId") Long appTeamId,
            @Param("seasonId") Long seasonId
    );

    interface PlayerDrinkTotalsProjection {
        Long getPlayerId();

        Long getBeerCount();

        Long getLiquorCount();
    }

    interface PlayerMetricProjection {
        Long getPlayerId();

        Long getMetricValue();
    }

    interface ScorerProgressProjection {
        Long getPlayerGoals();

        Long getPlayerAssists();

        Long getLeaderGoals();

        Long getLeaderAssists();
    }

    interface DrinkerProgressProjection {
        Long getPlayerDrinks();

        Long getPlayerMatches();

        Long getLeaderDrinks();

        Long getLeaderMatches();
    }
}
