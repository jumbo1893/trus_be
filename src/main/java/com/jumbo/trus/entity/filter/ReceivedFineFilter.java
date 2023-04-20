package com.jumbo.trus.entity.filter;


import lombok.Data;

@Data
public class ReceivedFineFilter {


    private int fineNumber;

    private Long playerId;

    private Long matchId;

    private Long fineId;

    private Long seasonId;

    //defaultní hodnota
    private int limit = 1000;

    public ReceivedFineFilter(Long matchId, Long playerId, Long fineId) {
        this.playerId = playerId;
        this.matchId = matchId;
        this.fineId = fineId;
    }
}
