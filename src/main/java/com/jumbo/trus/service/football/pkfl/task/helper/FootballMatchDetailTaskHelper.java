package com.jumbo.trus.service.football.pkfl.task.helper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class FootballMatchDetailTaskHelper {

    private String refereeComment;

    private List<PlayerMatchStatsHelper> homeTeamPlayers;

    private List<PlayerMatchStatsHelper> awayTeamPlayers;

}
