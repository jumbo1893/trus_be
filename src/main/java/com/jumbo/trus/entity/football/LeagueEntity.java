package com.jumbo.trus.entity.football;

import com.jumbo.trus.dto.football.Organization;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity(name = "league")
@Data
public class LeagueEntity {

    @Id
    @GeneratedValue(generator = "league_seq")
    @SequenceGenerator(name = "league_seq", sequenceName = "league_seq", allocationSize = 1)
    private Long id;

    private String name;

    private int rank;

    @Enumerated(EnumType.STRING)
    private Organization organization;

    private String organizationUnit;

    private String uri;

    private String year;

    @ManyToMany(mappedBy = "leagueList")
    private List<TeamEntity> teamList;

    @OneToMany(mappedBy = "league")
    private List<TableTeamEntity> tableTeamList;

    private boolean currentLeague;

    @OneToMany(mappedBy = "league")
    private List<FootballMatchEntity> matchList;

}
