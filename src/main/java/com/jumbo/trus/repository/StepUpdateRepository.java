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
}

