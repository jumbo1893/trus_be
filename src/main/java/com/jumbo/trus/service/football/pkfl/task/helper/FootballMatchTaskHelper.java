package com.jumbo.trus.service.football.pkfl.task.helper;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jumbo.trus.dto.football.LeagueDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class FootballMatchTaskHelper {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    private Date date;

    private String homeTeamUri;

    private String awayTeamUri;

    private Integer round;

    private LeagueDTO league;

    private String referee;

    private String stadium;

    private Integer homeGoalNumber;

    private Integer awayGoalNumber;

    private String urlResult;

    private boolean isAlreadyPlayed;

}
