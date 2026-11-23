package com.jumbo.trus.entity;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.football.FootballMatchEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "planned_match", uniqueConstraints =
        @UniqueConstraint(name = "uk_planned_match_team_football", columnNames = {"app_team_id", "football_match_id"}))
@Getter
@Setter
public class PlannedMatchEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "planned_match_seq")
    @SequenceGenerator(name = "planned_match_seq", sequenceName = "planned_match_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_team_id", nullable = false)
    private AppTeamEntity appTeam;

    @Column(nullable = false)
    private Date date;

    /** Actual kick-off when the event was started manually; the clock runs from it. */
    private Date kickoffAt;

    private String opponent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PlannedMatchType type;

    private Boolean home;

    @ManyToOne(fetch = FetchType.LAZY)
    private SeasonEntity season;

    @ManyToOne(fetch = FetchType.LAZY)
    private FootballMatchEntity footballMatch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PlannedMatchStatus status = PlannedMatchStatus.SCHEDULED;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", unique = true)
    private MatchEntity match;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean hasPhoto;
}
