package com.jumbo.trus.repository.participation;

import com.jumbo.trus.entity.participation.MatchParticipationEntity;
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

    @Modifying
    @Query("""
            DELETE FROM MatchParticipationEntity participation
            WHERE participation.footballMatch.league.id = :leagueId
              AND participation.footballMatch.id NOT IN :footballMatchIds
            """)
    void deleteObsoleteByLeague(
            @Param("leagueId") Long leagueId,
            @Param("footballMatchIds") List<Long> footballMatchIds
    );
}
