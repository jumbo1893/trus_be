package com.jumbo.trus.repository.view;

import com.jumbo.trus.entity.football.view.BestScorerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BestScorerViewRepository extends JpaRepository<BestScorerEntity, Long> {

    @Query(value = """
    SELECT
        fmp.team_id AS team_id,
        fm.league_id AS league_id,
        fmp.player_id AS player_id,
        SUM(fmp.goals) AS total_goals
    FROM football_match_player fmp
    JOIN football_match fm ON fm.id = fmp.match_id
    WHERE
        fmp.team_id = :teamId
        AND fm.league_id = :leagueId
        AND fmp.goals IS NOT NULL
    GROUP BY fmp.team_id, fm.league_id, fmp.player_id
    ORDER BY total_goals DESC
    LIMIT 1
""", nativeQuery = true)
    Optional<BestScorerEntity> findBestScorerByTeamAndLeague(
            @Param("teamId") Long teamId,
            @Param("leagueId") Long leagueId
    );

}
