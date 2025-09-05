package com.jumbo.trus.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jumbo.trus.entity.achievement.PlayerAchievementEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.entity.football.FootballPlayerEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "player")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PlayerEntity {

    @Id
    @GeneratedValue(generator="player_seq")
    @SequenceGenerator(name = "player_seq", sequenceName = "player_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private long id;

    @Column(nullable = false)
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    @Column(nullable = false)
    private Date birthday;

    @Column(nullable = false)
    private boolean fan;

    @Column(nullable = false)
    private boolean active;

    @ManyToMany(mappedBy = "playerList")
    private List<MatchEntity> matchList;

    @OneToMany(mappedBy = "player")
    private List<BeerEntity> beerList;

    @OneToMany(mappedBy = "player")
    private List<ReceivedFineEntity> fineList;

    @OneToMany(mappedBy = "player")
    private List<GoalEntity> goalList;

    @ManyToOne
    private AppTeamEntity appTeam;

    @OneToOne
    @JoinColumn(name = "football_player_id", unique = true)
    private FootballPlayerEntity footballPlayer;

    @OneToMany(mappedBy = "player")
    private List<UserTeamRole> userTeamRoles = new ArrayList<>();


    @OneToMany(mappedBy = "player")
    private List<PlayerAchievementEntity> playerAchievements;
}
