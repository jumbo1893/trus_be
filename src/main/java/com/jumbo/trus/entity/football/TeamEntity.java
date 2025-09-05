package com.jumbo.trus.entity.football;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "team")
@Data
public class TeamEntity {

    @Id
    @GeneratedValue(generator = "team_seq")
    @SequenceGenerator(name = "team_seq", sequenceName = "team_seq", allocationSize = 1)
    private Long id;

    private String name;

    @ManyToOne
    private LeagueEntity currentLeague;

    private String uri;

    @ManyToMany
    @JoinTable(name = "leagueTeam",
            joinColumns = @JoinColumn(name = "teamId"),
            inverseJoinColumns = @JoinColumn(name = "leagueId"))
    private List<LeagueEntity> leagueList;

    @ManyToMany
    @JoinTable(name = "teamPlayers",
            joinColumns = @JoinColumn(name = "teamId"),
            inverseJoinColumns = @JoinColumn(name = "footballPlayerId"))
    private List<FootballPlayerEntity> footballPlayerList;

    @OneToMany(mappedBy = "team")
    private List<TableTeamEntity> tableTeamList;

    @OneToMany(mappedBy = "homeTeam")
    private List<FootballMatchEntity> homeMatchList;

    @OneToMany(mappedBy = "awayTeam")
    private List<FootballMatchEntity> awayMatchList;

    @OneToMany(mappedBy = "team")
    private List<FootballMatchPlayerEntity> footballMatchPlayers;

    @OneToMany(mappedBy = "team")
    private List<AppTeamEntity> appTeams;
}

