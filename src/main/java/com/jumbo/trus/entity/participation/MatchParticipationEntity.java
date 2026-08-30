package com.jumbo.trus.entity.participation;

import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.football.FootballMatchEntity;
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
        name = "match_participation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_match_participation_team_match_player",
                columnNames = {"app_team_id", "football_match_id", "player_id"}
        )
)
@Getter
@Setter
public class MatchParticipationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "match_participation_seq")
    @SequenceGenerator(
            name = "match_participation_seq",
            sequenceName = "match_participation_seq",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_team_id", nullable = false)
    private AppTeamEntity appTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "football_match_id", nullable = false)
    private FootballMatchEntity footballMatch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private MatchParticipationStatus status;

    @Column(nullable = false)
    private Instant respondedAt;
}
