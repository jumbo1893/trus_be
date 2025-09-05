package com.jumbo.trus.repository;

import com.jumbo.trus.entity.BeerEntity;
import com.jumbo.trus.service.beer.helper.AverageBeer;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BeerRepository extends PagingAndSortingRepository<BeerEntity, Long>, JpaRepository<BeerEntity, Long>, JpaSpecificationExecutor<BeerEntity> {

    @Query(value = "SELECT * from beer WHERE app_team_id=:#{#appTeamId} LIMIT :limit", nativeQuery = true)
    List<BeerEntity> getAll(@Param("limit") int limit, @Param("appTeamId") Long appTeamId);

    @Modifying
    @Query(value = "DELETE from beer WHERE player_id=:#{#playerId}", nativeQuery = true)
    void deleteByPlayerId(@Param("playerId") long playerId);

    @Modifying
    @Query(value = "DELETE from beer WHERE match_id=:#{#matchId}", nativeQuery = true)
    void deleteByMatchId(@Param("matchId") long matchId);

    @Query(value = """
            SELECT *
            FROM (
                SELECT b.*, row_number() over(partition by b.player_id order by b.beer_number) rn
                FROM beer b
                JOIN (
                    SELECT player_id, max(beer_number) AS max_beer_number
                    FROM beer
                    WHERE app_team_id=:#{#appTeamId}
                    GROUP BY player_id
                ) max_beer ON b.player_id = max_beer.player_id AND b.beer_number = max_beer.max_beer_number
                WHERE beer_number > 0
            ) ranked_beer
            WHERE app_team_id=:#{#appTeamId}
            ORDER BY beer_number DESC;
            """, nativeQuery = true)
    List<BeerEntity> getMaxBeer(@Param("appTeamId") Long appTeamId);

    @Query(value = """
            SELECT *
            FROM (
                SELECT b.*, row_number() over(partition by b.player_id order by b.beer_number) rn
                FROM beer b
                JOIN (
                    SELECT player_id, max(beer_number) AS max_beer_number
                    FROM beer
                    INNER JOIN match m ON match_id=m.id
                        WHERE m.season_id=:#{#seasonId}
                        AND app_team_id=:#{#appTeamId}
                    GROUP BY player_id
                ) max_beer ON b.player_id = max_beer.player_id AND b.beer_number = max_beer.max_beer_number
                INNER JOIN match m ON match_id=m.id
                WHERE m.season_id=:#{#seasonId}
                AND beer_number > 0
            ) ranked_beer
            WHERE rn = 1
            ORDER BY beer_number DESC;
            """, nativeQuery = true)
    List<BeerEntity> getMaxBeer(@Param("seasonId") long seasonId, @Param("appTeamId") Long appTeamId);

    @Query(value = """
            SELECT *
            FROM (
                SELECT b.*, row_number() over(partition by b.player_id order by b.liquor_number) rn
                FROM beer b
                JOIN (
                    SELECT player_id, max(liquor_number) AS max_liquor_number
                    FROM beer
                    WHERE app_team_id=:#{#appTeamId}
                    GROUP BY player_id
                ) max_liquor ON b.player_id = max_liquor.player_id AND b.liquor_number = max_liquor.max_liquor_number
                WHERE liquor_number > 0
            ) ranked_liquor
            WHERE rn = 1
            ORDER BY liquor_number DESC;
            """, nativeQuery = true)
    List<BeerEntity> getMaxLiquor(@Param("appTeamId") Long appTeamId);

    @Query(value = """
            SELECT *
            FROM (
                SELECT b.*, row_number() over(partition by b.player_id order by b.liquor_number) rn
                FROM beer b
                JOIN (
                    SELECT player_id, max(liquor_number) AS max_liquor_number
                    FROM beer
                    INNER JOIN match m ON match_id=m.id
                        WHERE m.season_id=:#{#seasonId}
                        AND app_team_id=:#{#appTeamId}
                    GROUP BY player_id
                ) max_liquor ON b.player_id = max_liquor.player_id AND b.liquor_number = max_liquor.max_liquor_number
                INNER JOIN match m ON match_id=m.id
                WHERE m.season_id=:#{#seasonId}
                AND liquor_number > 0
            ) ranked_liquor
            WHERE rn = 1
            ORDER BY liquor_number DESC;
            """, nativeQuery = true)
    List<BeerEntity> getMaxLiquor(@Param("seasonId") long seasonId, @Param("appTeamId") Long appTeamId);

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.beer_number) AS totalBeerNumber, mp.match_count as matchCount,
                   CAST(SUM(b.beer_number) AS FLOAT) / mp.match_count AS avgBeerPerMatch
            FROM beer b
            JOIN (
                SELECT player_id, COUNT(*) AS match_count
                FROM match_players mp
                WHERE match_id in (SELECT id FROM match WHERE app_team_id=:#{#appTeamId})
                GROUP BY player_id
            ) mp ON b.player_id = mp.player_id
            WHERE b.app_team_id=:#{#appTeamId}
            GROUP BY b.player_id, mp.match_count
            HAVING SUM(b.beer_number) > 0
            ORDER BY ?#{#sort}
            """, nativeQuery = true)
    List<AverageBeer> getAverageBeer(Sort sort, @Param("appTeamId") Long appTeamId);

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.beer_number) AS totalBeerNumber, mp.match_count as matchCount,
                   CAST(SUM(b.beer_number) AS FLOAT) / mp.match_count AS avgBeerPerMatch
            FROM beer b
            JOIN (
                SELECT player_id, COUNT(*) AS match_count
                FROM match_players mp
                INNER JOIN match m ON match_id=m.id
                WHERE m.season_id=:#{#seasonId}
                AND match_id in (SELECT id FROM match WHERE app_team_id=:#{#appTeamId})
                GROUP BY player_id
            ) mp ON b.player_id = mp.player_id
            INNER JOIN match m ON match_id=m.id
                WHERE m.season_id=:#{#seasonId}
                AND b.app_team_id=:#{#appTeamId}
            GROUP BY b.player_id, mp.match_count
            HAVING SUM(b.beer_number) > 0
            ORDER BY ?#{#sort}
            """, nativeQuery = true)
    List<AverageBeer> getAverageBeer(@Param("seasonId") long seasonId, @Param("appTeamId") Long appTeamId, Sort sort);

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.liquor_number) AS totalBeerNumber, mp.match_count as matchCount,
                   CAST(SUM(b.liquor_number) AS FLOAT) / mp.match_count AS avgBeerPerMatch
            FROM beer b
            JOIN (
                SELECT player_id, COUNT(*) AS match_count
                FROM match_players mp
                WHERE match_id in (SELECT id FROM match WHERE app_team_id=:#{#appTeamId})
                GROUP BY player_id
            ) mp ON b.player_id = mp.player_id
            WHERE b.app_team_id=:#{#appTeamId}
            GROUP BY b.player_id, mp.match_count
            HAVING SUM(b.liquor_number) > 0
            ORDER BY ?#{#sort}
            """, nativeQuery = true)
    List<AverageBeer> getAverageLiquor(Sort sort, @Param("appTeamId") Long appTeamId);

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.liquor_number) AS totalBeerNumber, mp.match_count as matchCount,
                   CAST(SUM(b.liquor_number) AS FLOAT) / mp.match_count AS avgBeerPerMatch
            FROM beer b
            JOIN (
                SELECT player_id, COUNT(*) AS match_count
                FROM match_players mp
                INNER JOIN match m ON match_id=m.id
                WHERE m.season_id=:#{#seasonId}
                AND match_id in (SELECT id FROM match WHERE app_team_id=:#{#appTeamId})
                GROUP BY player_id
            ) mp ON b.player_id = mp.player_id
            INNER JOIN match m ON match_id=m.id
                WHERE m.season_id=:#{#seasonId}
                AND b.app_team_id=:#{#appTeamId}
            GROUP BY b.player_id, mp.match_count
            HAVING SUM(b.liquor_number) > 0
            ORDER BY ?#{#sort}
            """, nativeQuery = true)
    List<AverageBeer> getAverageLiquor(@Param("seasonId") long seasonId, @Param("appTeamId") Long appTeamId, Sort sort);

    @Query(value = """
            SELECT b.player_id as playerId, (SUM(b.beer_number)+SUM(b.liquor_number)) AS totalBeerNumber, mp.match_count as matchCount,
                   CAST((SUM(b.beer_number)+SUM(b.liquor_number)) AS FLOAT) / mp.match_count AS avgBeerPerMatch
            FROM beer b
            JOIN (
                SELECT player_id, COUNT(*) AS match_count
                FROM match_players mp
                INNER JOIN match m ON match_id=m.id
                WHERE m.season_id=:#{#seasonId}
                AND match_id in (SELECT id FROM match WHERE app_team_id=:#{#appTeamId})
                GROUP BY player_id
            ) mp ON b.player_id = mp.player_id
            INNER JOIN match m ON match_id=m.id
                WHERE m.season_id=:#{#seasonId}
                AND b.app_team_id=:#{#appTeamId}
            GROUP BY b.player_id, mp.match_count
            HAVING SUM(b.beer_number) > 0 OR SUM(b.liquor_number) > 0
            ORDER BY totalBeerNumber DESC
            """, nativeQuery = true)
    List<AverageBeer> getAverageBeerAndLiquorSum(@Param("seasonId") long seasonId, @Param("appTeamId") Long appTeamId);

    @Query(value = """
            SELECT b.player_id as playerId, (SUM(b.beer_number)+SUM(b.liquor_number)) AS totalBeerNumber, mp.match_count as matchCount,
                   CAST((SUM(b.beer_number)+SUM(b.liquor_number)) AS FLOAT) / mp.match_count AS avgBeerPerMatch
            FROM beer b
            JOIN (
                SELECT player_id, COUNT(*) AS match_count
                FROM match_players mp
                WHERE match_id in (SELECT id FROM match WHERE app_team_id=:#{#appTeamId})
                GROUP BY player_id
            ) mp ON b.player_id = mp.player_id
            WHERE b.app_team_id=:#{#appTeamId}
            GROUP BY b.player_id, mp.match_count
            HAVING SUM(b.beer_number) > 0 OR SUM(b.liquor_number) > 0
            ORDER BY totalBeerNumber DESC
            """, nativeQuery = true)
    List<AverageBeer> getAverageBeerAndLiquorSum(@Param("appTeamId") Long appTeamId);

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.beer_number) AS totalBeerNumber, g.goal_count as matchCount,
                               CAST(SUM(b.beer_number) AS FLOAT) / g.goal_count AS avgBeerPerMatch
                        FROM beer b
                        JOIN (
                            SELECT player_id, SUM(goal_number) AS goal_count
                            FROM goal g
                            WHERE match_id in (SELECT id FROM match WHERE app_team_id=:#{#appTeamId})
                            GROUP BY player_id
                        ) g ON b.player_id = g.player_id
            			    WHERE g.goal_count > 0
            			    AND b.app_team_id=:#{#appTeamId}
                        GROUP BY b.player_id, g.goal_count
                        HAVING SUM(b.beer_number) > 0
                        ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getGoalBeerRatio(@Param("appTeamId") Long appTeamId);

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.beer_number) AS totalBeerNumber, g.goal_count as matchCount,
                               CAST(SUM(b.beer_number) AS FLOAT) / g.goal_count AS avgBeerPerMatch
                        FROM beer b
                        JOIN (
                            SELECT player_id, SUM(goal_number) AS goal_count
                            FROM goal g
                            INNER JOIN match m ON match_id=m.id
                                WHERE m.season_id=:#{#seasonId}
                                AND b.app_team_id=:#{#appTeamId}
                            GROUP BY player_id
                        ) g ON b.player_id = g.player_id
                        INNER JOIN match m ON match_id=m.id
                            WHERE m.season_id=:#{#seasonId}
            			    AND g.goal_count > 0
            			    AND b.app_team_id=:#{#appTeamId}
                        GROUP BY b.player_id, g.goal_count
                        HAVING SUM(b.beer_number) > 0
                        ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getGoalBeerRatio(@Param("seasonId") long seasonId, @Param("appTeamId") Long appTeamId);

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.liquor_number) AS totalBeerNumber, g.goal_count as matchCount,
                               CAST(SUM(b.liquor_number) AS FLOAT) / g.goal_count AS avgBeerPerMatch
                        FROM beer b
                        JOIN (
                            SELECT player_id, SUM(goal_number) AS goal_count
                            FROM goal g
                            WHERE match_id in (SELECT id FROM match WHERE app_team_id=:#{#appTeamId})
                            GROUP BY player_id
                        ) g ON b.player_id = g.player_id
            			    WHERE g.goal_count > 0
            			    AND b.app_team_id=:#{#appTeamId}
                        GROUP BY b.player_id, g.goal_count
                        HAVING SUM(b.liquor_number) > 0
                        ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getGoalLiquorRatio(@Param("appTeamId") Long appTeamId);

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.liquor_number) AS totalBeerNumber, g.goal_count as matchCount,
                               CAST(SUM(b.liquor_number) AS FLOAT) / g.goal_count AS avgBeerPerMatch
                        FROM beer b
                        JOIN (
                            SELECT player_id, SUM(goal_number) AS goal_count
                            FROM goal g
                            INNER JOIN match m ON match_id=m.id
                                WHERE m.season_id=:#{#seasonId}
                                AND b.app_team_id=:#{#appTeamId}
                            GROUP BY player_id
                        ) g ON b.player_id = g.player_id
                        INNER JOIN match m ON match_id=m.id
                            WHERE m.season_id=:#{#seasonId}
            			    AND g.goal_count > 0
            			    AND b.app_team_id=:#{#appTeamId}
                        GROUP BY b.player_id, g.goal_count
                        HAVING SUM(b.liquor_number) > 0
                        ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getGoalLiquorRatio(@Param("seasonId") long seasonId, @Param("appTeamId") Long appTeamId);

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.beer_number) AS totalBeerNumber, g.assist_count as matchCount,
                               CAST(SUM(b.beer_number) AS FLOAT) / g.assist_count AS avgBeerPerMatch
                        FROM beer b
                        JOIN (
                            SELECT player_id, SUM(assist_number) AS assist_count
                            FROM goal g
                            WHERE match_id in (SELECT id FROM match WHERE app_team_id=:#{#appTeamId})
                            GROUP BY player_id
                        ) g ON b.player_id = g.player_id
            			    WHERE g.assist_count > 0
            			    AND b.app_team_id=:#{#appTeamId}
                        GROUP BY b.player_id, g.assist_count
                        HAVING SUM(b.beer_number) > 0
                        ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getAssistBeerRatio(@Param("appTeamId") Long appTeamId);

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.beer_number) AS totalBeerNumber, g.assist_count as matchCount,
                               CAST(SUM(b.beer_number) AS FLOAT) / g.assist_count AS avgBeerPerMatch
                        FROM beer b
                        JOIN (
                            SELECT player_id, SUM(assist_number) AS assist_count
                            FROM goal g
                            INNER JOIN match m ON match_id=m.id
                                WHERE m.season_id=:#{#seasonId}
                                AND b.app_team_id=:#{#appTeamId}
                            GROUP BY player_id
                        ) g ON b.player_id = g.player_id
                        INNER JOIN match m ON match_id=m.id
                            WHERE m.season_id=:#{#seasonId}
            			    AND g.assist_count > 0
            			    AND b.app_team_id=:#{#appTeamId}
                        GROUP BY b.player_id, g.assist_count
                        HAVING SUM(b.beer_number) > 0
                        ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getAssistBeerRatio(@Param("seasonId") long seasonId, @Param("appTeamId") Long appTeamId);

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.liquor_number) AS totalBeerNumber, g.assist_count as matchCount,
                               CAST(SUM(b.liquor_number) AS FLOAT) / g.assist_count AS avgBeerPerMatch
                        FROM beer b
                        JOIN (
                            SELECT player_id, SUM(assist_number) AS assist_count
                            FROM goal g
                            WHERE match_id in (SELECT id FROM match WHERE app_team_id=:#{#appTeamId})
                            GROUP BY player_id
                        ) g ON b.player_id = g.player_id
            			    WHERE g.assist_count > 0
            			    AND b.app_team_id=:#{#appTeamId}
                        GROUP BY b.player_id, g.assist_count
                        HAVING SUM(b.liquor_number) > 0
                        ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getAssistLiquorRatio(@Param("appTeamId") Long appTeamId);

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.liquor_number) AS totalBeerNumber, g.assist_count as matchCount,
                               CAST(SUM(b.liquor_number) AS FLOAT) / g.assist_count AS avgBeerPerMatch
                        FROM beer b
                        JOIN (
                            SELECT player_id, SUM(assist_number) AS assist_count
                            FROM goal g
                            INNER JOIN match m ON match_id=m.id
                                WHERE m.season_id=:#{#seasonId}
                                AND b.app_team_id=:#{#appTeamId}
                            GROUP BY player_id
                        ) g ON b.player_id = g.player_id
                        INNER JOIN match m ON match_id=m.id
                            WHERE m.season_id=:#{#seasonId}
            			    AND g.assist_count > 0
            			    AND b.app_team_id=:#{#appTeamId}
                        GROUP BY b.player_id, g.assist_count
                        HAVING SUM(b.liquor_number) > 0
                        ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getAssistLiquorRatio(@Param("seasonId") long seasonId, @Param("appTeamId") Long appTeamId);

    @Query(value = """
                SELECT DISTINCT ON (m.date) b.*
                FROM beer b
                JOIN match m ON b.match_id = m.id
                WHERE b.app_team_id = :appTeamId
                AND (b.beer_number + b.liquor_number) = (
                    SELECT MAX(b2.beer_number + b2.liquor_number)
                    FROM beer b2
                    WHERE b2.match_id = b.match_id
                    AND b2.app_team_id = :appTeamId
                )
                ORDER BY m.date ASC, (b.beer_number + b.liquor_number) DESC
            """, nativeQuery = true)
    List<BeerEntity> findTopDrinkersByMatchOrderedByDate(@Param("appTeamId") Long appTeamId);

    @Query(value = """
                SELECT b.*
                FROM beer b
                JOIN match m ON b.match_id = m.id
                WHERE b.player_id = :playerId
                AND b.liquor_number > b.beer_number
                ORDER BY m.date ASC
                LIMIT 1
            """, nativeQuery = true)
    Optional<BeerEntity> findFirstMatchWhereLiquorMoreThanBeer(@Param("playerId") Long playerId);

    @Query(value = """
                SELECT b.*
                FROM beer b
                JOIN match m ON b.match_id = m.id
                JOIN received_fine r ON b.match_id = r.match_id AND b.player_id = r.player_id
                JOIN fine f ON r.fine_id = f.id
                WHERE b.player_id = :playerId
                AND f.name = :fineName
                AND b.beer_number > :beerNumber
                ORDER BY m.date ASC
                LIMIT 1
            """, nativeQuery = true)
    Optional<BeerEntity> findFirstMatchWhereAtLeastBeersAfterFine(@Param("playerId") Long playerId, @Param("fineName") String fineName, @Param("beerNumber") int beerNumber);

    @Query(value = """
                SELECT b.*
                FROM beer b
                JOIN match m ON b.match_id = m.id
                WHERE b.player_id = :playerId
                AND b.liquor_number >= :liquorNumber
                AND b.player_id NOT IN (
                    SELECT player_id FROM match_players mp
                    WHERE mp.match_id = (
                	SELECT m2.id FROM match m2
                	WHERE m2.date > m.date
                	ORDER BY m2.date asc
                	LIMIT 1
                	)
                )
                ORDER BY m.date ASC
                LIMIT 1;
            """, nativeQuery = true)
    Optional<BeerEntity> findBeerIfPlayerDrinksAtLeastXLiquorsAndThenNotAttendInNextMatch(@Param("playerId") Long playerId, @Param("liquorNumber") int liquorNumber);
}

