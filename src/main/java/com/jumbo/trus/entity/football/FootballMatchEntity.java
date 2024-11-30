package com.jumbo.trus.entity.football;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jumbo.trus.entity.MatchEntity;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Entity(name = "football_match")
@Data
public class FootballMatchEntity {

    @Id
    @GeneratedValue(generator = "football_match_seq")
    @SequenceGenerator(name = "football_match_seq", sequenceName = "football_match_seq", allocationSize = 1)
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    private Date date;

    @ManyToOne
    private TeamEntity homeTeam;

    @ManyToOne
    private TeamEntity awayTeam;

    private Integer round;

    private Long leagueId;

    private String stadium;

    private String referee;

    private Integer homeGoalNumber;

    private Integer awayGoalNumber;

    private String urlResult;

    @Column(columnDefinition = "TEXT")
    private String refereeComment;

    private boolean alreadyPlayed;

    @OneToMany(mappedBy = "footballMatch")
    private List<MatchEntity> matchList;

    @OneToMany(mappedBy = "match")
    private List<FootballMatchPlayerEntity> playerList;

}
