package com.jumbo.trus.dto.football;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TableTeamDTO {

    private Long id;

    private int rank;

    private int matches;

    private int wins;

    private int draws;

    private int losses;

    private int goalsScored;

    private int goalsReceived;

    private String penalty;

    private int points;

    private Long teamId;

    private String teamName;

    private LeagueDTO league;

    public TableTeamDTO(int rank, int matches, int wins, int draws, int losses, int goalsScored, int goalsReceived, String penalty, int points, LeagueDTO league) {
        this.rank = rank;
        this.matches = matches;
        this.wins = wins;
        this.draws = draws;
        this.losses = losses;
        this.goalsScored = goalsScored;
        this.goalsReceived = goalsReceived;
        this.penalty = penalty;
        this.points = points;
        this.league = league;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableTeamDTO that = (TableTeamDTO) o;
        return rank == that.rank && matches == that.matches && wins == that.wins && draws == that.draws && losses == that.losses && goalsScored == that.goalsScored && goalsReceived == that.goalsReceived && points == that.points && Objects.equals(penalty, that.penalty);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rank, matches, wins, draws, losses, goalsScored, goalsReceived, penalty, points);
    }
}
