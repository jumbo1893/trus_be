package com.jumbo.trus.dto.pkfl;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PkflMatchDTO {

    private long id;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    private Date date;

    private PkflOpponentDTO opponent;

    private int round;

    private String league;

    private PkflStadiumDTO stadium;

    private PkflRefereeDTO referee;

    private PkflSeasonDTO season;

    private Integer trusGoalNumber;

    private Integer opponentGoalNumber;

    private boolean homeMatch;

    private String urlResult;

    private String refereeComment;

    private boolean alreadyPlayed;

    @NotNull
    private List<PkflIndividualStatsDTO> playerList;

    @NotNull
    private List<@Positive Long> matchIdList;

    public PkflMatchDTO(Date date, PkflOpponentDTO opponent, int round, String league, PkflStadiumDTO stadium, PkflRefereeDTO referee, PkflSeasonDTO season, Integer trusGoalNumber, Integer opponentGoalNumber, boolean homeMatch, String urlResult, boolean alreadyPlayed) {
        this.date = date;
        this.opponent = opponent;
        this.round = round;
        this.league = league;
        this.stadium = stadium;
        this.referee = referee;
        this.season = season;
        this.trusGoalNumber = trusGoalNumber;
        this.opponentGoalNumber = opponentGoalNumber;
        this.homeMatch = homeMatch;
        this.urlResult = urlResult;
        this.alreadyPlayed = alreadyPlayed;
        this.matchIdList = new ArrayList<>();
    }

}
