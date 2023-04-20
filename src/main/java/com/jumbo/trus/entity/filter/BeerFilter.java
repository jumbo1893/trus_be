package com.jumbo.trus.entity.filter;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class BeerFilter {


    private int beerNumber;

    private int liquorNumber;

    private Long playerId;

    private Long matchId;

    private Long seasonId;

    //defaultní hodnota
    private int limit = 1000;

    public BeerFilter(Long matchId, Long playerId) {
        this.playerId = playerId;
        this.matchId = matchId;
    }
}
