package com.jumbo.trus.entity.repository.football;

import com.jumbo.trus.entity.football.FootballMatchEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface FootballMatchRepository extends JpaRepository<FootballMatchEntity, Long> {

    Optional<FootballMatchEntity> findByHomeTeam_IdAndRoundAndLeagueId(Long homeTeamId, Integer round, Long leagueId);

    @Transactional
    @Modifying
    @Query("DELETE FROM football_match f " +
            "WHERE f.league.id = :leagueId AND f.id NOT IN :ids")
    int deleteByLeagueIdAndMatchIdNotIn(@Param("leagueId") Long leagueId,
                                             @Param("ids") List<Long> ids);

    @Query(value = "SELECT * FROM football_match WHERE date > CURRENT_TIMESTAMP AND (home_team_id = :teamId OR away_team_id = :teamId) ORDER BY DATE ASC LIMIT 1", nativeQuery = true)
    FootballMatchEntity findNextMatch(@Param("teamId") Long teamId);

    @Query(value = "SELECT * FROM football_match WHERE date < CURRENT_TIMESTAMP AND (home_team_id = :teamId OR away_team_id = :teamId) ORDER BY DATE DESC LIMIT 1", nativeQuery = true)
    FootballMatchEntity findLastMatch(@Param("teamId") Long teamId);

    @Query(value = "SELECT * FROM football_match WHERE date > CURRENT_TIMESTAMP AND (home_team_id = :teamId OR away_team_id = :teamId) AND already_played = false ORDER BY DATE ASC", nativeQuery = true)
    List<FootballMatchEntity> findNonPlayedNextMatches(@Param("teamId") Long teamId);

    @Query(value = "SELECT * FROM football_match WHERE date < CURRENT_TIMESTAMP AND (home_team_id = :teamId OR away_team_id = :teamId) AND league_id = :leagueId ORDER BY DATE DESC", nativeQuery = true)
    List<FootballMatchEntity> findPastMatchesInLeague(@Param("teamId") Long teamId, @Param("leagueId") Long leagueId);

    @Query(value = "SELECT * FROM football_match WHERE already_played = true AND ((home_team_id = :teamId1 AND away_team_id = :teamId2) OR (home_team_id = :teamId2 AND away_team_id = :teamId1)) ORDER BY DATE DESC", nativeQuery = true)
    List<FootballMatchEntity> findAlreadyPlayedMatchesOfTwoTeams(@Param("teamId1") Long teamId1, @Param("teamId2") Long teamId2);

    @Query(value = "SELECT * FROM football_match WHERE (home_team_id = :teamId OR away_team_id = :teamId) AND date BETWEEN :startDate AND :endDate LIMIT 1", nativeQuery = true)
    FootballMatchEntity findByDate(@Param("teamId") Long teamId, @Param("startDate") Date startDate, @Param("endDate")Date endDate);
}
