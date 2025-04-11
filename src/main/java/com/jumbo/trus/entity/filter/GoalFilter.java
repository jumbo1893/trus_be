package com.jumbo.trus.entity.filter;


import com.jumbo.trus.entity.auth.AppTeamEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GoalFilter {


    private int goalNumber;

    private int assistNumber;

    private Long playerId;

    private Long matchId;

    private Long seasonId;

    private AppTeamEntity appTeam;

    //defaultní hodnota
    private int limit = 1000;

    public GoalFilter(Long matchId, Long playerId) {
        this.playerId = playerId;
        this.matchId = matchId;
    }
}
