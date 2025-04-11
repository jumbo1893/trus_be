package com.jumbo.trus.entity.repository.view;

import com.jumbo.trus.entity.football.view.BestScorerEntity;
import com.jumbo.trus.entity.football.view.FootballSumIndividualStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FootballSumIndividualStatsRepository extends JpaRepository<FootballSumIndividualStatsEntity, Long> {

    @Query("SELECT f FROM FootballSumIndividualStatsEntity f " +
            "WHERE f.team.id = :teamId " +
            "AND f.league.id = f.team.currentLeague.id")
    List<FootballSumIndividualStatsEntity> findAllByTeamInCurrentLeague(@Param("teamId") Long teamId);

    List<FootballSumIndividualStatsEntity> findAllByTeamId(Long teamId);

    @Query(value = "SELECT " +
            "fmp.player_id AS player_id, " +
            "fmp.team_id AS team_id, " +
            "NULL AS league_id, " +
            "SUM(fmp.goals) AS goals, " +
            "SUM(fmp.own_goals) AS own_goals, " +
            "SUM(fmp.received_goals) AS received_goals, " +
            "SUM(fmp.goalkeeping_minutes) AS goalkeeping_minutes, " +
            "SUM(fmp.yellow_cards) AS yellow_cards, " +
            "SUM(fmp.red_cards) AS red_cards, " +
            "SUM(fmp.matches) AS matches, " +
            "SUM(fmp.best_players) AS best_players, " +
            "SUM(fmp.hattricks) AS hattricks, " +
            "SUM(fmp.clean_sheets) AS clean_sheets, " +
            "SUM(fmp.match_points) AS match_points " +
            "FROM football_sum_individual_stats fmp " +
            "WHERE fmp.team_id = :teamId " +
            "GROUP BY fmp.player_id, fmp.team_id " +
            "ORDER BY matches DESC",
            nativeQuery = true)
    List<FootballSumIndividualStatsEntity> findPlayerStatsByTeamId(@Param("teamId") Long teamId);

    @Query(value = "SELECT " +
            "fmp.player_id AS player_id, " +
            "fmp.team_id AS team_id, " +
            "NULL AS league_id, " +
            "SUM(fmp.goals) AS goals, " +
            "SUM(fmp.own_goals) AS own_goals, " +
            "SUM(fmp.received_goals) AS received_goals, " +
            "SUM(fmp.goalkeeping_minutes) AS goalkeeping_minutes, " +
            "SUM(fmp.yellow_cards) AS yellow_cards, " +
            "SUM(fmp.red_cards) AS red_cards, " +
            "SUM(fmp.matches) AS matches, " +
            "SUM(fmp.best_players) AS best_players, " +
            "SUM(fmp.hattricks) AS hattricks, " +
            "SUM(fmp.clean_sheets) AS clean_sheets, " +
            "SUM(fmp.match_points) AS match_points " +
            "FROM football_sum_individual_stats fmp " +
            "WHERE fmp.team_id = :teamId " +
            "AND fmp.player_id = :playerId " +
            "GROUP BY fmp.player_id, fmp.team_id " +
            "ORDER BY matches DESC",
            nativeQuery = true)
    FootballSumIndividualStatsEntity findPlayerStatsByTeamIdAndPlayerId(@Param("teamId") Long teamId, @Param("playerId") Long playerId);
}
