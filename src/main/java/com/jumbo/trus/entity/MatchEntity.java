package com.jumbo.trus.entity;

import com.jumbo.trus.entity.achievement.PlayerAchievementEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.football.FootballMatchEntity;
import com.jumbo.trus.entity.footbar.FootbarSessionEntity;
import com.jumbo.trus.entity.pkfl.PkflMatchEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
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

    private Integer homeGoalNumber;

    private Integer awayGoalNumber;

    @OneToMany(
            mappedBy = "match",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Fetch(FetchMode.SUBSELECT)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<MatchWeatherEntity> weatherEntries = new ArrayList<>();

    @Transient
    public MatchWeatherEntity getWeather() {
        return weatherEntries == null || weatherEntries.isEmpty()
                ? null
                : weatherEntries.get(0);
    }

    public void setWeather(MatchWeatherEntity weather) {
        if (weatherEntries == null) {
            weatherEntries = new ArrayList<>();
        }

        weatherEntries.clear();

        if (weather != null) {
            weather.setMatch(this);
            weatherEntries.add(weather);
        }
    }

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

    @OneToMany(mappedBy = "match")
    private List<FootbarSessionEntity> footbarSessions;
}
