package com.jumbo.trus.repository;

import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import org.springframework.data.domain.Pageable;
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

    @Query("""
            SELECT m
            FROM match m
            ORDER BY m.id
            """)
    List<MatchEntity> getAll(Pageable pageable);

    @Query("""
            SELECT m
            FROM match m
            WHERE m.season.id = :seasonId
              AND m.appTeam.id = :appTeamId
            ORDER BY m.date DESC
            """)
    List<MatchEntity> findAllBySeasonId(
            @Param("seasonId") long seasonId,
            @Param("appTeamId") Long appTeamId
    );

    @Query("""
            SELECT m
            FROM match m
            WHERE m.appTeam.id = :appTeamId
            ORDER BY m.date DESC
            """)
    List<MatchEntity> getMatchesOrderByDateDesc(
            @Param("appTeamId") Long appTeamId,
            Pageable pageable
    );

    @Query("""
            SELECT m
            FROM match m
            WHERE m.appTeam.id = :appTeamId
            ORDER BY m.date ASC
            """)
    List<MatchEntity> getMatchesOrderByDateAsc(
            @Param("appTeamId") Long appTeamId,
            Pageable pageable
    );

    @Query("""
            SELECT m
            FROM match m
            WHERE m.footballMatch.id = :footballMatchId
              AND m.appTeam.id = :appTeamId
            """)
    Optional<MatchEntity> findAllByFootballMatchId(
            @Param("footballMatchId") long footballMatchId,
            @Param("appTeamId") Long appTeamId
    );

    @Query("""
            SELECT m
            FROM match m
            WHERE m.pkflMatch.id = :pkflMatchId
            """)
    List<MatchEntity> findAllByPkflMatchId(@Param("pkflMatchId") long pkflMatchId);

    @Query("""
            SELECT m
            FROM match m
            WHERE m.season.id = :seasonId
              AND m.appTeam.id = :appTeamId
            ORDER BY m.date DESC
            """)
    List<MatchEntity> findLastBySeasonId(
            @Param("seasonId") long seasonId,
            @Param("appTeamId") Long appTeamId,
            Pageable pageable
    );

    @Query("""
            SELECT m
            FROM match m
            WHERE m.appTeam.id = :appTeamId
            ORDER BY m.date DESC
            """)
    List<MatchEntity> findLastByAppTeamId(
            @Param("appTeamId") Long appTeamId,
            Pageable pageable
    );

    @Modifying
    @Query(value = "DELETE from match_players WHERE match_id=:#{#matchId}", nativeQuery = true)
    void deleteByPlayersInMatchByMatchId(@Param("matchId") long matchId);

    @Modifying
    @Query(value = "Update match SET season_id=" + OTHER_SEASON_ID + " WHERE season_id=:#{#seasonId}", nativeQuery = true)
    void updateSeasonId(@Param("seasonId") long seasonId);

    @Query("""
            SELECT DISTINCT m
            FROM match m
            JOIN m.playerList player
            WHERE player.id = :playerId
            ORDER BY m.date ASC
            """)
    List<MatchEntity> findFirstMatchWherePlayerAttends(
            @Param("playerId") Long playerId,
            Pageable pageable
    );

    @Query("""
            SELECT m
            FROM match m
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
