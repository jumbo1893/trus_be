package com.jumbo.trus.repository.participation;

import com.jumbo.trus.entity.participation.MatchParticipationCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface MatchParticipationCommentRepository extends JpaRepository<MatchParticipationCommentEntity, Long> {

    List<MatchParticipationCommentEntity> findAllByParticipationFootballMatchIdAndParticipationAppTeamIdOrderByCreatedAtAsc(
            Long footballMatchId,
            Long appTeamId
    );

    Optional<MatchParticipationCommentEntity> findByIdAndParticipationAppTeamId(Long id, Long appTeamId);

    List<MatchParticipationCommentEntity> findAllByParticipationIdOrderByCreatedAtAsc(Long participationId);

    @Modifying
    @Query("""
            DELETE FROM MatchParticipationCommentEntity comment
            WHERE comment.id IN :commentIds
            """)
    void deleteAllByIds(@Param("commentIds") Collection<Long> commentIds);

    @Modifying
    @Query("""
            DELETE FROM MatchParticipationCommentEntity comment
            WHERE comment.participation.footballMatch.league.id = :leagueId
              AND comment.participation.footballMatch.id NOT IN :footballMatchIds
            """)
    void deleteObsoleteByLeague(
            @Param("leagueId") Long leagueId,
            @Param("footballMatchIds") List<Long> footballMatchIds
    );
}
