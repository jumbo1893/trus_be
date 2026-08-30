package com.jumbo.trus.entity.auth;

import com.jumbo.trus.entity.*;
import com.jumbo.trus.entity.football.TeamEntity;
import com.jumbo.trus.entity.notification.NotificationEntity;
import com.jumbo.trus.entity.participation.MatchParticipationPromptAudience;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

@Entity
@Table(name = "app_team")
@Data
public class AppTeamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "app_team_seq")
    @SequenceGenerator(name = "app_team_seq", sequenceName = "app_team_seq", allocationSize = 1)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private MatchParticipationPromptAudience matchParticipationPromptAudience =
            MatchParticipationPromptAudience.ALL_PAIRED;

    @ManyToOne
    private UserEntity owner;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private TeamEntity team;

    @OneToMany(mappedBy = "appTeam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserTeamRole> teamRoles = new ArrayList<>();

    @OneToMany(mappedBy = "appTeam")
    private List<PlayerEntity> playerList = new ArrayList<>();

    @OneToMany(mappedBy = "appTeam")
    private List<MatchEntity> matchList = new ArrayList<>();

    @OneToMany(mappedBy = "appTeam")
    private List<NotificationEntity> notificationList = new ArrayList<>();

    @OneToMany(mappedBy = "appTeam")
    private List<GoalEntity> goalList = new ArrayList<>();

    @OneToMany(mappedBy = "appTeam")
    private List<ReceivedFineEntity> receivedFineList = new ArrayList<>();

    @OneToMany(mappedBy = "appTeam")
    private List<SeasonEntity> seasonList = new ArrayList<>();

    @OneToMany(mappedBy = "appTeam")
    private List<FineEntity> fineList = new ArrayList<>();

    @OneToMany(mappedBy = "appTeam")
    private List<BeerEntity> beerList = new ArrayList<>();

    @OneToMany(mappedBy = "appTeam")
    private List<DynamicTextEntity> dynamicTextList = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppTeamEntity appTeam = (AppTeamEntity) o;
        return Objects.equals(id, appTeam.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
