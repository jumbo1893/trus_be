package com.jumbo.trus.service.fact;

import com.jumbo.trus.dto.beer.response.get.BeerDetailedResponse;
import lombok.Data;

@Data
public class BeerFact {

    final BeerDetailedResponse allBeerDetailedResponseForMatch;
    final BeerDetailedResponse currentSeasonBeerDetailedResponseForMatch;

    final BeerDetailedResponse allBeerDetailedResponseForPlayer;
    final BeerDetailedResponse currentSeasonBeerDetailedResponseForPlayer;

    public BeerFact(BeerDetailedResponse allBeerDetailedResponseForMatch, BeerDetailedResponse currentSeasonBeerDetailedResponseForMatch,
                    BeerDetailedResponse allBeerDetailedResponseForPlayer, BeerDetailedResponse currentSeasonBeerDetailedResponseForPlayer) {
        this.allBeerDetailedResponseForMatch = allBeerDetailedResponseForMatch;
        this.currentSeasonBeerDetailedResponseForMatch = currentSeasonBeerDetailedResponseForMatch;
        this.allBeerDetailedResponseForPlayer = allBeerDetailedResponseForPlayer;
        this.currentSeasonBeerDetailedResponseForPlayer = currentSeasonBeerDetailedResponseForPlayer;
    }


}
