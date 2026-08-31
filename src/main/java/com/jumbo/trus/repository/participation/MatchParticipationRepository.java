package com.jumbo.trus.repository.participation;

import com.jumbo.trus.entity.participation.MatchParticipationEntity;
import com.jumbo.trus.entity.participation.MatchParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchParticipationRepository extends JpaRepository<MatchParticipationEntity, Long> {

    Optional<MatchParticipationEntity> findByFootballMatchIdAndAppTeamIdAndPlayerId(
            Long footballMatchId,
            Long appTeamId,
            Long playerId
    );

    List<MatchParticipationEntity> findAllByFootballMatchIdAndAppTeamIdOrderByPlayerNameAsc(
            Long footballMatchId,
            Long appTeamId
    );

    List<MatchParticipationEntity> findAllByFootballMatchIdAndAppTeamIdAndStatusOrderByPlayerNameAsc(
            Long footballMatchId,
            Long appTeamId,
            MatchParticipationStatus status
    );

    @Modifying
    @Query(value = """
            DELETE FROM match_participation
            WHERE football_match_id IN (
                SELECT football_match.id
                FROM football_match football_match
                WHERE football_match.league_id = :leagueId
                  AND football_match.id NOT IN (:footballMatchIds)
            )
            """, nativeQuery = true)
    void deleteObsoleteByLeague(
            @Param("leagueId") Long leagueId,
            @Param("footballMatchIds") List<Long> footballMatchIds
    );
}
