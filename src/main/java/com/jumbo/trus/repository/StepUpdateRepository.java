package com.jumbo.trus.repository;

import com.jumbo.trus.entity.StepUpdateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StepUpdateRepository extends JpaRepository<StepUpdateEntity, Long> {

    Optional<StepUpdateEntity> findByUserIdAndDate(Long userId, LocalDate date);

    List<StepUpdateEntity> findAllByUserIdAndDateBetweenOrderByDateAsc(Long userId, LocalDate from, LocalDate to);

    @Query("""
            SELECT new com.jumbo.trus.dto.step.StepLeaderboardDTO(
                u.id,
                u.name,
                COALESCE(SUM(s.stepNumber), 0),
                COUNT(s.id),
                COALESCE(AVG(s.stepNumber), 0.0)
            )
            FROM StepConsentEntity c
            JOIN c.user u
            LEFT JOIN StepUpdateEntity s ON s.user = u AND s.date BETWEEN :from AND :to
            WHERE c.appTeam.id = :appTeamId AND c.enabled = true
              AND EXISTS (SELECT r.id FROM UserTeamRole r WHERE r.user = u AND r.appTeam.id = :appTeamId)
            GROUP BY u.id, u.name
            ORDER BY COALESCE(SUM(s.stepNumber), 0) DESC, u.name ASC
            """)
    List<com.jumbo.trus.dto.step.StepLeaderboardDTO> leaderboard(
            @Param("appTeamId") Long appTeamId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT
                COALESCE(SUM(s.stepNumber), 0) AS stepCount,
                COUNT(DISTINCT s.date) AS dayCount
            FROM StepUpdateEntity s
            WHERE s.date BETWEEN :from AND :to
              AND EXISTS (
                  SELECT r.id
                  FROM UserTeamRole r
                  WHERE r.user = s.user
                    AND r.appTeam.id = :appTeamId
                    AND r.player.id = :playerId
              )
              AND EXISTS (
                  SELECT c.id
                  FROM StepConsentEntity c
                  WHERE c.user = s.user
                    AND c.appTeam.id = :appTeamId
                    AND c.enabled = true
              )
            """)
    StepPeriodStatsProjection playerStats(
            @Param("playerId") Long playerId,
            @Param("appTeamId") Long appTeamId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query(value = """
            SELECT
                timeline.cumulative_steps AS "stepCount",
                timeline.day_count AS "dayCount"
            FROM (
                SELECT
                    s.step_date,
                    SUM(s.step_number) OVER (
                        ORDER BY s.step_date
                        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                    ) AS cumulative_steps,
                    ROW_NUMBER() OVER (ORDER BY s.step_date) AS day_count
                FROM step_update s
                WHERE EXISTS (
                    SELECT 1
                    FROM user_team_role r
                    WHERE r.user_id = s.user_id
                      AND r.app_team_id = :appTeamId
                      AND r.player_id = :playerId
                )
                  AND EXISTS (
                    SELECT 1
                    FROM step_consent c
                    WHERE c.user_id = s.user_id
                      AND c.app_team_id = :appTeamId
                      AND c.enabled = true
                )
            ) timeline
            WHERE timeline.cumulative_steps >= :threshold
            ORDER BY timeline.step_date
            LIMIT 1
            """, nativeQuery = true)
    Optional<StepPeriodStatsProjection> milestoneStats(
            @Param("playerId") Long playerId,
            @Param("appTeamId") Long appTeamId,
            @Param("threshold") long threshold);

    @Query("""
            SELECT
                r.player.id AS playerId,
                SUM(s.stepNumber) AS stepCount
            FROM StepUpdateEntity s
            JOIN UserTeamRole r ON r.user = s.user
            JOIN StepConsentEntity c ON c.user = s.user AND c.appTeam = r.appTeam
            WHERE r.appTeam.id = :appTeamId
              AND r.player IS NOT NULL
              AND c.enabled = true
              AND s.date BETWEEN :from AND :to
            GROUP BY r.player.id
            HAVING SUM(s.stepNumber) > 0
            ORDER BY SUM(s.stepNumber) DESC, r.player.id ASC
            """)
    List<PlayerStepTotalProjection> playerTotals(
            @Param("appTeamId") Long appTeamId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}

