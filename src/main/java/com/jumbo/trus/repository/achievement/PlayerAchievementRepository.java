package com.jumbo.trus.repository.achievement;

import com.jumbo.trus.dto.player.stats.projection.IPlayerAchievementCountProjection;
import com.jumbo.trus.dto.achievement.IPlayerAchievementStats;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.achievement.PlayerAchievementEntity;
import com.jumbo.trus.service.achievement.helper.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerAchievementRepository extends JpaRepository<PlayerAchievementEntity, Long> {

    Optional<PlayerAchievementEntity> findByPlayerIdAndAchievementId(Long playerId, Long achievementId);

    boolean existsByPlayerIdAndAchievementId(Long playerId, Long achievementId);

    Optional<PlayerAchievementEntity> findByPlayerIdAndAchievementIdAndAccomplished(Long playerId, Long achievementId, boolean accomplished);

    @Query("""
        SELECT pa.achievement.id,
               SUM(CASE WHEN pa.player.fan = false THEN 1 ELSE 0 END),
               COUNT(DISTINCT pa.player.id)
        FROM PlayerAchievementEntity pa
        WHERE pa.player.appTeam.id = :appTeamId
          AND pa.accomplished = true
          AND pa.player.deleted = false
        GROUP BY pa.achievement.id
        """)
    List<Object[]> countAccomplishedStatsByAchievementForTeam(
            @Param("appTeamId") Long appTeamId
    );

    @Query("""
                SELECT
                    COUNT(pae) AS firstNumber,
                    COALESCE(SUM(CASE WHEN pae.accomplished = true THEN 1 ELSE 0 END), 0) AS secondNumber
                FROM PlayerAchievementEntity pae
                WHERE pae.player.id IN :playerIds
                AND pae.achievement.id = :achievementId
            """)
    IMatchIdNumberOneNumberTwo countAchievements(@Param("playerIds") List<Long> playerIds, @Param("achievementId") Long achievementId);

    @Query("""
                SELECT pae.player FROM PlayerAchievementEntity pae
                WHERE pae.achievement.id = :achievementId
                AND pae.accomplished = true
                AND pae.player.id IN :playerIds
            """)
    List<PlayerEntity> findAccomplishedPlayersByAchievement(@Param("achievementId") Long achievementId, @Param("playerIds") List<Long> playerIds);

    @Query("""
                SELECT pae
                FROM PlayerAchievementEntity pae
                WHERE pae.player.id IN :playerIds
                  AND pae.accomplished = true
                  AND pae.accomplishedDate IS NOT NULL
                ORDER BY pae.accomplishedDate DESC
            """)
    List<PlayerAchievementEntity> findLastAccomplishedByPlayers(
            @Param("playerIds") List<Long> playerIds,
            Pageable pageable
    );


    List<PlayerAchievementEntity> findAllByPlayerId(Long playerId);

    List<PlayerAchievementEntity> findAllByPlayerIdIn(List<Long> playerIds);

    @Query("""
        SELECT
            COUNT(pa) AS totalAchievements,
            COALESCE(SUM(CASE WHEN pa.accomplished = true THEN 1 ELSE 0 END), 0) AS accomplishedAchievements
        FROM PlayerAchievementEntity pa
        WHERE pa.player.id = :playerId
    """)
    IPlayerAchievementCountProjection countStatsByPlayerId(@Param("playerId") Long playerId);


    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM PlayerAchievementEntity i WHERE i.player.id = :playerId AND i.achievement.id = :achievementId")
    boolean existsByPlayerAndAchievement(@Param("playerId") long playerId, @Param("achievementId") long achievementId);


    @Query(value = """
            SELECT b.match_id AS matchId, b.beer_number AS beerNumber,
            b.liquor_number AS liquorNumber, g.goal_number AS goalNumber
            FROM beer b
            JOIN goal g ON b.match_id = g.match_id AND b.player_id = g.player_id
            JOIN match m ON m.id = b.match_id
            WHERE b.beer_number+b.liquor_number = g.goal_number
            AND g.goal_number > 0
            AND b.player_id = :playerId
            ORDER BY m.date ASC
            LIMIT 1
            """, nativeQuery = true)
    IGoalBeerMatch getFirstMatchWithSameGoalsAndBeers(@Param("playerId") Long playerId);

    @Query(value = """
            SELECT b.match_id AS matchId,
                   b.beer_number AS beerNumber,
                   b.liquor_number AS liquorNumber,
                   g.goal_number AS goalNumber,
                   r.fine_number AS fineNumber
            FROM beer b
            JOIN goal g ON b.match_id = g.match_id AND b.player_id = g.player_id
            JOIN received_fine r ON b.match_id = r.match_id AND b.player_id = r.player_id
            JOIN match m ON m.id = b.match_id
            JOIN fine f ON r.fine_id = f.id
            WHERE b.beer_number > 0
              AND b.liquor_number > 0
              AND g.goal_number > 0
              AND f.name = :fineName
              AND r.fine_number > 0
              AND b.player_id = :playerId
            ORDER BY m.date ASC
            LIMIT 1
            """, nativeQuery = true)
    IGoalBeerFineMatch getFirstMatchWithGoalYellowBeerAndLiquor(
            @Param("playerId") Long playerId,
            @Param("fineName") String fineName
    );

    @Query(value = """
            SELECT r.match_id
                    FROM player p
            		JOIN received_fine r ON p.id = r.player_id
                    JOIN fine f ON r.fine_id = f.id
                    JOIN match m ON m.id = r.match_id
                    JOIN football_match fm ON m.football_match_id = fm.id
                    JOIN football_match_player fmp ON fm.id = fmp.match_id AND p.football_player_id = fmp.player_id
                    WHERE (fmp.hattrick is true OR fmp.clean_sheet is true)
                    AND f.name = :fineName
                    AND r.fine_number > 0
                    AND r.player_id = :playerId
                    ORDER BY m.date ASC
                    LIMIT 1
            """, nativeQuery = true)
    Long getFirstMatchWithHangoverAndHattrickOrCleanSheet(@Param("playerId") Long playerId, @Param("fineName") String fineName);

    @Query(value = """
            SELECT r.match_id AS matchId,
            SUM(CASE WHEN f.name = :firstFineName THEN r.fine_number ELSE 0 END) AS firstNumber,
            SUM(CASE WHEN f.name = :secondFineName THEN r.fine_number ELSE 0 END) AS secondNumber
            FROM received_fine r
            JOIN fine f ON r.fine_id = f.id
            JOIN match m ON r.match_id = m.id
            WHERE r.player_id = :playerId
            GROUP BY r.match_id, m.date
            HAVING SUM(CASE WHEN f.name = :firstFineName THEN r.fine_number ELSE 0 END) >= :firstFineCount
            AND SUM(CASE WHEN f.name = :secondFineName THEN r.fine_number ELSE 0 END) >= :secondFineCount
            ORDER BY m.date ASC
            LIMIT 1;
            
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo getFirstMatchWithAtLeastXFines(@Param("playerId") Long playerId, @Param("firstFineName") String firstFineName, @Param("secondFineName") String secondFineName,
                                                              @Param("firstFineCount") int firstFineCount, @Param("secondFineCount") int secondFineCount);

    @Query(value = """
            SELECT r.match_id AS matchId,
                   r.player_id AS playerId,
                   CAST(best_player_matches.isBestPlayer AS INT) AS firstNumber,
                   SUM(CASE WHEN f.name = :firstFineName THEN r.fine_number ELSE 0 END) AS secondNumber
            FROM received_fine r
            JOIN fine f ON r.fine_id = f.id
            JOIN match m ON r.match_id = m.id
            JOIN player p ON r.player_id = p.id
            JOIN football_match fm ON fm.id = m.football_match_id
            JOIN (
                SELECT fmp.match_id, fmp.player_id,
                       CASE WHEN MAX(CASE WHEN fmp.best_player = true THEN 1 ELSE 0 END) = 1 THEN true ELSE false END AS isBestPlayer
                FROM football_match_player fmp
                GROUP BY fmp.match_id, fmp.player_id
            ) best_player_matches ON fm.id = best_player_matches.match_id AND p.football_player_id = best_player_matches.player_id
            WHERE p.id = :playerId
            GROUP BY r.match_id, r.player_id, best_player_matches.isBestPlayer, m.date
            HAVING best_player_matches.isBestPlayer = true
               AND SUM(CASE WHEN f.name = :firstFineName THEN r.fine_number ELSE 0 END) >= 1
            ORDER BY m.date ASC
            LIMIT 1;
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo getFirstMatchWherePlayerIsBestPlayerWithFine(@Param("playerId") Long playerId, @Param("firstFineName") String firstFineName);

    @Query(value = """
            SELECT r.match_id AS matchId,
                   r.player_id AS playerId,
                   CAST(best_player_matches.isBestPlayer AS INT) AS firstNumber,
                   SUM(CASE WHEN f.name IN (:firstFineName, :firstFineName2, :firstFineName3) THEN r.fine_number ELSE 0 END) AS secondNumber
            FROM received_fine r
            JOIN fine f ON r.fine_id = f.id
            JOIN match m ON r.match_id = m.id
            JOIN player p ON r.player_id = p.id
            JOIN football_match fm ON fm.id = m.football_match_id
            JOIN (
                SELECT fmp.match_id, fmp.player_id,
                       CASE WHEN MAX(CASE WHEN fmp.best_player = true THEN 1 ELSE 0 END) = 1 THEN true ELSE false END AS isBestPlayer
                FROM football_match_player fmp
                GROUP BY fmp.match_id, fmp.player_id
            ) best_player_matches ON fm.id = best_player_matches.match_id AND p.football_player_id = best_player_matches.player_id
            WHERE p.id = :playerId
            GROUP BY r.match_id, r.player_id, best_player_matches.isBestPlayer, m.date
            HAVING best_player_matches.isBestPlayer = true
               AND SUM(CASE WHEN f.name IN (:firstFineName, :firstFineName2, :firstFineName3) THEN r.fine_number ELSE 0 END) >= 1
            ORDER BY m.date ASC
            LIMIT 1;
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo getFirstMatchWherePlayerIsBestPlayerWithFine(@Param("playerId") Long playerId, @Param("firstFineName") String firstFineName,
                                                                            @Param("firstFineName2") String firstFineName2, @Param("firstFineName3") String firstFineName3);

    @Query(value = """
            SELECT r.match_id AS matchId,
                   SUM(CASE WHEN f.name IN (:firstFineName, :firstFineName2, :firstFineName3) THEN r.fine_number ELSE 0 END) AS firstNumber,
                   SUM(CASE WHEN f.name = :secondFineName THEN r.fine_number ELSE 0 END) AS secondNumber
            FROM received_fine r
            JOIN fine f ON r.fine_id = f.id
            JOIN match m ON r.match_id = m.id
            WHERE r.player_id = :playerId
            GROUP BY r.match_id, m.date
            HAVING SUM(CASE WHEN f.name IN (:firstFineName, :firstFineName2, :firstFineName3) THEN r.fine_number ELSE 0 END) >= 1
               AND SUM(CASE WHEN f.name = :secondFineName THEN r.fine_number ELSE 0 END) >= :secondFineCount
            ORDER BY m.date ASC
            LIMIT 1;
            
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo getFirstMatchWithAtLeastOneOfFinesAndXSecondFines(@Param("playerId") Long playerId, @Param("firstFineName") String firstFineName,
                                                                                 @Param("firstFineName2") String firstFineName2, @Param("firstFineName3") String firstFineName3,
                                                                                 @Param("secondFineName") String secondFineName, @Param("secondFineCount") int secondFineCount);

    @Query(value = """
            SELECT r.match_id
            FROM player p
            JOIN received_fine r ON p.id = r.player_id
            JOIN fine f ON r.fine_id = f.id
            JOIN match m ON m.id = r.match_id
            JOIN football_match fm ON m.football_match_id = fm.id
            WHERE f.name = :fineName
            AND r.fine_number > 0
            AND r.player_id = :playerId
            AND (
                (:teamId = fm.home_team_id AND fm.home_goal_number > fm.away_goal_number)
                OR
                (:teamId = fm.away_team_id AND fm.away_goal_number > fm.home_goal_number)
            )
            ORDER BY m.date ASC
            LIMIT 1;
            """, nativeQuery = true)
    Long getFirstWinningMatchWithFine(@Param("playerId") Long playerId, @Param("fineName") String fineName, @Param("teamId") Long teamId);

    @Query(value = """
                SELECT r.match_id
                FROM received_fine r
                JOIN fine f ON r.fine_id = f.id
                JOIN match m ON m.id = r.match_id
                WHERE r.player_id = :playerId
                AND f.name IN (
                    'Pozdní příchod do začátku',
                    'Pozdní příchod po 10. minutě',
                    'Pozdní příchod po začátku',
                    'Nepříchod'
                )
                GROUP BY r.match_id, m.date
                HAVING COUNT(DISTINCT f.id) >= 3
                ORDER BY m.date ASC
                LIMIT 1;
            
            """, nativeQuery = true)
    Long findFirstMatchWherePlayerReceivedAtLeastXFines(@Param("playerId") Long playerId);

    @Query(value = """
                WITH FineCounts AS (
                SELECT r.match_id,
                       m.date,
                       SUM(CASE WHEN f.name IN (
                            'Pozdní příchod do začátku',
                            'Pozdní příchod po 10. minutě',
                            'Pozdní příchod po začátku',
                            'Nepříchod'
                       ) THEN r.fine_number ELSE 0 END) AS total_fines
                FROM received_fine r
                JOIN fine f ON r.fine_id = f.id
                JOIN match m ON m.id = r.match_id
                JOIN season s ON m.season_id = s.id
                WHERE r.player_id = :playerId
                  AND s.id = :seasonId
                GROUP BY r.match_id, m.date
                ORDER BY m.date ASC
            ),
            CumulativeFines AS (
                SELECT match_id, date,
                       SUM(total_fines) OVER (ORDER BY date ASC ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_fine_count
                FROM FineCounts
            )
            SELECT match_id
            FROM CumulativeFines
            WHERE running_fine_count >= 3
            ORDER BY date ASC
            LIMIT 1;
            """, nativeQuery = true)
    Long findFirstMatchInSeasonWithLateArrival(@Param("playerId") Long playerId, @Param("seasonId") Long seasonId);

    @Query("""
                SELECT pae FROM PlayerAchievementEntity pae
                WHERE pae.match.id = (
                    SELECT pae2.match.id
                    FROM PlayerAchievementEntity pae2
                    WHERE pae2.player.id = :playerId
                    GROUP BY pae2.match.id
                    HAVING COUNT(pae2.id) > 1
                    ORDER BY MIN(pae2.id) ASC
                    LIMIT 1
                )
                AND pae.player.id = :playerId
            """)
    List<PlayerAchievementEntity> findFirstDuplicateMatchAchievements(@Param("playerId") Long playerId);

    @Query(value = """
            SELECT
                        COUNT(CASE WHEN f.name = :firstFineName THEN r.fine_number END) AS firstNumber,
                        COUNT(CASE WHEN f.name = :secondFineName THEN r.fine_number END) AS secondNumber,
                        MAX(m.id) AS matchId
                    FROM received_fine r
                    JOIN fine f ON r.fine_id = f.id
                    JOIN match m ON r.match_id = m.id
                    JOIN season s ON m.season_id = s.id
                    WHERE r.player_id = :playerId
                    AND s.id = :seasonId
                    AND f.name IN (:firstFineName, :secondFineName)
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findLastMatchInSeasonWherePlayerGetsTwoFines(@Param("playerId") Long playerId, @Param("firstFineName") String firstFineName,
                                                                            @Param("secondFineName") String secondFineName, @Param("seasonId") Long seasonId);

    @Query(value = """
            SELECT r.match_id AS matchId,
                        SUM(CASE WHEN f.name = :firstFineName THEN r.fine_number ELSE 0 END) AS firstNumber,
                        SUM(b.beer_number) AS secondNumber
                        FROM received_fine r
                        JOIN fine f ON r.fine_id = f.id
                        JOIN match m ON r.match_id = m.id
            			JOIN beer b ON r.match_id = b.match_id AND r.player_id = b.player_id
                        WHERE r.player_id = :playerId
                        GROUP BY r.match_id, m.date
                        HAVING SUM(CASE WHEN f.name = :firstFineName THEN r.fine_number ELSE 0 END) >= 1
                        AND SUM(b.beer_number) >= 1
                        ORDER BY m.date ASC
                        LIMIT 1;
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findFirstMatchWhereFineExistsAndPlayerHasBeer(@Param("playerId") Long playerId, @Param("firstFineName") String firstFineName);

    @Query(value = """
            SELECT r.match_id AS matchId,
                        SUM(CASE WHEN f.name = :firstFineName THEN r.fine_number ELSE 0 END) AS firstNumber,
                        SUM(b.liquor_number) AS secondNumber
                        FROM received_fine r
                        JOIN fine f ON r.fine_id = f.id
                        JOIN match m ON r.match_id = m.id
            			JOIN beer b ON r.match_id = b.match_id AND r.player_id = b.player_id
                        WHERE r.player_id = :playerId
                        GROUP BY r.match_id, m.date
                        HAVING SUM(CASE WHEN f.name = :firstFineName THEN r.fine_number ELSE 0 END) >= 1
                        AND SUM(b.liquor_number) >= 1
                        ORDER BY m.date ASC
                        LIMIT 1;
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findFirstMatchWhereFineExistsAndPlayerHasLiquor(@Param("playerId") Long playerId, @Param("firstFineName") String firstFineName);

    @Query(value = """
            SELECT
            			SUM(g.goal_number) AS firstNumber,
                        SUM(b.beer_number) AS secondNumber
                        FROM match m
                        JOIN goal g ON m.id = g.match_id AND g.player_id = :playerId
            			JOIN beer b ON m.id = b.match_id AND b.player_id = :playerId
            			WHERE m.season_id = :seasonId
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findBeersAndGoalsInSeason(@Param("playerId") Long playerId, @Param("seasonId") Long seasonId);

    @Query("""
                SELECT
                    pae.player.id AS playerId,
                    COALESCE(SUM(CASE WHEN pae.accomplished = true THEN 1 ELSE 0 END), 0) AS accomplishedCount,
                    COALESCE(SUM(CASE WHEN pae.accomplished IS NULL OR pae.accomplished = false THEN 1 ELSE 0 END), 0) AS notAccomplishedCount
                FROM PlayerAchievementEntity pae
                WHERE pae.player.appTeam.id = :appTeamId
                GROUP BY pae.player.id
                ORDER BY
                    COALESCE(SUM(CASE WHEN pae.accomplished = true THEN 1 ELSE 0 END), 0) DESC,
                    pae.player.id ASC
            """)
    List<IPlayerAchievementStats> findTopAchievementStatsByAppTeam(
            @Param("appTeamId") Long appTeamId,
            Pageable pageable
    );

    @Query("""
                SELECT pae
                FROM PlayerAchievementEntity pae
                WHERE pae.accomplished = true
                  AND pae.player.appTeam.id = :appTeamId
                  AND (
                        (:matchId IS NOT NULL AND pae.match.id = :matchId)
                        OR
                        (:footballMatchId IS NOT NULL AND pae.footballMatch.id = :footballMatchId)
                      )
            """)
    List<PlayerAchievementEntity> findAllAccomplishedByMatchOrFootballMatch(
            @Param("appTeamId") Long appTeamId,
            @Param("matchId") Long matchId,
            @Param("footballMatchId") Long footballMatchId
    );

    @Query(value = """
            WITH attendees AS (
                SELECT mp.match_id, COUNT(DISTINCT mp.player_id) AS player_count
                FROM match_players mp
                JOIN player p ON p.id = mp.player_id
                JOIN match m ON m.id = mp.match_id
                WHERE p.fan = false
                  AND m.app_team_id = :appTeamId
                GROUP BY mp.match_id
            ), third_half AS (
                SELECT rf.match_id, COUNT(DISTINCT rf.player_id) AS fined_count
                FROM received_fine rf
                JOIN fine f ON f.id = rf.fine_id
                JOIN player p ON p.id = rf.player_id
                JOIN match m ON m.id = rf.match_id
                WHERE f.name = 'Třetí poločas'
                  AND rf.fine_number > 0
                  AND p.fan = false
                  AND m.app_team_id = :appTeamId
                GROUP BY rf.match_id
            )
            SELECT mp.match_id AS matchId,
                   CAST(a.player_count AS int) AS firstNumber,
                   CAST(COALESCE(th.fined_count, 0) AS int) AS secondNumber
            FROM match_players mp
            JOIN match m ON m.id = mp.match_id
            JOIN player p ON p.id = mp.player_id
            JOIN attendees a ON a.match_id = mp.match_id
            LEFT JOIN third_half th ON th.match_id = mp.match_id
            WHERE mp.player_id = :playerId
              AND p.fan = false
              AND m.app_team_id = :appTeamId
              AND a.player_count > 0
              AND a.player_count = COALESCE(th.fined_count, 0)
            ORDER BY m.date ASC
            LIMIT 1
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findKlubSracu(@Param("playerId") Long playerId,
                                             @Param("appTeamId") Long appTeamId);

    @Query(value = """
            WITH attendees AS (
                SELECT mp.match_id, COUNT(DISTINCT mp.player_id) AS player_count
                FROM match_players mp
                JOIN player p ON p.id = mp.player_id
                JOIN match m ON m.id = mp.match_id
                WHERE p.fan = false
                  AND m.app_team_id = :appTeamId
                GROUP BY mp.match_id
            ), third_half AS (
                SELECT rf.match_id, COUNT(DISTINCT rf.player_id) AS fined_count
                FROM received_fine rf
                JOIN fine f ON f.id = rf.fine_id
                JOIN player p ON p.id = rf.player_id
                JOIN match m ON m.id = rf.match_id
                WHERE f.name = 'Třetí poločas'
                  AND rf.fine_number > 0
                  AND p.fan = false
                  AND m.app_team_id = :appTeamId
                GROUP BY rf.match_id
            )
            SELECT mp.match_id AS matchId,
                   CAST(a.player_count AS int) AS firstNumber,
                   CAST(COALESCE(th.fined_count, 0) AS int) AS secondNumber
            FROM match_players mp
            JOIN match m ON m.id = mp.match_id
            JOIN player p ON p.id = mp.player_id
            JOIN attendees a ON a.match_id = mp.match_id
            LEFT JOIN third_half th ON th.match_id = mp.match_id
            WHERE mp.player_id = :playerId
              AND p.fan = false
              AND m.app_team_id = :appTeamId
              AND a.player_count - COALESCE(th.fined_count, 0) = 1
              AND NOT EXISTS (
                  SELECT 1
                  FROM received_fine rf
                  JOIN fine f ON f.id = rf.fine_id
                  WHERE rf.match_id = mp.match_id
                    AND rf.player_id = :playerId
                    AND f.name = 'Třetí poločas'
                    AND rf.fine_number > 0
              )
            ORDER BY m.date ASC
            LIMIT 1
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findOsamelyDrzak(@Param("playerId") Long playerId,
                                                @Param("appTeamId") Long appTeamId);

    // Ve dvou se to lépe táhne
    @Query(value = """
            WITH player_matches AS (
                SELECT mp.match_id, mp.player_id
                FROM match_players mp
                JOIN player p ON p.id = mp.player_id
                JOIN match m ON m.id = mp.match_id
                WHERE p.fan = false
                  AND m.app_team_id = :appTeamId
            ), third_half_players AS (
                SELECT DISTINCT rf.match_id, rf.player_id
                FROM received_fine rf
                JOIN fine f ON f.id = rf.fine_id
                JOIN match m ON m.id = rf.match_id
                WHERE f.name = 'Třetí poločas'
                  AND rf.fine_number > 0
                  AND m.app_team_id = :appTeamId
            ), not_fined AS (
                SELECT pm.match_id, pm.player_id
                FROM player_matches pm
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM third_half_players th
                    WHERE th.match_id = pm.match_id
                      AND th.player_id = pm.player_id
                )
            ), not_fined_with_beer AS (
                SELECT nf.match_id, nf.player_id
                FROM not_fined nf
                JOIN beer b ON b.match_id = nf.match_id
                           AND b.player_id = nf.player_id
                           AND b.beer_number > 0
            ), match_counts AS (
                SELECT pm.match_id,
                       COUNT(DISTINCT pm.player_id) AS player_count,
                       COUNT(DISTINCT th.player_id) AS fined_count,
                       COUNT(DISTINCT nf.player_id) AS not_fined_count,
                       COUNT(DISTINCT nfb.player_id) AS not_fined_with_beer_count
                FROM player_matches pm
                LEFT JOIN third_half_players th
                       ON th.match_id = pm.match_id
                      AND th.player_id = pm.player_id
                LEFT JOIN not_fined nf
                       ON nf.match_id = pm.match_id
                      AND nf.player_id = pm.player_id
                LEFT JOIN not_fined_with_beer nfb
                       ON nfb.match_id = pm.match_id
                      AND nfb.player_id = pm.player_id
                GROUP BY pm.match_id
            ), player_drinks AS (
                SELECT b.match_id,
                       b.player_id,
                       SUM(COALESCE(b.beer_number, 0)) AS beer_number,
                       SUM(COALESCE(b.liquor_number, 0)) AS liquor_number
                FROM beer b
                WHERE b.player_id = :playerId
                GROUP BY b.match_id, b.player_id
            ), second_player AS (
                SELECT nf.match_id,
                       p.name AS second_player_name
                FROM not_fined nf
                JOIN player p ON p.id = nf.player_id
                WHERE nf.player_id <> :playerId
            )
            SELECT nf.match_id AS matchId,
                   CAST(mc.not_fined_count AS int) AS firstNumber,
                   CAST(COALESCE(pd.beer_number, 0) AS int) AS secondNumber,
                   CAST(COALESCE(pd.liquor_number, 0) AS int) AS thirdNumber,
                   sp.second_player_name AS text
            FROM not_fined nf
            JOIN match_counts mc ON mc.match_id = nf.match_id
            JOIN match m ON m.id = nf.match_id
            JOIN player_drinks pd ON pd.match_id = nf.match_id
                                  AND pd.player_id = nf.player_id
            JOIN second_player sp ON sp.match_id = nf.match_id
            WHERE nf.player_id = :playerId
              AND mc.not_fined_count = 2
              AND mc.not_fined_with_beer_count = 2
              AND COALESCE(pd.beer_number, 0) > 0
            ORDER BY m.date ASC
            LIMIT 1
            """, nativeQuery = true)
    IMatchIdThreeNumbersAndText findVeDvouSeToLepeTahne(
            @Param("playerId") Long playerId,
            @Param("appTeamId") Long appTeamId
    );

    @Query(value = """
            WITH goal_stats AS (
                SELECT g.player_id,
                       SUM(COALESCE(g.goal_number, 0)) AS goals,
                       SUM(COALESCE(g.assist_number, 0)) AS assist
                FROM goal g
                JOIN match m ON m.id = g.match_id
                JOIN player p ON p.id = g.player_id
                WHERE m.season_id = :seasonId
                  AND m.app_team_id = :appTeamId
                  AND p.fan = false
                GROUP BY g.player_id
            ), ranked AS (
                SELECT player_id,
                       goals,
                       assist,
                       RANK() OVER (ORDER BY goals DESC, assist DESC) AS rank_position
                FROM goal_stats
                WHERE goals > 0
            )
            SELECT NULL AS matchId,
                   CAST(goals AS int) AS firstNumber,
                   CAST(assist AS int) AS secondNumber
            FROM ranked
            WHERE player_id = :playerId
              AND rank_position = 1
            LIMIT 1
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findStrelecInSeason(@Param("playerId") Long playerId,
                                                   @Param("seasonId") Long seasonId,
                                                   @Param("appTeamId") Long appTeamId);

    // Fotr je lotr
    @Query(value = """
            WITH birth_total AS (
                SELECT rf.player_id,
                       SUM(COALESCE(rf.fine_number, 0)) AS birth_count
                FROM received_fine rf
                JOIN fine f ON f.id = rf.fine_id
                JOIN match m ON m.id = rf.match_id
                WHERE rf.player_id = :playerId
                  AND m.app_team_id = :appTeamId
                  AND rf.fine_number > 0
                  AND f.name IN ('Narození dítěte (holka)', 'Narození dítěte (kluk)')
                GROUP BY rf.player_id
            ), card_total AS (
                SELECT rf.player_id,
                       SUM(COALESCE(rf.fine_number, 0)) AS card_count
                FROM received_fine rf
                JOIN fine f ON f.id = rf.fine_id
                JOIN match m ON m.id = rf.match_id
                WHERE rf.player_id = :playerId
                  AND m.app_team_id = :appTeamId
                  AND rf.fine_number > 0
                  AND f.name IN ('Červená karta', 'Žlutá karta')
                GROUP BY rf.player_id
            ), first_match_with_both AS (
                SELECT rf.match_id,
                       m.date
                FROM received_fine rf
                JOIN fine f ON f.id = rf.fine_id
                JOIN match m ON m.id = rf.match_id
                WHERE rf.player_id = :playerId
                  AND m.app_team_id = :appTeamId
                  AND rf.fine_number > 0
                  AND f.name IN (
                      'Narození dítěte (holka)',
                      'Narození dítěte (kluk)',
                      'Červená karta',
                      'Žlutá karta'
                  )
                GROUP BY rf.match_id, m.date
                HAVING SUM(CASE
                           WHEN f.name IN ('Narození dítěte (holka)', 'Narození dítěte (kluk)')
                           THEN COALESCE(rf.fine_number, 0)
                           ELSE 0
                       END) > 0
                   AND SUM(CASE
                           WHEN f.name IN ('Červená karta', 'Žlutá karta')
                           THEN COALESCE(rf.fine_number, 0)
                           ELSE 0
                       END) > 0
                ORDER BY m.date ASC
                LIMIT 1
            )
            SELECT fm.match_id AS matchId,
                   CAST(bt.birth_count AS int) AS firstNumber,
                   CAST(ct.card_count AS int) AS secondNumber
            FROM first_match_with_both fm
            JOIN birth_total bt ON true
            JOIN card_total ct ON true
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findFotrJeLotr(@Param("playerId") Long playerId,
                                              @Param("appTeamId") Long appTeamId);

    // Maratonec
    @Query(value = """
            WITH ordered_sessions AS (
                SELECT fs.match_id,
                       fs.player_id,
                       COALESCE(fs.distance, 0) AS distance,
                       COALESCE(m.date, fs.start_date) AS event_date,
                       SUM(COALESCE(fs.distance, 0)) OVER (
                           PARTITION BY fs.player_id
                           ORDER BY COALESCE(m.date, fs.start_date), fs.id
                       ) AS cumulative_distance
                FROM footbar_session fs
                LEFT JOIN match m ON m.id = fs.match_id
                WHERE fs.player_id = :playerId
                  AND (:appTeamId IS NULL OR m.app_team_id = :appTeamId OR fs.match_id IS NULL)
            )
            SELECT match_id AS matchId,
                   CAST(ROUND(cumulative_distance) AS int) AS firstNumber,
                   CAST(42100 AS int) AS secondNumber
            FROM ordered_sessions
            WHERE cumulative_distance >= 42100
            ORDER BY event_date ASC
            LIMIT 1
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findMaratonec(@Param("playerId") Long playerId,
                                             @Param("appTeamId") Long appTeamId);

    // Roberto Carlos
    @Query(value = """
            SELECT fs.match_id AS matchId,
                   CAST(ROUND(fs.shot_speed * 3.6) AS int) AS firstNumber,
                   CAST(g.goal_number AS int) AS secondNumber
            FROM footbar_session fs
            JOIN goal g ON g.match_id = fs.match_id
                       AND g.player_id = fs.player_id
                       AND g.goal_number > 0
            JOIN match m ON m.id = fs.match_id
            WHERE fs.player_id = :playerId
              AND m.app_team_id = :appTeamId
              AND fs.shot_speed > (80.0 / 3.6)
            ORDER BY m.date ASC
            LIMIT 1
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findRobertoCarlos(@Param("playerId") Long playerId,
                                                 @Param("appTeamId") Long appTeamId);

    // Špílmachr
    @Query(value = """
            SELECT fs.match_id AS matchId,
                   CAST(fs.pass_count AS int) AS firstNumber,
                   CAST(COALESCE(SUM(g.assist_number), 0) AS int) AS secondNumber
            FROM footbar_session fs
            JOIN match m ON m.id = fs.match_id
            LEFT JOIN goal g ON g.match_id = fs.match_id
                            AND g.player_id = fs.player_id
            WHERE fs.player_id = :playerId
              AND m.app_team_id = :appTeamId
              AND fs.pass_count >= 40
            GROUP BY fs.match_id, fs.pass_count, m.date
            ORDER BY m.date ASC
            LIMIT 1
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findSpilmachr(@Param("playerId") Long playerId,
                                             @Param("appTeamId") Long appTeamId);

    // Já to za vás oběhal
    @Query(value = """
            WITH match_distances AS (
                SELECT fs.match_id,
                       fs.player_id,
                       SUM(COALESCE(fs.distance, 0)) AS distance
                FROM footbar_session fs
                JOIN match m ON m.id = fs.match_id
                WHERE m.app_team_id = :appTeamId
                GROUP BY fs.match_id, fs.player_id
            ), ranked AS (
                SELECT md.*,
                       COUNT(*) OVER (PARTITION BY md.match_id) AS footbar_players,
                       RANK() OVER (PARTITION BY md.match_id ORDER BY md.distance DESC) AS rank_position
                FROM match_distances md
            )
            SELECT r.match_id AS matchId,
                   CAST(r.distance / 1000.0 AS double precision) AS firstNumber,
                   CAST(r.footbar_players AS int) AS secondNumber
            FROM ranked r
            JOIN match m ON m.id = r.match_id
            WHERE r.player_id = :playerId
              AND r.footbar_players >= 2
              AND r.rank_position = 1
            ORDER BY m.date ASC
            LIMIT 1
            """, nativeQuery = true)
    IMatchIdDecimalAndNumber findJaToZaVasObehal(@Param("playerId") Long playerId,
                                                 @Param("appTeamId") Long appTeamId);

    // Doplnění tekutin
    @Query(value = """
            SELECT fs.match_id AS matchId,
                   CAST(fs.distance / 1000.0 AS double precision) AS firstNumber,
                   CAST(b.beer_number AS int) AS secondNumber
            FROM footbar_session fs
            JOIN beer b ON b.match_id = fs.match_id
                       AND b.player_id = fs.player_id
            JOIN match m ON m.id = fs.match_id
            WHERE fs.player_id = :playerId
              AND m.app_team_id = :appTeamId
              AND fs.distance >= 3000
              AND b.beer_number >= (fs.distance / 1000.0)
            ORDER BY m.date ASC
            LIMIT 1
            """, nativeQuery = true)
    IMatchIdDecimalAndNumber findDoplneniTekutin(@Param("playerId") Long playerId,
                                                 @Param("appTeamId") Long appTeamId);

    // Nástup jako hrom - varianta podle názvu: první zápas a gól
    @Query(value = """
            WITH first_attended_match AS (
                SELECT mp.player_id, mp.match_id, m.date
                FROM match_players mp
                JOIN match m ON m.id = mp.match_id
                WHERE mp.player_id = :playerId
                  AND m.app_team_id = :appTeamId
                ORDER BY m.date ASC
                LIMIT 1
            )
            SELECT fam.match_id AS matchId,
                   CAST(g.goal_number AS int) AS firstNumber,
                   NULL AS secondNumber
            FROM first_attended_match fam
            JOIN goal g ON g.match_id = fam.match_id
                       AND g.player_id = fam.player_id
                       AND g.goal_number > 0
            LIMIT 1
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findNastupJakoHromGoal(@Param("playerId") Long playerId,
                                                      @Param("appTeamId") Long appTeamId);

    // Když leju tak pořádně - volat pro každou sezonu
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
                       SUM(COALESCE(b.beer_number, 0)) AS beer_count,
                       SUM(COALESCE(b.liquor_number, 0)) AS liquor_count
                FROM beer b
                JOIN match m ON m.id = b.match_id
                WHERE m.season_id = :seasonId
                  AND m.app_team_id = :appTeamId
                GROUP BY b.player_id
            ), ranked AS (
                SELECT a.player_id,
                       a.matches_count,
                       COALESCE(d.beer_count, 0) AS beer_count,
                       COALESCE(d.liquor_count, 0) AS liquor_count,
                       (
                           CAST(
                               COALESCE(d.beer_count, 0) + COALESCE(d.liquor_count, 0)
                               AS numeric
                           ) / NULLIF(a.matches_count, 0)
                       ) AS avg_total_drinks,
                       (
                           CAST(COALESCE(d.beer_count, 0) AS numeric)
                           / NULLIF(a.matches_count, 0)
                       ) AS avg_beers,
                       (
                           CAST(COALESCE(d.liquor_count, 0) AS numeric)
                           / NULLIF(a.matches_count, 0)
                       ) AS avg_liquors,
                       RANK() OVER (
                           ORDER BY (
                               CAST(
                                   COALESCE(d.beer_count, 0) + COALESCE(d.liquor_count, 0)
                                   AS numeric
                               ) / NULLIF(a.matches_count, 0)
                           ) DESC
                       ) AS rank_position
                FROM attendance a
                LEFT JOIN drinks d ON d.player_id = a.player_id
                WHERE a.matches_count > 0
            )
            SELECT NULL AS matchId,
                   CAST(avg_beers AS double precision) AS firstNumber,
                   CAST(avg_liquors AS double precision) AS secondNumber,
                   CAST(beer_count AS int) AS thirdNumber,
                   CAST(liquor_count AS int) AS fourthNumber,
                   CAST(matches_count AS int) AS fifthNumber
            FROM ranked
            WHERE player_id = :playerId
              AND (beer_count + liquor_count) > 0
              AND rank_position = 1
            LIMIT 1
            """, nativeQuery = true)
    ISeasonDrinkAverage findKdyzLejuTakPoradneInSeason(@Param("playerId") Long playerId,
                                                       @Param("seasonId") Long seasonId,
                                                       @Param("appTeamId") Long appTeamId);

    // Machýrek
    @Query(value = """
            WITH attendees AS (
                SELECT mp.match_id, mp.player_id
                FROM match_players mp
                JOIN player p ON p.id = mp.player_id
                JOIN match m ON m.id = mp.match_id
                WHERE p.fan = false
                  AND m.app_team_id = :appTeamId
            ), rabona_players AS (
                SELECT DISTINCT rf.match_id, rf.player_id
                FROM received_fine rf
                JOIN fine f ON f.id = rf.fine_id
                WHERE f.name = 'Rabona (gól)'
                  AND rf.fine_number > 0
            ), counts AS (
                SELECT a.match_id,
                       COUNT(DISTINCT a.player_id) AS players_count,
                       COUNT(DISTINCT rp.player_id) AS rabona_fined_count
                FROM attendees a
                LEFT JOIN rabona_players rp ON rp.match_id = a.match_id AND rp.player_id = a.player_id
                GROUP BY a.match_id
            )
            SELECT a.match_id AS matchId,
                   CAST(c.players_count AS int) AS firstNumber,
                   CAST(c.rabona_fined_count AS int) AS secondNumber
            FROM attendees a
            JOIN counts c ON c.match_id = a.match_id
            JOIN match m ON m.id = a.match_id
            WHERE a.player_id = :playerId
              AND c.players_count > 1
              AND c.rabona_fined_count = c.players_count - 1
              AND NOT EXISTS (
                  SELECT 1
                  FROM rabona_players rp
                  WHERE rp.match_id = a.match_id
                    AND rp.player_id = :playerId
              )
            ORDER BY m.date ASC
            LIMIT 1
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findMachyrek(@Param("playerId") Long playerId,
                                            @Param("appTeamId") Long appTeamId);

    // Sdílený střelec
    @Query(value = """
            WITH hattricks AS (
                SELECT g.match_id, g.player_id, g.goal_number
                FROM goal g
                JOIN match m ON m.id = g.match_id
                WHERE m.app_team_id = :appTeamId
                  AND g.goal_number >= 3
            ), match_hattricks AS (
                SELECT match_id, COUNT(DISTINCT player_id) AS hattrick_players
                FROM hattricks
                GROUP BY match_id
            )
            SELECT h.match_id AS matchId,
                   CAST(h.goal_number AS int) AS firstNumber,
                   CAST(mh.hattrick_players AS int) AS secondNumber
            FROM hattricks h
            JOIN match_hattricks mh ON mh.match_id = h.match_id
            JOIN match m ON m.id = h.match_id
            WHERE h.player_id = :playerId
              AND mh.hattrick_players >= 2
            ORDER BY m.date ASC
            LIMIT 1
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findSdilenyStrelec(@Param("playerId") Long playerId,
                                                  @Param("appTeamId") Long appTeamId);

    // Nesobecký hrdina
    @Query(value = """
            SELECT g.match_id AS matchId,
                   CAST(g.assist_number AS int) AS firstNumber,
                    CAST(g.goal_number AS int) AS secondNumber
            FROM goal g
            JOIN match m ON m.id = g.match_id
            WHERE g.player_id = :playerId
              AND m.app_team_id = :appTeamId
              AND g.assist_number >= 3
            ORDER BY m.date ASC
            LIMIT 1
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findNesobeckyHrdina(@Param("playerId") Long playerId,
                                                   @Param("appTeamId") Long appTeamId);

    // Góly? Ne, raději pivo - volat pro každou sezonu
    @Query(value = """
            WITH beers AS (
                SELECT b.player_id,
                       SUM(COALESCE(b.beer_number, 0)) AS beer_count
                FROM beer b
                JOIN match m ON m.id = b.match_id
                WHERE m.season_id = :seasonId
                  AND m.app_team_id = :appTeamId
                GROUP BY b.player_id
            ), goals AS (
                SELECT g.player_id,
                       SUM(COALESCE(g.goal_number, 0)) AS goal_count
                FROM goal g
                JOIN match m ON m.id = g.match_id
                WHERE m.season_id = :seasonId
                  AND m.app_team_id = :appTeamId
                GROUP BY g.player_id
            ), ranked AS (
                SELECT be.player_id,
                       be.beer_count,
                       go.goal_count,
                       (
                           CAST(be.beer_count AS numeric)
                           / NULLIF(go.goal_count, 0)
                       ) AS beers_per_goal,
                       RANK() OVER (
                           ORDER BY (
                               CAST(be.beer_count AS numeric)
                               / NULLIF(go.goal_count, 0)
                           ) DESC
                       ) AS rank_position
                FROM beers be
                JOIN goals go ON go.player_id = be.player_id
                WHERE be.beer_count > 0
                  AND go.goal_count > 0
            )
            SELECT NULL AS matchId,
                   CAST(beers_per_goal AS double precision) AS firstNumber,
                   CAST(beer_count AS int) AS secondNumber,
                   CAST(goal_count AS int) AS thirdNumber
            FROM ranked
            WHERE player_id = :playerId
              AND rank_position = 1
            LIMIT 1
            """, nativeQuery = true)
    IAverageAndTwoNumbers findGolyNeRadejiPivoInSeason(@Param("playerId") Long playerId,
                                                       @Param("seasonId") Long seasonId,
                                                       @Param("appTeamId") Long appTeamId);

    // Jarda Kužel
    @Query(value = """
            WITH ordered_matches AS (
                SELECT m.id AS match_id,
                       m.football_match_id,
                       m.date,
                       ROW_NUMBER() OVER (ORDER BY m.date ASC, m.id ASC) AS rn
                FROM match m
                WHERE m.app_team_id = :appTeamId
            ), attendance AS (
                SELECT om.match_id,
                       om.rn,
                       CASE WHEN mp.player_id IS NULL THEN false ELSE true END AS attended
                FROM ordered_matches om
                LEFT JOIN match_players mp ON mp.match_id = om.match_id
                                          AND mp.player_id = :playerId
            ), candidate_matches AS (
                SELECT om.*
                FROM ordered_matches om
                WHERE EXISTS (
                    SELECT 1
                    FROM match_players mp
                    WHERE mp.match_id = om.match_id
                      AND mp.player_id = :playerId
                )
                  AND EXISTS (
                    SELECT 1
                    FROM player p
                    JOIN football_match_player fmp ON fmp.match_id = om.football_match_id
                                                  AND fmp.player_id = p.football_player_id
                                                  AND fmp.best_player = true
                    WHERE p.id = :playerId
                )
            ), candidates_with_absences AS (
                SELECT cm.match_id,
                       cm.date,
                       cm.rn,
                       COALESCE((
                           SELECT COUNT(*)
                           FROM ordered_matches prev
                           WHERE prev.rn < cm.rn
                             AND NOT EXISTS (
                                 SELECT 1
                                 FROM match_players mp
                                 WHERE mp.match_id = prev.match_id
                                   AND mp.player_id = :playerId
                             )
                             AND prev.rn > COALESCE((
                                 SELECT MAX(att.rn)
                                 FROM attendance att
                                 WHERE att.rn < cm.rn
                                   AND att.attended = true
                             ), 0)
                       ), 0) AS missed_before_count
                FROM candidate_matches cm
            ), goals_in_candidate AS (
                SELECT g.match_id,
                       SUM(COALESCE(g.goal_number, 0)) AS goal_count
                FROM goal g
                WHERE g.player_id = :playerId
                GROUP BY g.match_id
            )
            SELECT cwa.match_id AS matchId,
                   CAST(cwa.missed_before_count AS int) AS firstNumber,
                   CAST(COALESCE(gic.goal_count, 0) AS int) AS secondNumber
            FROM candidates_with_absences cwa
            LEFT JOIN goals_in_candidate gic ON gic.match_id = cwa.match_id
            WHERE cwa.missed_before_count >= 3
            ORDER BY cwa.date ASC
            LIMIT 1
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findJardaKuzel(@Param("playerId") Long playerId,
                                              @Param("appTeamId") Long appTeamId);

    // Moderní gólmanská škola
    @Query(value = """
            SELECT g.match_id AS matchId,
                   CAST(g.assist_number AS int) AS firstNumber,
                   CAST(fmp.goalkeeping_minutes AS int) AS secondNumber
            FROM goal g
            JOIN match m ON m.id = g.match_id
            JOIN player p ON p.id = g.player_id
            JOIN football_match_player fmp ON fmp.match_id = m.football_match_id
                                          AND fmp.player_id = p.football_player_id
            WHERE g.player_id = :playerId
              AND m.app_team_id = :appTeamId
              AND g.assist_number > 0
              AND fmp.goalkeeping_minutes > 0
            ORDER BY m.date ASC
            LIMIT 1
            """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findModerniGolmanskaSkola(@Param("playerId") Long playerId,
                                                         @Param("appTeamId") Long appTeamId);

    // Morální podpora - hráč se zúčastnil zápasu, ale nebyl na oficiální soupisce
    @Query(value = """
        SELECT m.id AS matchId,
               CAST(COALESCE(SUM(b.beer_number), 0) AS int) AS firstNumber,
               CAST(COALESCE(SUM(b.liquor_number), 0) AS int) AS secondNumber
        FROM match m
        JOIN match_players mp
             ON mp.match_id = m.id
            AND mp.player_id = :playerId
        JOIN player p
             ON p.id = mp.player_id
        LEFT JOIN beer b
             ON b.match_id = m.id
            AND b.player_id = p.id
        WHERE p.id = :playerId
          AND p.fan = false
          AND p.football_player_id IS NOT NULL
          AND m.football_match_id IS NOT NULL
          AND m.app_team_id = :appTeamId
          AND NOT EXISTS (
              SELECT 1
              FROM football_match_player fmp
              WHERE fmp.match_id = m.football_match_id
                AND fmp.player_id = p.football_player_id
          )
        GROUP BY m.id, m.date
        ORDER BY m.date ASC, m.id ASC
        LIMIT 1
        """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findMoralniPodpora(
            @Param("playerId") Long playerId,
            @Param("appTeamId") Long appTeamId
    );

    // Lazar na tribuně - alespoň 3 zápasy mimo oficiální soupisku v jedné sezoně
    @Query(value = """
        WITH tribunal_matches AS (
            SELECT m.id AS match_id,
                   m.date,
                   CAST(COALESCE(SUM(b.beer_number), 0) AS int) AS beer_number
            FROM match m
            JOIN match_players mp
                 ON mp.match_id = m.id
                AND mp.player_id = :playerId
            JOIN player p
                 ON p.id = mp.player_id
            LEFT JOIN beer b
                 ON b.match_id = m.id
                AND b.player_id = p.id
            WHERE p.id = :playerId
              AND p.fan = false
              AND p.football_player_id IS NOT NULL
              AND m.football_match_id IS NOT NULL
              AND m.season_id = :seasonId
              AND m.app_team_id = :appTeamId
              AND NOT EXISTS (
                  SELECT 1
                  FROM football_match_player fmp
                  WHERE fmp.match_id = m.football_match_id
                    AND fmp.player_id = p.football_player_id
              )
            GROUP BY m.id, m.date
        ),
        ranked_tribunal_matches AS (
            SELECT tm.*,
                   ROW_NUMBER() OVER (
                       ORDER BY tm.date ASC NULLS LAST, tm.match_id ASC
                   ) AS tribunal_order
            FROM tribunal_matches tm
        ),
        tribunal_totals AS (
            SELECT COUNT(*) AS tribunal_match_count,
                   COALESCE(SUM(beer_number), 0) AS tribunal_beer_count
            FROM tribunal_matches
        )
        SELECT rtm.match_id AS matchId,
               CAST(tt.tribunal_match_count AS int) AS firstNumber,
               CAST(tt.tribunal_beer_count AS int) AS secondNumber
        FROM ranked_tribunal_matches rtm
        CROSS JOIN tribunal_totals tt
        WHERE rtm.tribunal_order = 3
        """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findLazarNaTribune(
            @Param("playerId") Long playerId,
            @Param("appTeamId") Long appTeamId,
            @Param("seasonId") Long seasonId
    );

    // Společný kumulativní milník piv/panáků
    @Query(value = """
        WITH drinks_per_match AS (
            SELECT b.match_id,
                   m.date,
                   SUM(COALESCE(b.beer_number, 0)) AS beers,
                   SUM(COALESCE(b.liquor_number, 0)) AS liquors
            FROM beer b
            JOIN match m ON m.id = b.match_id
            WHERE b.player_id = :playerId
              AND m.app_team_id = :appTeamId
            GROUP BY b.match_id, m.date
        ), cumulative AS (
            SELECT match_id,
                   date,
                   SUM(beers) OVER (ORDER BY date ASC NULLS LAST, match_id ASC) AS cumulative_beers,
                   SUM(liquors) OVER (ORDER BY date ASC NULLS LAST, match_id ASC) AS cumulative_liquors
            FROM drinks_per_match
        )
        SELECT match_id AS matchId,
               CAST(cumulative_beers AS int) AS firstNumber,
               CAST(cumulative_liquors AS int) AS secondNumber
        FROM cumulative
        WHERE cumulative_beers >= :beerThreshold
          AND cumulative_liquors >= :liquorThreshold
          AND (:beerThreshold > 0 OR :liquorThreshold > 0)
        ORDER BY date ASC NULLS LAST, match_id ASC
        LIMIT 1
        """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findDrinkMilestone(@Param("playerId") Long playerId,
                                                   @Param("appTeamId") Long appTeamId,
                                                   @Param("beerThreshold") int beerThreshold,
                                                   @Param("liquorThreshold") int liquorThreshold);

    // Hvězda co se nezdá
    @Query(value = """
        SELECT fm.id AS matchId,
               CAST(fmp.goals AS int) AS firstNumber,
               NULL AS secondNumber
        FROM player p
        JOIN football_match_player fmp ON fmp.player_id = p.football_player_id
        JOIN football_match fm ON fm.id = fmp.match_id
        JOIN match m ON m.football_match_id = fm.id
        WHERE p.id = :playerId
          AND m.app_team_id = :appTeamId
          AND fmp.best_player = true
        ORDER BY m.date ASC NULLS LAST, m.id ASC
        LIMIT 1
        """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findFirstBestPlayerMatch(@Param("playerId") Long playerId,
                                                        @Param("appTeamId") Long appTeamId);


    // Konzistence - gól ve třech týmových zápasech za sebou podle tabulky goal + match
    @Query(value = """
        WITH ordered_matches AS (
            SELECT m.id AS match_id,
                   m.date,
                   CAST(COALESCE(SUM(g.goal_number), 0) AS int) AS goals,
                   CAST(COALESCE(SUM(g.assist_number), 0) AS int) AS assists
            FROM match m
            LEFT JOIN goal g
                   ON g.match_id = m.id
                  AND g.player_id = :playerId
            WHERE m.app_team_id = :appTeamId
              AND m.football_match_id IS NOT NULL
            GROUP BY m.id, m.date
        ), match_windows AS (
            SELECT om.*,
                   LEAD(match_id, 2) OVER (ORDER BY date ASC NULLS LAST, match_id ASC) AS third_match_id,
                   LEAD(goals, 1) OVER (ORDER BY date ASC NULLS LAST, match_id ASC) AS second_goals,
                   LEAD(goals, 2) OVER (ORDER BY date ASC NULLS LAST, match_id ASC) AS third_goals,
                   LEAD(assists, 1) OVER (ORDER BY date ASC NULLS LAST, match_id ASC) AS second_assists,
                   LEAD(assists, 2) OVER (ORDER BY date ASC NULLS LAST, match_id ASC) AS third_assists
            FROM ordered_matches om
        )
        SELECT third_match_id AS matchId,
               CAST((goals + second_goals + third_goals) AS int) AS firstNumber,
               CAST((assists + second_assists + third_assists) AS int) AS secondNumber
        FROM match_windows
        WHERE goals > 0
          AND second_goals > 0
          AND third_goals > 0
        ORDER BY date ASC NULLS LAST, match_id ASC
        LIMIT 1
        """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findFirstThreeConsecutiveMatchesWithGoal(@Param("playerId") Long playerId,
                                                                        @Param("appTeamId") Long appTeamId);

    // Komplexní hráč
    @Query(value = """
        SELECT g.match_id AS matchId,
               CAST(g.goal_number AS int) AS firstNumber,
               CAST(g.assist_number AS int) AS secondNumber
        FROM goal g
        JOIN match m ON m.id = g.match_id
        WHERE g.player_id = :playerId
          AND m.app_team_id = :appTeamId
          AND g.goal_number > 0
          AND g.assist_number > 0
        ORDER BY m.date ASC NULLS LAST, m.id ASC
        LIMIT 1
        """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findFirstMatchWithGoalAndAssist(@Param("playerId") Long playerId,
                                                               @Param("appTeamId") Long appTeamId);

    // Společný milník fanouškovských účastí
    @Query(value = """
        WITH distinct_attendances AS (
            SELECT DISTINCT mp.match_id, m.date
            FROM match_players mp
            JOIN match m ON m.id = mp.match_id
            JOIN player p ON p.id = mp.player_id
            WHERE mp.player_id = :playerId
              AND p.fan = true
              AND m.app_team_id = :appTeamId
        ), attendances AS (
            SELECT match_id,
                   date,
                   ROW_NUMBER() OVER (ORDER BY date ASC NULLS LAST, match_id ASC) AS attendance_number
            FROM distinct_attendances
        )
        SELECT match_id AS matchId,
               CAST(attendance_number AS int) AS firstNumber,
               NULL AS secondNumber
        FROM attendances
        WHERE attendance_number = :attendanceThreshold
        LIMIT 1
        """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findFanAttendanceMilestone(@Param("playerId") Long playerId,
                                                           @Param("appTeamId") Long appTeamId,
                                                           @Param("attendanceThreshold") int attendanceThreshold);

    // Do počtu
    @Query(value = """
        WITH attended_matches AS (
            SELECT m.id AS match_id,
                   m.date,
                   s.name AS season_name,
                   COALESCE(SUM(g.goal_number), 0) AS goals,
                   COALESCE(SUM(g.assist_number), 0) AS assists
            FROM match_players mp
            JOIN player p ON p.id = mp.player_id
            JOIN match m ON m.id = mp.match_id
            LEFT JOIN season s ON s.id = m.season_id
            LEFT JOIN goal g ON g.match_id = m.id AND g.player_id = mp.player_id
            WHERE mp.player_id = :playerId
              AND p.fan = false
              AND m.app_team_id = :appTeamId
            GROUP BY m.id, m.date, s.name
        ), grouped AS (
            SELECT am.*,
                   SUM(CASE WHEN goals + assists > 0 THEN 1 ELSE 0 END)
                       OVER (ORDER BY date ASC NULLS LAST, match_id ASC) AS points_group
            FROM attended_matches am
        ), zero_point_streaks AS (
            SELECT g.*,
                   ROW_NUMBER() OVER (PARTITION BY points_group ORDER BY date ASC NULLS LAST, match_id ASC) AS streak_position,
                   COUNT(*) OVER (PARTITION BY points_group) AS streak_length
            FROM grouped g
            WHERE goals + assists = 0
        )
        SELECT match_id AS matchId,
               CAST(streak_length AS int) AS firstNumber,
               CAST(goals AS int) AS secondNumber,
               CAST(assists AS int) AS thirdNumber,
               season_name AS text
        FROM zero_point_streaks
        WHERE streak_position = 5
        ORDER BY date ASC NULLS LAST, match_id ASC
        LIMIT 1
        """, nativeQuery = true)
    IMatchIdThreeNumbersAndText findDoPoctu(@Param("playerId") Long playerId,
                                            @Param("appTeamId") Long appTeamId);

    // Hattrick Gordieho Howa
    @Query(value = """
        SELECT g.match_id AS matchId,
               CAST(g.goal_number AS int) AS firstNumber,
               CAST(g.assist_number AS int) AS secondNumber,
               CAST(SUM(rf.fine_number) AS int) AS thirdNumber,
               STRING_AGG(DISTINCT f.name, ', ') AS text
        FROM goal g
        JOIN match m ON m.id = g.match_id
        JOIN received_fine rf ON rf.match_id = g.match_id AND rf.player_id = g.player_id
        JOIN fine f ON f.id = rf.fine_id
        WHERE g.player_id = :playerId
          AND m.app_team_id = :appTeamId
          AND g.goal_number > 0
          AND g.assist_number > 0
          AND rf.fine_number > 0
          AND f.name IN ('Žlutá karta', 'Červená karta')
        GROUP BY g.match_id, g.goal_number, g.assist_number, m.date
        ORDER BY m.date ASC NULLS LAST, g.match_id ASC
        LIMIT 1
        """, nativeQuery = true)
    IMatchIdThreeNumbersAndText findHattrickGordiehoHowa(@Param("playerId") Long playerId,
                                                         @Param("appTeamId") Long appTeamId);

    // Společný milník vybraných pokut
    @Query(value = """
        WITH fines_per_match AS (
            SELECT rf.match_id,
                   m.date,
                   SUM(rf.fine_number) AS fine_count
            FROM received_fine rf
            JOIN fine f ON f.id = rf.fine_id
            JOIN match m ON m.id = rf.match_id
            WHERE rf.player_id = :playerId
              AND m.app_team_id = :appTeamId
              AND f.name IN (:fineNames)
              AND rf.fine_number > 0
            GROUP BY rf.match_id, m.date
        ), cumulative AS (
            SELECT match_id,
                   date,
                   fine_count,
                   SUM(fine_count) OVER (ORDER BY date ASC NULLS LAST, match_id ASC) AS cumulative_fines,
                   SUM(fine_count) OVER () AS total_fines
            FROM fines_per_match
        )
        SELECT match_id AS matchId,
               CAST(total_fines AS int) AS firstNumber,
               CAST(cumulative_fines AS int) AS secondNumber
        FROM cumulative
        WHERE cumulative_fines >= :threshold
        ORDER BY date ASC NULLS LAST, match_id ASC
        LIMIT 1
        """, nativeQuery = true)
    IMatchIdNumberOneNumberTwo findFineMilestone(@Param("playerId") Long playerId,
                                                  @Param("appTeamId") Long appTeamId,
                                                  @Param("fineNames") List<String> fineNames,
                                                  @Param("threshold") int threshold);

    // Černé geny
    @Query(value = """
        SELECT fs.match_id AS matchId,
               CAST(MAX(fs.sprint_speed) * 3.6 AS double precision) AS firstNumber,
               CAST(COALESCE(SUM(fs.sprint_count), 0) AS int) AS secondNumber
        FROM footbar_session fs
        JOIN match m ON m.id = fs.match_id
        WHERE fs.player_id = :playerId
          AND m.app_team_id = :appTeamId
          AND fs.sprint_speed >= (25.0 / 3.6)
        GROUP BY fs.match_id, m.date
        ORDER BY m.date ASC NULLS LAST, fs.match_id ASC
        LIMIT 1
        """, nativeQuery = true)
    IMatchIdDecimalAndNumber findCerneGeny(@Param("playerId") Long playerId,
                                           @Param("appTeamId") Long appTeamId);

}

