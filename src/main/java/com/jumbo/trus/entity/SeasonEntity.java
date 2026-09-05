package com.jumbo.trus.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

import java.util.Date;
import java.util.List;

@Entity(name = "season")
@Table(name = "season", uniqueConstraints = @UniqueConstraint(
        name = "uk_season_team_automatic_key",
        columnNames = {"app_team_id", "automatic_key"}
))
@Data
public class SeasonEntity {

    @Id
    @GeneratedValue(generator="season_seq")
    @SequenceGenerator(name = "season_seq", sequenceName = "season_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;


    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    @Column(nullable = false)
    private Date fromDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    @Column(nullable = false)
    private Date toDate;

    // Committed with the event so restarting does not enqueue the same end date twice.
    @Column(name = "achievement_event_for_end")
    private Date achievementEventForEnd;

    @Column(name = "automatic_key", length = 32)
    private String automaticKey;

    @Column(name = "dates_manually_edited", nullable = false)
    @ColumnDefault("false")
    private boolean datesManuallyEdited;

    @ColumnDefault("true")
    private boolean editable = true;

    @OneToMany(mappedBy = "season")
    private List<MatchEntity> matchList;

    @ManyToOne
    @JoinColumn(name = "app_team_id")
    private AppTeamEntity appTeam;
}
