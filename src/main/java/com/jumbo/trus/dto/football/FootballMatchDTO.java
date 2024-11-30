package com.jumbo.trus.dto.football;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jumbo.trus.service.football.pkfl.task.helper.FootballMatchTaskHelper;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class FootballMatchDTO {

    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    private Date date;

    private TeamDTO homeTeam;

    private TeamDTO awayTeam;

    private int round;

    private Long leagueId;

    private String stadium;

    private String referee;

    private Integer homeGoalNumber;

    private Integer awayGoalNumber;

    private String urlResult;

    private String refereeComment;

    private boolean alreadyPlayed;

    @NotNull
    private List<FootballMatchPlayerDTO> playerList;

    @NotNull
    private List<@Positive Long> matchIdList;

    private List<FootballMatchPlayerDTO> homePlayerList;

    private List<FootballMatchPlayerDTO> awayPlayerList;


    public FootballMatchDTO(FootballMatchTaskHelper footballMatchTaskHelper, TeamDTO homeTeam, TeamDTO awayTeam) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.date = footballMatchTaskHelper.getDate();
        this.round = footballMatchTaskHelper.getRound();
        this.leagueId = footballMatchTaskHelper.getLeagueId();
        this.stadium = footballMatchTaskHelper.getStadium();
        this.referee = footballMatchTaskHelper.getReferee();
        this.homeGoalNumber = footballMatchTaskHelper.getHomeGoalNumber();
        this.awayGoalNumber = footballMatchTaskHelper.getAwayGoalNumber();
        this.urlResult = footballMatchTaskHelper.getUrlResult();
        this.alreadyPlayed = footballMatchTaskHelper.isAlreadyPlayed();
        this.playerList = new ArrayList<>();
        this.matchIdList = new ArrayList<>();
        this.homePlayerList = new ArrayList<>();
        this.awayPlayerList = new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FootballMatchDTO that = (FootballMatchDTO) o;
        return round == that.round && areDatesEqual(date, that.date) && Objects.equals(homeTeam.getId(), that.homeTeam.getId()) && Objects.equals(awayTeam.getId(), that.awayTeam.getId()) && Objects.equals(leagueId, that.leagueId) && Objects.equals(stadium, that.stadium) && Objects.equals(referee, that.referee) && Objects.equals(homeGoalNumber, that.homeGoalNumber) && Objects.equals(awayGoalNumber, that.awayGoalNumber) && Objects.equals(urlResult, that.urlResult);
    }

    private boolean areDatesEqual(Date date1, Date date2) {
        if (date1 == null || date2 == null) return date1 == date2;
        return date1.getTime() == date2.getTime();
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, homeTeam, awayTeam, round, leagueId, stadium, referee, homeGoalNumber, awayGoalNumber, urlResult);
    }
}
