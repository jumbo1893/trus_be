package com.jumbo.trus.entity.football;

import jakarta.persistence.*;
import lombok.Data;

@Entity(name = "table_team")
@Data
public class TableTeamEntity {

    @Id
    @GeneratedValue(generator = "table_team_seq")
    @SequenceGenerator(name = "table_team_seq", sequenceName = "table_team_seq", allocationSize = 1)
    private Long id;

    private int rank;

    private int matches;

    private int wins;

    private int draws;

    private int losses;

    private int goalsScored;

    private int goalsReceived;

    private String penalty;

    private int points;

    @ManyToOne
    private TeamEntity team;

    @ManyToOne
    private LeagueEntity league;
}
