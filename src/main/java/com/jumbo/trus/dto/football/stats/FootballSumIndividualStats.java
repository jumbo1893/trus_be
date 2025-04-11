package com.jumbo.trus.dto.football.stats;

import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.dto.football.LeagueDTO;
import com.jumbo.trus.dto.football.TeamDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FootballSumIndividualStats {

    private FootballPlayerDTO player;

    private TeamDTO team;

    private LeagueDTO league;

    private int matches;

    private int goals;

    private int receivedGoals;

    private int ownGoals;

    private int goalkeepingMinutes;

    private int yellowCards;

    private int redCards;

    private int bestPlayers;

    private int hattricks;

    private int cleanSheets;

    private int matchPoints;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FootballSumIndividualStats that = (FootballSumIndividualStats) o;
        return Objects.equals(player.getId(), that.player.getId()) && Objects.equals(team.getId(), that.team.getId()) && Objects.equals(league.getId(), that.league.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(player.getId(), team.getId(), league.getId());
    }
}
