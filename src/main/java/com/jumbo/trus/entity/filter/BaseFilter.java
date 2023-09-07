package com.jumbo.trus.entity.filter;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

public class BaseFilter {

    private Long playerId;

    private Long matchId;

    //defaultní hodnota
    private int limit = 1000;

    public BaseFilter(Long playerId, Long matchId) {
        this.playerId = playerId;
        this.matchId = matchId;
    }

    public BaseFilter(Long matchId) {
        this.matchId = matchId;
    }
}
