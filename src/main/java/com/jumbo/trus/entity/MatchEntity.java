package com.jumbo.trus.entity;

import com.jumbo.trus.entity.achievement.PlayerAchievementEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.football.FootballMatchEntity;
import com.jumbo.trus.entity.pkfl.PkflMatchEntity;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Entity(name = "match")
@Data
public class MatchEntity {

    @Id
    @GeneratedValue(generator = "match_seq")
    @SequenceGenerator(name = "match_seq", sequenceName = "match_seq", allocationSize = 1)
    private Long id;

    private String name;

    private Date date;

    @ManyToMany
    @JoinTable(name = "matchPlayers",
            joinColumns = @JoinColumn(name = "matchId"),
            inverseJoinColumns = @JoinColumn(name = "playerId"))
    private List<PlayerEntity> playerList;

    @ManyToOne
    private SeasonEntity season;

    private boolean home;

    @OneToMany(mappedBy = "match")
    private List<BeerEntity> beerList;

    @OneToMany(mappedBy = "match")
    private List<ReceivedFineEntity> fineList;

    @OneToMany(mappedBy = "match")
    private List<GoalEntity> goalList;

    @ManyToOne
    private FootballMatchEntity footballMatch;

    @ManyToOne
    private AppTeamEntity appTeam;

    @ManyToOne
    private PkflMatchEntity pkflMatch;

    @OneToMany(mappedBy = "match")
    private List<PlayerAchievementEntity> playerAchievements;
}
