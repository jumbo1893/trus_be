package com.jumbo.trus.repository.participation;

import com.jumbo.trus.entity.participation.MatchParticipationCommentReactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface MatchParticipationCommentReactionRepository extends JpaRepository<MatchParticipationCommentReactionEntity, Long> {

    List<MatchParticipationCommentReactionEntity> findAllByCommentParticipationFootballMatchIdAndCommentParticipationAppTeamId(
            Long footballMatchId,
            Long appTeamId
    );

    Optional<MatchParticipationCommentReactionEntity> findByCommentIdAndPlayerId(Long commentId, Long playerId);

    @Modifying
    @Query("""
            DELETE FROM MatchParticipationCommentReactionEntity reaction
            WHERE reaction.comment.id IN :commentIds
            """)
    void deleteAllByCommentIds(@Param("commentIds") Collection<Long> commentIds);

    @Modifying
    @Query("""
            DELETE FROM MatchParticipationCommentReactionEntity reaction
            WHERE reaction.comment.participation.footballMatch.league.id = :leagueId
              AND reaction.comment.participation.footballMatch.id NOT IN :footballMatchIds
            """)
    void deleteObsoleteByLeague(
            @Param("leagueId") Long leagueId,
            @Param("footballMatchIds") List<Long> footballMatchIds
    );
}
