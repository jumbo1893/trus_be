package com.jumbo.trus.entity.repository.football;

import com.jumbo.trus.entity.football.FootballMatchPlayerEntity;
import com.jumbo.trus.entity.football.FootballMatchPlayerEntity_;
import com.jumbo.trus.entity.pkfl.PkflIndividualStatsEntity_;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FootballMatchPlayerRepository extends JpaRepository<FootballMatchPlayerEntity, Long> {

    @Query("SELECT f.id " +
            "FROM football_match_player f " +
            "WHERE f.team.id = :teamId AND f.match.id = :matchId AND f.player.id = :playerId")
    Optional<Long> findFirstIdByTeamAndMatchAndPlayer(@Param("teamId") Long teamId,
                                                      @Param("matchId") Long matchId,
                                                      @Param("playerId") Long playerId);

    @Transactional
    @Modifying
    @Query("DELETE FROM football_match_player f " +
            "WHERE f.match.id = :matchId AND f.id NOT IN :ids")
    int deleteByMatchIdAndMatchPlayerIdNotIn(@Param("matchId") Long matchId,
                                             @Param("ids") List<Long> ids);

    List<FootballMatchPlayerEntity> findAllByTeamId(long teamId);

    @Query("SELECT fmp FROM football_match_player fmp " +
            "WHERE fmp.player.id = :playerId" +
            "  AND fmp.team.id = :teamId" +
            "  AND (fmp.yellowCardComment IS NOT NULL" +
            "       OR fmp.redCardComment IS NOT NULL)")
    List<FootballMatchPlayerEntity> findAllCardComments(@Param("teamId") Long teamId, @Param("playerId") Long playerId);

    @Query("SELECT fmp FROM football_match_player fmp " +
            "JOIN football_match fm " +
            "ON fmp.match.id = fm.id " +
            "WHERE fmp.player.id = :playerId " +
            "  AND fmp.team.id = :teamId " +
            "  AND (fmp.yellowCardComment IS NOT NULL " +
            "       OR fmp.redCardComment IS NOT NULL) " +
            "  AND fm.league.id = :leagueId")
    List<FootballMatchPlayerEntity> findAllCardCommentsInLeague(@Param("teamId") Long teamId,
                                                                @Param("leagueId") Long leagueId,
                                                                @Param("playerId") Long playerId);

    @Query(value = "SELECT * FROM football_match_player i WHERE i.goals = (SELECT MAX(subI.goals) FROM football_match_player subI WHERE subI.player_id = :playerId) AND i.player_id = :playerId", nativeQuery = true)
    List<FootballMatchPlayerEntity> findAllWithHighestGoals(@Param("playerId") long playerId);

    @Query("SELECT AVG(CASE WHEN " + FootballMatchPlayerEntity_.BEST_PLAYER + " THEN 1 ELSE 0 END) as total, i.match.referee as text FROM football_match_player i WHERE i.player.id = :playerId GROUP BY i.match.referee HAVING COUNT(i.match.referee) > 3 ORDER BY total DESC LIMIT 3")
    List<Object[]> findMostAverageBestPlayersPerReferee(@Param("playerId") long playerId);

    @Query("SELECT AVG(CASE WHEN " + FootballMatchPlayerEntity_.BEST_PLAYER + " THEN 1 ELSE 0 END) as total, i.match.referee as text FROM football_match_player i WHERE i.player.id = :playerId GROUP BY i.match.referee HAVING COUNT(i.match.referee) > 3 ORDER BY total ASC LIMIT 3")
    List<Object[]> findLeastAverageBestPlayersPerReferee(@Param("playerId") long playerId);

    @Query("SELECT AVG(" + FootballMatchPlayerEntity_.GOALS + ") as total, i.match.stadium as text FROM football_match_player i WHERE i.player.id = :playerId GROUP BY i.match.stadium HAVING COUNT(i.match.stadium) > 3 ORDER BY total DESC LIMIT 3")
    List<Object[]> findMostAverageGoalsPerStadium(@Param("playerId") long playerId);

    @Query("SELECT AVG(i.goals) as total, " +
            "CASE " +
            "  WHEN i.match.homeTeam.id = :teamId THEN i.match.awayTeam.name " +
            "  ELSE i.match.homeTeam.name " +
            "END as opponent " +
            "FROM football_match_player i " +
            "WHERE i.player.id = :playerId " +
            "  AND (i.match.homeTeam.id = :teamId OR i.match.awayTeam.id = :teamId) " +
            "GROUP BY opponent " +
            "HAVING COUNT(i.id) > 1 " +
            "ORDER BY total DESC " +
            "LIMIT 3")
    List<Object[]> findMostAverageGoalsPerOpponent(@Param("teamId") long teamId, @Param("playerId") long playerId);

    @Query(value = """
            SELECT f.* FROM football_match_player f
            JOIN football_match m on f.match_id = m.id
            WHERE f.best_player is true
            AND f.goals < 1
            AND f.player_id = :playerId
            ORDER BY m.date ASC
            LIMIT 1
            """
           , nativeQuery = true)
    Optional<FootballMatchPlayerEntity> findBestPlayerWithoutGoalsByPlayer(@Param("playerId") long playerId);

    @Query(value = """
             WITH RankedMatches AS (
                            SELECT fmp.*, fm.date,
                                   LAG(fmp.goals > 0) OVER (PARTITION BY fmp.player_id ORDER BY fm.date) AS prev_match_goals,
                                   LAG(fmp.goals > 0, 2) OVER (PARTITION BY fmp.player_id ORDER BY fm.date) AS prev_prev_match_goals
                            FROM football_match_player fmp
                            JOIN football_match fm ON fmp.match_id = fm.id
                            WHERE fmp.player_id = :playerId
                              AND (fm.home_team_id = :teamId OR fm.away_team_id = :teamId)
                        )
                        SELECT * FROM RankedMatches
                        WHERE goals > 0
                          AND prev_match_goals IS TRUE
                          AND prev_prev_match_goals IS TRUE
                        ORDER BY date ASC
                        LIMIT 1;
            
            """
            , nativeQuery = true)
    Optional<FootballMatchPlayerEntity> findIfPlayerScoresInThreeMatchesInRow(@Param("playerId") long playerId, @Param("teamId") long teamId);

}
