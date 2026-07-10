package com.jumbo.trus.repository;

import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.jumbo.trus.config.Config.OTHER_SEASON_ID;

public interface MatchRepository extends PagingAndSortingRepository<MatchEntity, Long>, JpaRepository<MatchEntity, Long>, JpaSpecificationExecutor<MatchEntity> {

    @Query(value = "SELECT * from match LIMIT :limit", nativeQuery = true)
    List<MatchEntity> getAll(@Param("limit") int limit);

    @Query(value = "SELECT * from match where season_id = :seasonId AND app_team_id=:#{#appTeamId} ORDER BY DATE DESC", nativeQuery = true)
    List<MatchEntity> findAllBySeasonId(@Param("seasonId") long seasonId, @Param("appTeamId") Long appTeamId);

    @Query(value = "SELECT * from match WHERE app_team_id=:#{#appTeamId} ORDER BY DATE DESC LIMIT :limit", nativeQuery = true)
    List<MatchEntity> getMatchesOrderByDateDesc(@Param("limit") int limit, @Param("appTeamId") Long appTeamId);

    @Query(value = "SELECT * from match WHERE app_team_id=:#{#appTeamId} ORDER BY DATE ASC LIMIT :limit", nativeQuery = true)
    List<MatchEntity> getMatchesOrderByDateAsc(@Param("limit") int limit, @Param("appTeamId") Long appTeamId);

    @Query(value = "SELECT * from match WHERE football_match_id=:#{#footballMatchId} AND app_team_id=:#{#appTeamId}", nativeQuery = true)
    Optional<MatchEntity> findAllByFootballMatchId(@Param("footballMatchId") long footballMatchId, @Param("appTeamId") Long appTeamId);

    @Query(value = "SELECT * from match WHERE pkfl_match_id=:#{#footballMatchId}", nativeQuery = true)
    List<MatchEntity> findAllByPkflMatchId(@Param("footballMatchId") long footballMatchId);

    @Query(value = "SELECT * from match WHERE season_id=:#{#season} AND app_team_id=:#{#appTeamId} ORDER BY DATE DESC LIMIT 1", nativeQuery = true)
    MatchEntity getLastMatchBySeasonId(@Param("season") long seasonId, @Param("appTeamId") Long appTeamId);

    @Modifying
    @Query(value = "DELETE from match_players WHERE match_id=:#{#matchId}", nativeQuery = true)
    void deleteByPlayersInMatchByMatchId(@Param("matchId") long matchId);

    @Modifying
    @Query(value = "Update match SET season_id=" + OTHER_SEASON_ID + " WHERE season_id=:#{#seasonId}", nativeQuery = true)
    void updateSeasonId(@Param("seasonId") long seasonId);

    @Query(value = """
                SELECT * FROM match m
                 JOIN match_players mp ON mp.match_id = m.id
                 WHERE mp.player_id = :playerId
                 ORDER BY m.date asc
                 LIMIT 1
            """, nativeQuery = true)
    Optional<MatchEntity> findFirstMatchWherePlayerAttends(@Param("playerId") Long playerId);

    @Query("""
    SELECT m FROM match m
    WHERE m.appTeam = :appTeam
      AND m.footballMatch.date BETWEEN :from AND :to
""")
    Optional<MatchEntity> findMatchByTimeBetween(
            @Param("appTeam") AppTeamEntity appTeam,
            @Param("from") Date from,
            @Param("to") Date to
    );

    @Query(value = """
            SELECT DISTINCT player_id
            FROM (
                SELECT mp.player_id
                FROM match_players mp
                WHERE mp.match_id IN (:matchIds)

                UNION

                SELECT b.player_id
                FROM beer b
                WHERE b.match_id IN (:matchIds)

                UNION

                SELECT g.player_id
                FROM goal g
                WHERE g.match_id IN (:matchIds)

                UNION

                SELECT rf.player_id
                FROM received_fine rf
                WHERE rf.match_id IN (:matchIds)

                UNION

                SELECT p.id AS player_id
                FROM match m
                JOIN football_match_player fmp ON fmp.match_id = m.football_match_id
                JOIN player p ON p.football_player_id = fmp.player_id
                WHERE m.id IN (:matchIds)
            ) affected_players
            WHERE player_id IS NOT NULL
            """, nativeQuery = true)
    List<Long> findAffectedPlayerIdsByMatchIds(@Param("matchIds") Iterable<Long> matchIds);

    @Query(value = """
            SELECT DISTINCT season_id
            FROM match
            WHERE id IN (:matchIds)
              AND season_id IS NOT NULL
            """, nativeQuery = true)
    List<Long> findSeasonIdsByMatchIds(@Param("matchIds") Iterable<Long> matchIds);

}
