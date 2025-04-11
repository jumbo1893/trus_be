package com.jumbo.trus.entity.repository.achievement;

import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.achievement.PlayerAchievementEntity;
import com.jumbo.trus.service.achievement.helper.IGoalBeerFineMatch;
import com.jumbo.trus.service.achievement.helper.IGoalBeerMatch;
import com.jumbo.trus.service.achievement.helper.IMatchIdNumberOneNumberTwo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerAchievementRepository extends JpaRepository<PlayerAchievementEntity, Long> {

    Optional<PlayerAchievementEntity> findByPlayerIdAndAchievementId(Long playerId, Long achievementId);

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


    List<PlayerAchievementEntity> findAllByPlayerId(Long playerId);

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
            SELECT b.match_id AS matchId, b.beer_number AS beerNumber,
            b.liquor_number AS liquorNumber, g.goal_number AS goalNumber, r.fine_number AS fineNumber
            FROM beer b
            JOIN goal g ON b.match_id = g.match_id AND b.player_id = g.player_id
            JOIN received_fine r ON b.match_id = g.match_id AND b.player_id = g.player_id
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
    IGoalBeerFineMatch getFirstMatchWithGoalYellowBeerAndLiquor(@Param("playerId") Long playerId, @Param("fineName") String fineName);

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


}

