package com.jumbo.trus.dto.football.stats;

import com.jumbo.trus.dto.football.FootballPlayerDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FootballAllIndividualStats {

    private FootballPlayerDTO player;

    private int matches;

    private int goals;

    private int receivedGoals;

    private int ownGoals;

    private int goalkeepingMinutes;

    private int yellowCards;

    private int redCards;

    private int bestPlayer;

    private int hattrick;

    private int cleanSheet;

    private List<CardComment> yellowCardComments;

    private List<CardComment> redCardComments;

    private int matchPoints;

    public FootballAllIndividualStats(FootballSumIndividualStats footballSumIndividualStats) {
        this.player = footballSumIndividualStats.getPlayer();
        this.matches = footballSumIndividualStats.getMatches();
        this.goals = footballSumIndividualStats.getGoals();
        this.receivedGoals = footballSumIndividualStats.getReceivedGoals();
        this.ownGoals = footballSumIndividualStats.getOwnGoals();
        this.goalkeepingMinutes = footballSumIndividualStats.getGoalkeepingMinutes();
        this.yellowCards = footballSumIndividualStats.getYellowCards();
        this.redCards = footballSumIndividualStats.getRedCards();
        this.bestPlayer = footballSumIndividualStats.getBestPlayers();
        this.hattrick = footballSumIndividualStats.getHattricks();
        this.cleanSheet = footballSumIndividualStats.getCleanSheets();
        this.yellowCardComments = new ArrayList<>();
        this.redCardComments = new ArrayList<>();
        this.matchPoints = footballSumIndividualStats.getMatchPoints();
    }

    public FootballAllIndividualStats(FootballSumIndividualStats footballSumIndividualStats, List<CardComment> yellowCardComments, List<CardComment> redCardComments) {
        this.player = footballSumIndividualStats.getPlayer();
        this.matches = footballSumIndividualStats.getMatches();
        this.goals = footballSumIndividualStats.getGoals();
        this.receivedGoals = footballSumIndividualStats.getReceivedGoals();
        this.ownGoals = footballSumIndividualStats.getOwnGoals();
        this.goalkeepingMinutes = footballSumIndividualStats.getGoalkeepingMinutes();
        this.yellowCards = footballSumIndividualStats.getYellowCards();
        this.redCards = footballSumIndividualStats.getRedCards();
        this.bestPlayer = footballSumIndividualStats.getBestPlayers();
        this.hattrick = footballSumIndividualStats.getHattricks();
        this.cleanSheet = footballSumIndividualStats.getCleanSheets();
        this.yellowCardComments = yellowCardComments;
        this.redCardComments = redCardComments;
        this.matchPoints = footballSumIndividualStats.getMatchPoints();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FootballAllIndividualStats that = (FootballAllIndividualStats) o;

        return Objects.equals(player, that.player);
    }

    @Override
    public int hashCode() {
        return player != null ? player.hashCode() : 0;
    }
}
