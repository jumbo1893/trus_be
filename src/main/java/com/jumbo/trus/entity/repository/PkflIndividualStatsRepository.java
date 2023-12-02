package com.jumbo.trus.entity.repository;

import com.jumbo.trus.entity.pkfl.PkflIndividualStatsEntity;
import com.jumbo.trus.entity.pkfl.PkflIndividualStatsEntity_;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PkflIndividualStatsRepository extends JpaRepository<PkflIndividualStatsEntity, Long> {

    @Query(value = "SELECT * FROM pkfl_individual_stats WHERE player_id= :#{#player} LIMIT 1", nativeQuery = true)
    PkflIndividualStatsEntity getPlayerByPlayerId(@Param("player") long playerId);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM pkfl_individual_stats i WHERE i.player.id = :playerId AND i.match.id = :matchId")
    boolean existsByPlayerIdAndMatchId(@Param("playerId") long playerId, @Param("matchId") long matchId);

    @Query("SELECT match.id FROM pkfl_individual_stats i WHERE i.player.id = :playerId")
    List<Long> findAllMatchesByPlayerId(@Param("playerId") long playerId);

    @Query(value = "SELECT * FROM pkfl_individual_stats i WHERE i.goals = (SELECT MAX(subI.goals) FROM pkfl_individual_stats subI WHERE subI.player_id = :playerId) AND i.player_id = :playerId", nativeQuery = true)
    List<PkflIndividualStatsEntity> findAllWithHighestGoals(@Param("playerId") long playerId);

    @Query("SELECT AVG(CASE WHEN " + PkflIndividualStatsEntity_.BEST_PLAYER + " THEN 1 ELSE 0 END) as total, i.match.referee.name as text FROM pkfl_individual_stats i WHERE i.player.id = :playerId GROUP BY i.match.referee.name HAVING COUNT(i.match.referee.name) > 3 ORDER BY total DESC LIMIT 3")
    List<Object[]> findMostAverageBestPlayersPerReferee(@Param("playerId") long playerId);

    @Query("SELECT AVG(CASE WHEN " + PkflIndividualStatsEntity_.BEST_PLAYER + " THEN 1 ELSE 0 END) as total, i.match.referee.name as text FROM pkfl_individual_stats i WHERE i.player.id = :playerId GROUP BY i.match.referee.name HAVING COUNT(i.match.referee.name) > 3 ORDER BY total ASC LIMIT 3")
    List<Object[]> findLeastAverageBestPlayersPerReferee(@Param("playerId") long playerId);

    @Query("SELECT AVG(" + PkflIndividualStatsEntity_.GOALS + ") as total, i.match.stadium.name as text FROM pkfl_individual_stats i WHERE i.player.id = :playerId GROUP BY i.match.stadium.name HAVING COUNT(i.match.stadium.name) > 3 ORDER BY total DESC LIMIT 3")
    List<Object[]> findMostAverageGoalsPerStadium(@Param("playerId") long playerId);

    @Query("SELECT AVG(" + PkflIndividualStatsEntity_.GOALS + ") as total, i.match.opponent.name as text FROM pkfl_individual_stats i WHERE i.player.id = :playerId GROUP BY i.match.opponent.name HAVING COUNT(i.match.opponent.name) > 1 ORDER BY total DESC LIMIT 3")
    List<Object[]> findMostAverageGoalsPerOpponent(@Param("playerId") long playerId);

    @Query("SELECT SUM("+PkflIndividualStatsEntity_.GOALS+") as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId")
    int getGoalsSum(@Param("playerId") long playerId);

    @Query("SELECT SUM("+PkflIndividualStatsEntity_.GOALS+") as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId AND i.match.id IN :matchIds")
    int getGoalsSum(@Param("playerId") long playerId, @Param("matchIds") List<Long> matchIds);

    @Query("SELECT SUM("+PkflIndividualStatsEntity_.RECEIVED_GOALS+") as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId")
    int getReceivedGoalsSum(@Param("playerId") long playerId);

    @Query("SELECT SUM("+PkflIndividualStatsEntity_.RECEIVED_GOALS+") as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId AND i.match.id IN :matchIds")
    int getReceivedGoalsSum(@Param("playerId") long playerId, @Param("matchIds") List<Long> matchIds);

    @Query("SELECT SUM("+PkflIndividualStatsEntity_.OWN_GOALS+") as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId")
    int getOwnGoalsSum(@Param("playerId") long playerId);

    @Query("SELECT SUM("+PkflIndividualStatsEntity_.OWN_GOALS+") as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId AND i.match.id IN :matchIds")
    int getOwnGoalsSum(@Param("playerId") long playerId, @Param("matchIds") List<Long> matchIds);

    @Query("SELECT SUM("+PkflIndividualStatsEntity_.GOALKEEPING_MINUTES+") as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId")
    int getGoalkeepingMinutesSum(@Param("playerId") long playerId);

    @Query("SELECT SUM("+PkflIndividualStatsEntity_.GOALKEEPING_MINUTES+") as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId AND i.match.id IN :matchIds")
    int getGoalkeepingMinutesSum(@Param("playerId") long playerId, @Param("matchIds") List<Long> matchIds);

    @Query("SELECT SUM("+PkflIndividualStatsEntity_.YELLOW_CARDS+") as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId")
    int getYellowCardsSum(@Param("playerId") long playerId);

    @Query("SELECT SUM("+PkflIndividualStatsEntity_.YELLOW_CARDS+") as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId AND i.match.id IN :matchIds")
    int getYellowCardsSum(@Param("playerId") long playerId, @Param("matchIds") List<Long> matchIds);

    @Query("SELECT SUM("+PkflIndividualStatsEntity_.RED_CARDS+") as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId")
    int getRedCardsSum(@Param("playerId") long playerId);

    @Query("SELECT SUM("+PkflIndividualStatsEntity_.RED_CARDS+") as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId AND i.match.id IN :matchIds")
    int getRedCardsSum(@Param("playerId") long playerId, @Param("matchIds") List<Long> matchIds);

    @Query("SELECT SUM(CASE WHEN " + PkflIndividualStatsEntity_.BEST_PLAYER + " THEN 1 ELSE 0 END) as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId")
    int getBestPlayerSum(@Param("playerId") long playerId);

    @Query("SELECT SUM(CASE WHEN " + PkflIndividualStatsEntity_.BEST_PLAYER + " THEN 1 ELSE 0 END) as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId AND i.match.id IN :matchIds")
    int getBestPlayerSum(@Param("playerId") long playerId, @Param("matchIds") List<Long> matchIds);

    @Query("SELECT SUM(CASE WHEN " + PkflIndividualStatsEntity_.HATTRICK + " THEN 1 ELSE 0 END) as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId")
    int getHattrickSum(@Param("playerId") long playerId);

    @Query("SELECT SUM(CASE WHEN " + PkflIndividualStatsEntity_.HATTRICK + " THEN 1 ELSE 0 END) as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId AND i.match.id IN :matchIds")
    int getHattrickSum(@Param("playerId") long playerId, @Param("matchIds") List<Long> matchIds);

    @Query("SELECT SUM(CASE WHEN " + PkflIndividualStatsEntity_.CLEAN_SHEET + " THEN 1 ELSE 0 END) as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId")
    int getCleanSheetSum(@Param("playerId") long playerId);

    @Query("SELECT SUM(CASE WHEN " + PkflIndividualStatsEntity_.CLEAN_SHEET + " THEN 1 ELSE 0 END) as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId AND i.match.id IN :matchIds")
    int getCleanSheetSum(@Param("playerId") long playerId, @Param("matchIds") List<Long> matchIds);

    @Query("SELECT COUNT(id) as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId")
    int getCount(@Param("playerId") long playerId);

    @Query("SELECT COUNT(id) as total FROM pkfl_individual_stats i WHERE i.player.id = :playerId AND i.match.id IN :matchIds")
    int getCount(@Param("playerId") long playerId, @Param("matchIds") List<Long> matchIds);
    @Query(value = "SELECT * FROM pkfl_individual_stats WHERE player_id= :#{#player} AND yellow_card_comment IS NOT NULL", nativeQuery = true)
    List<PkflIndividualStatsEntity> findAllYellowCardCommentsByPlayerId(@Param("player") long playerId);

    @Query(value = "SELECT * FROM pkfl_individual_stats WHERE player_id= :#{#player} AND red_card_comment IS NOT NULL", nativeQuery = true)
    List<PkflIndividualStatsEntity> findAllRedCardCommentsByPlayerId(@Param("player") long playerId);

    @Query(value = "SELECT * FROM pkfl_individual_stats WHERE player_id= :#{#player} AND match_id IN :matchIds AND yellow_card_comment IS NOT NULL", nativeQuery = true)
    List<PkflIndividualStatsEntity> findAllYellowCardCommentsByPlayerId(@Param("player") long playerId, @Param("matchIds") List<Long> matchIds);

    @Query(value = "SELECT * FROM pkfl_individual_stats WHERE player_id= :#{#player} AND match_id IN :matchIds AND red_card_comment IS NOT NULL", nativeQuery = true)
    List<PkflIndividualStatsEntity> findAllRedCardCommentsByPlayerId(@Param("player") long playerId, @Param("matchIds") List<Long> matchIds);
}
