package com.jumbo.trus.entity.filter;


import com.jumbo.trus.entity.auth.AppTeamEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

public class BaseFilter {

    private Long playerId;

    private Long matchId;

    private AppTeamEntity appTeam;

    //defaultní hodnota
    private int limit = 1000000;

    public BaseFilter(Long playerId, Long matchId) {
        this.playerId = playerId;
        this.matchId = matchId;
    }

    public BaseFilter(Long playerId, Long matchId, AppTeamEntity appTeam) {
        this.playerId = playerId;
        this.matchId = matchId;
        this.appTeam = appTeam;
    }

    public BaseFilter(Long matchId) {
        this.matchId = matchId;
    }
}
