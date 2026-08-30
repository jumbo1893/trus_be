package com.jumbo.trus.entity.participation;

import com.jumbo.trus.entity.PlayerEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "match_participation_comment_reaction",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_match_participation_comment_reaction_player",
                columnNames = {"comment_id", "player_id"}
        )
)
@Getter
@Setter
public class MatchParticipationCommentReactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "match_participation_comment_reaction_seq")
    @SequenceGenerator(
            name = "match_participation_comment_reaction_seq",
            sequenceName = "match_participation_comment_reaction_seq",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private MatchParticipationCommentEntity comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private MatchParticipationCommentReactionType reaction;

    @Column(nullable = false)
    private Instant reactedAt;
}
