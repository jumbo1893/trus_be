package com.jumbo.trus.service.football.pkfl.task.helper;

import com.jumbo.trus.dto.football.TeamDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerMatchStatsHelper {

    private long id;

    private String playerUri;

    private int goals;

    private int receivedGoals;

    private int ownGoals;

    private int goalkeepingMinutes;

    private int yellowCards;

    private int redCards;

    private boolean bestPlayer;

    private boolean hattrick;

    private boolean cleanSheet;

    private String yellowCardComment;

    private String redCardComment;

    private TeamDTO team;

    public PlayerMatchStatsHelper(String playerUri, int goals, int receivedGoals, int ownGoals, int goalkeepingMinutes, int yellowCards, int redCards, boolean bestPlayer, boolean hattrick, boolean cleanSheet, String yellowCardComment, String redCardComment) {
        this.playerUri = playerUri;
        this.goals = goals;
        this.receivedGoals = receivedGoals;
        this.ownGoals = ownGoals;
        this.goalkeepingMinutes = goalkeepingMinutes;
        this.yellowCards = yellowCards;
        this.redCards = redCards;
        this.bestPlayer = bestPlayer;
        this.hattrick = hattrick;
        this.cleanSheet = cleanSheet;
        this.yellowCardComment = yellowCardComment;
        this.redCardComment = redCardComment;
    }
}
