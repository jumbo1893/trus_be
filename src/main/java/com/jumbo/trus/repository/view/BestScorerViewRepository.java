package com.jumbo.trus.repository.view;

import com.jumbo.trus.entity.football.view.BestScorerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BestScorerViewRepository extends JpaRepository<BestScorerEntity, Long> {

    @Query(value = "SELECT * FROM best_scorer_view WHERE team_id = :teamId AND league_id = :leagueId ORDER BY total_goals DESC LIMIT 1", nativeQuery = true)
    Optional<BestScorerEntity> findBestScorerByTeamAndLeague(@Param("teamId") Long teamId, @Param("leagueId") Long leagueId);
}
