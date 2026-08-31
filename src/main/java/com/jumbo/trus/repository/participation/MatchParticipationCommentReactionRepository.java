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
    @Query(value = """
            DELETE FROM match_participation_comment_reaction
            WHERE comment_id IN (
                SELECT comment.id
                FROM match_participation_comment comment
                JOIN match_participation participation
                  ON participation.id = comment.participation_id
                JOIN football_match football_match
                  ON football_match.id = participation.football_match_id
                WHERE football_match.league_id = :leagueId
                  AND football_match.id NOT IN (:footballMatchIds)
            )
            """, nativeQuery = true)
    void deleteObsoleteByLeague(
            @Param("leagueId") Long leagueId,
            @Param("footballMatchIds") List<Long> footballMatchIds
    );
}
