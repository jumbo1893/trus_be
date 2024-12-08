package com.jumbo.trus.entity.repository.football;

import com.jumbo.trus.entity.football.detail.BestScorerView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BestScorerViewRepository extends JpaRepository<BestScorerView, Long> {

    @Query(value = "SELECT * FROM best_scorer_view WHERE team_id = :teamId AND league_id = :leagueId ORDER BY total_goals DESC LIMIT 1", nativeQuery = true)
    Optional<BestScorerView> findBestScorerByTeamAndLeague(@Param("teamId") Long teamId, @Param("leagueId") Long leagueId);
}
