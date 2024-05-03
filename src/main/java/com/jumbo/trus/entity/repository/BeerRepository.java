package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.BeerEntity;
import com.jumbo.trus.service.beer.helper.AverageBeer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BeerRepository extends PagingAndSortingRepository<BeerEntity, Long>, JpaRepository<BeerEntity, Long>, JpaSpecificationExecutor<BeerEntity> {

    @Query(value = "SELECT * from beer LIMIT :limit", nativeQuery = true)
    List<BeerEntity> getAll(@Param("limit") int limit);

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
                    GROUP BY player_id
                ) max_beer ON b.player_id = max_beer.player_id AND b.beer_number = max_beer.max_beer_number
                WHERE beer_number > 0
            ) ranked_beer
            WHERE rn = 1
            ORDER BY beer_number DESC;
            """, nativeQuery = true)
    List<BeerEntity> getMaxBeer();

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
                    GROUP BY player_id
                ) max_beer ON b.player_id = max_beer.player_id AND b.beer_number = max_beer.max_beer_number
                INNER JOIN match m ON match_id=m.id
                WHERE m.season_id=:#{#seasonId}
                AND beer_number > 0
            ) ranked_beer
            WHERE rn = 1
            ORDER BY beer_number DESC;
            """, nativeQuery = true)
    List<BeerEntity> getMaxBeer(@Param("seasonId") long seasonId);

    @Query(value = """
            SELECT *
            FROM (
                SELECT b.*, row_number() over(partition by b.player_id order by b.liquor_number) rn
                FROM beer b
                JOIN (
                    SELECT player_id, max(liquor_number) AS max_liquor_number
                    FROM beer
                    GROUP BY player_id
                ) max_liquor ON b.player_id = max_liquor.player_id AND b.liquor_number = max_liquor.max_liquor_number
                WHERE liquor_number > 0
            ) ranked_liquor
            WHERE rn = 1
            ORDER BY liquor_number DESC;
            """, nativeQuery = true)
    List<BeerEntity> getMaxLiquor();

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
                    GROUP BY player_id
                ) max_liquor ON b.player_id = max_liquor.player_id AND b.liquor_number = max_liquor.max_liquor_number
                INNER JOIN match m ON match_id=m.id
                WHERE m.season_id=:#{#seasonId}
                AND liquor_number > 0
            ) ranked_liquor
            WHERE rn = 1
            ORDER BY liquor_number DESC;
            """, nativeQuery = true)
    List<BeerEntity> getMaxLiquor(@Param("seasonId") long seasonId);

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.beer_number) AS totalBeerNumber, mp.match_count as matchCount,
                   CAST(SUM(b.beer_number) AS FLOAT) / mp.match_count AS avgBeerPerMatch
            FROM beer b
            JOIN (
                SELECT player_id, COUNT(*) AS match_count
                FROM match_players mp
                GROUP BY player_id
            ) mp ON b.player_id = mp.player_id
            GROUP BY b.player_id, mp.match_count
            HAVING SUM(b.beer_number) > 0
            ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getAverageBeer();

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.beer_number) AS totalBeerNumber, mp.match_count as matchCount,
                   CAST(SUM(b.beer_number) AS FLOAT) / mp.match_count AS avgBeerPerMatch
            FROM beer b
            JOIN (
                SELECT player_id, COUNT(*) AS match_count
                FROM match_players mp
                INNER JOIN match m ON match_id=m.id
                WHERE m.season_id=:#{#seasonId}
                GROUP BY player_id
            ) mp ON b.player_id = mp.player_id
            INNER JOIN match m ON match_id=m.id
                WHERE m.season_id=:#{#seasonId}
            GROUP BY b.player_id, mp.match_count
            HAVING SUM(b.beer_number) > 0
            ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getAverageBeer(@Param("seasonId") long seasonId);

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.liquor_number) AS totalBeerNumber, mp.match_count as matchCount,
                   CAST(SUM(b.liquor_number) AS FLOAT) / mp.match_count AS avgBeerPerMatch
            FROM beer b
            JOIN (
                SELECT player_id, COUNT(*) AS match_count
                FROM match_players mp
                GROUP BY player_id
            ) mp ON b.player_id = mp.player_id
            GROUP BY b.player_id, mp.match_count
            HAVING SUM(b.liquor_number) > 0
            ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getAverageLiquor();

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.liquor_number) AS totalBeerNumber, mp.match_count as matchCount,
                   CAST(SUM(b.liquor_number) AS FLOAT) / mp.match_count AS avgBeerPerMatch
            FROM beer b
            JOIN (
                SELECT player_id, COUNT(*) AS match_count
                FROM match_players mp
                INNER JOIN match m ON match_id=m.id
                WHERE m.season_id=:#{#seasonId}
                GROUP BY player_id
            ) mp ON b.player_id = mp.player_id
            INNER JOIN match m ON match_id=m.id
                WHERE m.season_id=:#{#seasonId}
            GROUP BY b.player_id, mp.match_count
            HAVING SUM(b.liquor_number) > 0
            ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getAverageLiquor(@Param("seasonId") long seasonId);

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.beer_number) AS totalBeerNumber, g.goal_count as matchCount,
                               CAST(SUM(b.beer_number) AS FLOAT) / g.goal_count AS avgBeerPerMatch
                        FROM beer b
                        JOIN (
                            SELECT player_id, SUM(goal_number) AS goal_count
                            FROM goal g
                            GROUP BY player_id
                        ) g ON b.player_id = g.player_id
            			    WHERE g.goal_count > 0
                        GROUP BY b.player_id, g.goal_count
                        HAVING SUM(b.beer_number) > 0
                        ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getGoalBeerRatio();

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.beer_number) AS totalBeerNumber, g.goal_count as matchCount,
                               CAST(SUM(b.beer_number) AS FLOAT) / g.goal_count AS avgBeerPerMatch
                        FROM beer b
                        JOIN (
                            SELECT player_id, SUM(goal_number) AS goal_count
                            FROM goal g
                            INNER JOIN match m ON match_id=m.id
                                WHERE m.season_id=:#{#seasonId}
                            GROUP BY player_id
                        ) g ON b.player_id = g.player_id
                        INNER JOIN match m ON match_id=m.id
                            WHERE m.season_id=:#{#seasonId}
            			    AND g.goal_count > 0
                        GROUP BY b.player_id, g.goal_count
                        HAVING SUM(b.beer_number) > 0
                        ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getGoalBeerRatio(@Param("seasonId") long seasonId);

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.liquor_number) AS totalBeerNumber, g.goal_count as matchCount,
                               CAST(SUM(b.liquor_number) AS FLOAT) / g.goal_count AS avgBeerPerMatch
                        FROM beer b
                        JOIN (
                            SELECT player_id, SUM(goal_number) AS goal_count
                            FROM goal g
                            GROUP BY player_id
                        ) g ON b.player_id = g.player_id
            			    WHERE g.goal_count > 0
                        GROUP BY b.player_id, g.goal_count
                        HAVING SUM(b.liquor_number) > 0
                        ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getGoalLiquorRatio();

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.liquor_number) AS totalBeerNumber, g.goal_count as matchCount,
                               CAST(SUM(b.liquor_number) AS FLOAT) / g.goal_count AS avgBeerPerMatch
                        FROM beer b
                        JOIN (
                            SELECT player_id, SUM(goal_number) AS goal_count
                            FROM goal g
                            INNER JOIN match m ON match_id=m.id
                                WHERE m.season_id=:#{#seasonId}
                            GROUP BY player_id
                        ) g ON b.player_id = g.player_id
                        INNER JOIN match m ON match_id=m.id
                            WHERE m.season_id=:#{#seasonId}
            			    AND g.goal_count > 0
                        GROUP BY b.player_id, g.goal_count
                        HAVING SUM(b.liquor_number) > 0
                        ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getGoalLiquorRatio(@Param("seasonId") long seasonId);

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.beer_number) AS totalBeerNumber, g.assist_count as matchCount,
                               CAST(SUM(b.beer_number) AS FLOAT) / g.assist_count AS avgBeerPerMatch
                        FROM beer b
                        JOIN (
                            SELECT player_id, SUM(assist_number) AS assist_count
                            FROM goal g
                            GROUP BY player_id
                        ) g ON b.player_id = g.player_id
            			    WHERE g.assist_count > 0
                        GROUP BY b.player_id, g.assist_count
                        HAVING SUM(b.beer_number) > 0
                        ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getAssistBeerRatio();

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.beer_number) AS totalBeerNumber, g.assist_count as matchCount,
                               CAST(SUM(b.beer_number) AS FLOAT) / g.assist_count AS avgBeerPerMatch
                        FROM beer b
                        JOIN (
                            SELECT player_id, SUM(assist_number) AS assist_count
                            FROM goal g
                            INNER JOIN match m ON match_id=m.id
                                WHERE m.season_id=:#{#seasonId}
                            GROUP BY player_id
                        ) g ON b.player_id = g.player_id
                        INNER JOIN match m ON match_id=m.id
                            WHERE m.season_id=:#{#seasonId}
            			    AND g.assist_count > 0
                        GROUP BY b.player_id, g.assist_count
                        HAVING SUM(b.beer_number) > 0
                        ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getAssistBeerRatio(@Param("seasonId") long seasonId);

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.liquor_number) AS totalBeerNumber, g.assist_count as matchCount,
                               CAST(SUM(b.liquor_number) AS FLOAT) / g.assist_count AS avgBeerPerMatch
                        FROM beer b
                        JOIN (
                            SELECT player_id, SUM(assist_number) AS assist_count
                            FROM goal g
                            GROUP BY player_id
                        ) g ON b.player_id = g.player_id
            			    WHERE g.assist_count > 0
                        GROUP BY b.player_id, g.assist_count
                        HAVING SUM(b.liquor_number) > 0
                        ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getAssistLiquorRatio();

    @Query(value = """
            SELECT b.player_id as playerId, SUM(b.liquor_number) AS totalBeerNumber, g.assist_count as matchCount,
                               CAST(SUM(b.liquor_number) AS FLOAT) / g.assist_count AS avgBeerPerMatch
                        FROM beer b
                        JOIN (
                            SELECT player_id, SUM(assist_number) AS assist_count
                            FROM goal g
                            INNER JOIN match m ON match_id=m.id
                                WHERE m.season_id=:#{#seasonId}
                            GROUP BY player_id
                        ) g ON b.player_id = g.player_id
                        INNER JOIN match m ON match_id=m.id
                            WHERE m.season_id=:#{#seasonId}
            			    AND g.assist_count > 0
                        GROUP BY b.player_id, g.assist_count
                        HAVING SUM(b.liquor_number) > 0
                        ORDER BY avgBeerPerMatch DESC;
            """, nativeQuery = true)
    List<AverageBeer> getAssistLiquorRatio(@Param("seasonId") long seasonId);


}

