package com.jumbo.trus.dto.football;

import com.jumbo.trus.service.football.pkfl.task.helper.PlayerMatchStatsHelper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FootballMatchPlayerDTO {

    private long id;

    private FootballPlayerDTO player;

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

    private Long matchId;

    private TeamDTO team;

    public FootballMatchPlayerDTO(FootballPlayerDTO player, int goals, int receivedGoals, int ownGoals, int goalkeepingMinutes, int yellowCards, int redCards, boolean bestPlayer, boolean hattrick, boolean cleanSheet, String yellowCardComment, String redCardComment) {
        this.player = player;
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

    public FootballMatchPlayerDTO(PlayerMatchStatsHelper playerMatchStatsHelper, FootballPlayerDTO footballPlayerDTO, TeamDTO team, Long matchId) {
        this.goals = playerMatchStatsHelper.getGoals();
        this.receivedGoals = playerMatchStatsHelper.getReceivedGoals();
        this.ownGoals = playerMatchStatsHelper.getOwnGoals();
        this.goalkeepingMinutes = playerMatchStatsHelper.getGoalkeepingMinutes();
        this.yellowCards = playerMatchStatsHelper.getYellowCards();
        this.redCards = playerMatchStatsHelper.getRedCards();
        this.bestPlayer = playerMatchStatsHelper.isBestPlayer();
        this.hattrick = playerMatchStatsHelper.isHattrick();
        this.cleanSheet = playerMatchStatsHelper.isCleanSheet();
        this.yellowCardComment = playerMatchStatsHelper.getYellowCardComment();
        this.redCardComment = playerMatchStatsHelper.getRedCardComment();
        this.player = footballPlayerDTO;
        this.team = team;
        this.matchId = matchId;
    }
}
