package com.jumbo.trus.dto.football.detail;

import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.dto.football.LeagueDTO;
import com.jumbo.trus.dto.football.TeamDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BestScorer {

    private FootballPlayerDTO player;

    private Integer totalGoals;

    private TeamDTO team;

    private LeagueDTO league;
}
