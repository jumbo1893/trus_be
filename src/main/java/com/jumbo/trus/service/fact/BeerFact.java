package com.jumbo.trus.service.fact;

import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedResponse;
import com.jumbo.trus.dto.match.MatchHelper;
import com.jumbo.trus.service.order.OrderBeerByBeerOrLiquorNumber;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.jumbo.trus.config.Config.ALL_SEASON_ID;
import static com.jumbo.trus.config.Config.OTHER_SEASON_ID;

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
