package com.jumbo.trus.repository;

import com.jumbo.trus.entity.PlannedMatchEntity;
import com.jumbo.trus.entity.PlannedMatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface PlannedMatchRepository extends JpaRepository<PlannedMatchEntity, Long> {
    Optional<PlannedMatchEntity> findByIdAndAppTeamId(Long id, Long appTeamId);
    Optional<PlannedMatchEntity> findByAppTeamIdAndFootballMatchId(Long appTeamId, Long footballMatchId);
    Optional<PlannedMatchEntity> findFirstByAppTeamIdAndStatusAndDateAfterOrderByDateAsc(
            Long appTeamId, PlannedMatchStatus status, Date date);
    List<PlannedMatchEntity> findTop2ByAppTeamIdAndStatusAndDateAfterOrderByDateAsc(
            Long appTeamId, PlannedMatchStatus status, Date date);
    List<PlannedMatchEntity> findAllByAppTeamIdOrderByDateAsc(Long appTeamId);

    @Query("""
            select p from PlannedMatchEntity p
            left join fetch p.footballMatch f
            left join fetch f.homeTeam
            left join fetch f.awayTeam
            where p.appTeam.id = :teamId
              and p.status = :status and p.date > :now
            order by p.date asc
            """)
    List<PlannedMatchEntity> findUpcomingForTeam(@Param("teamId") Long teamId,
            @Param("status") PlannedMatchStatus status, @Param("now") Date now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PlannedMatchEntity p where p.id = :id and p.appTeam.id = :teamId")
    Optional<PlannedMatchEntity> lockForTeam(@Param("id") Long id, @Param("teamId") Long teamId);

    @Modifying
    @Query(value = """
            DELETE FROM planned_match p
            WHERE p.football_match_id IN (
                SELECT f.id FROM football_match f
                WHERE f.league_id = :leagueId AND f.id NOT IN (:footballMatchIds)
            ) AND p.match_id IS NULL
            """, nativeQuery = true)
    void deleteObsoleteByLeague(@Param("leagueId") Long leagueId,
                                @Param("footballMatchIds") List<Long> footballMatchIds);
}
